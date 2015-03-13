package N3ReasonerPackage;
//import Query;
//import QueryExecution;
//import QuerySolution;
//import ResultSet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;




import java.net.URL;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;

import org.xml.sax.SAXException;

import com.hp.hpl.jena.graph.impl.TripleStore;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.reasoner.Reasoner;


@Path("/")
public class ReasonerService {

	@Context
	UriInfo uriInfo;
	@Context
	Request request;

	@POST
	@Path("/input")
	@Consumes("application/rdf+xml")
	public void postStuff(String rdf, @Context final HttpServletResponse servletResponse, @Context final HttpServletRequest servletRequest, @Context final ServletContext context) throws IOException, SAXException {
		
		// Our PLAN:
		/* String rdf is read (via POST from Postman) and has a rdf/xml structure that should satisfy the pattern in Helper.getSparqlInputPattern()
		  a) check if it satisfies the SPARQL input pattern 
		  b) if yes, then extract 
		  - request
		  - rule URI
		  - patient file
		  from the POST
		  c) from the rule URI obtain the appropriate rule file on the server via Helper.getSparqlPatternForN3RuleFile 
		  d) execute the obtained rule file on the patient file 

		*/
		
		//Variables where we gonna store our request parameters
		String patient=null;
		String rule=null;
		String n3RuleFile = null;
		String requestURI="";
		
		String base = "http://surgipedia.sfb125.de/wiki/Special:URIResolver/";
		
		String sparql_prefixes = Helper.getSparqlPrefixesAsString();		
		String sparql_input_pattern = Helper.getSparqlInputPattern();
		String querystring = sparql_prefixes + sparql_input_pattern;
		
		//check
		//System.out.println(querystring);
		
		//Encodes this String into a sequence of bytes
		InputStream in = new ByteArrayInputStream(rdf.getBytes());
		
		//Answer a fresh rdf Model with the default specification.
		Model model = ModelFactory.createDefaultModel();
			
	    //Add statements from a document. This method assumes the concrete syntax is RDF/XML
		model.read(in, "");
		
		//Close the Model and free up resources held.
		in.close();
	
		QuerySolution soln = Helper.evaluationOfSPARQLQueryAgainstModel(querystring, model);
	
		if (soln==null) 
			System.out.println("Input SPARQL pattern has not been satisfied.");
		else {

			//Return the value of the named variable in this binding, casting to a Resource. 
			requestURI = soln.getResource("request").toString();
			rule = soln.getResource("ruleURI").toString();
			patient = soln.getResource("patientFile").toString(); 
		
			//check
			System.out.println("Rule: " + rule);
			System.out.println("Patient: " + patient);

			
			//--------------------------------------------------------------------------------------------------
			//------------------------------------- handling patient -------------------------------------------
			// finding the name of patient file 
			String patientName;
			if (patient.contains("/")) {
				// we do not need the extension within a patient name
				if (patient.contains(".")) 
					patientName = patient.substring(patient.lastIndexOf("/")+1, patient.lastIndexOf("."));
				else 
					patientName = patient.substring(patient.lastIndexOf("/")+1, patient.length());
			}
			else {
				// we do not need the extension within a patient name
				if (patient.contains(".")) 
					patientName = patient.substring(0, patient.indexOf("."));
				else 
					patientName = patient;
			}

			//check
			System.out.println("Patient name: " + patientName);

			
			//-----------------------------------------------------------------------------------------------
			//------------------------------------- handling rule ------------------------------------------
			String ruleName;
			String prefix;

			/* finding out the the prefix and the immediate rule name without a prefix. 
			 * E.g.: http://surgipedia.sfb125.de/wiki/Special:URIResolver/test2 -> 
			 * prefix = http://surgipedia.sfb125.de/wiki/Special:URIResolver/
			 * ruleName  = test2
			 */
			if (rule.contains("/")) {
				prefix = rule.substring(0, rule.lastIndexOf("/")+1);
				ruleName = rule.substring(rule.lastIndexOf("/")+1, rule.length());
			}
			else { 
				// if our rule does not have any '/', use base SMW prefix
				prefix = base;
				ruleName = rule;
			}				
			
			//check
			//System.out.println("Prefix: " + prefix);
			//System.out.println("Rule name: " + ruleName);
						
			//-------------------------------------------------------------------------------------			
			/* Now we should find the rule file which corresponds to the obtained rule URI. 
			 *  Therefore we evaluate another query over the global triple store.
			 *  1. In case of localhost we just add some RDF/XML files to the model. 
			 *  2. In case of server we should add the RDF export of the SFB Triple Store. 
			 */
		
			Model model2 = ModelFactory.createDefaultModel();

			//1. loading local files and triples form them  (using them as a local triple store)
			
	       	/*
		    model2.read("http://localhost:8080/CognitiveApp6/files/output/rule_CHILDPUGH.owl", "RDF/XML");
	        model2.read("http://localhost:8080/CognitiveApp6/files/output/rule_SF.owl", "RDF/XML");
	        model2.read("http://localhost:8080/CognitiveApp6/files/output/rule_FONG.owl", "RDF/XML");
	        model2.read("http://localhost:8080/CognitiveApp6/files/output/rule_RINGSELECTION.owl", "RDF/XML");
	        model2.read("http://localhost:8080/CognitiveApp2/files/output/RulePancreas1.owl", "RDF/XML");
	        model2.read("http://localhost:8080/CognitiveApp2/files/output/RulePancreas2.owl", "RDF/XML");
	        model2.read("http://localhost:8080/CognitiveApp2/files/output/RulePancreas3.owl", "RDF/XML");
	        model2.read("http://localhost:8080/CognitiveApp2/files/output/RulePancreas4.owl", "RDF/XML");
	        model2.read("http://localhost:8080/CognitiveApp2/files/output/RulePancreas5.owl", "RDF/XML");
	        model2.read("http://localhost:8080/CognitiveApp2/files/output/RuleCCE1.owl", "RDF/XML");
	        model2.read("http://localhost:8080/CognitiveApp2/files/output/RuleCCE2.owl", "RDF/XML");
	        model2.read("http://localhost:8080/CognitiveApp2/files/output/RuleCCE3.owl", "RDF/XML");
	        model2.read("http://localhost:8080/CognitiveApp2/files/output/TestRule0.owl", "RDF/XML");
	        model2.read("http://localhost:8080/CognitiveApp2/files/output/TestRule1.owl", "RDF/XML");
	        model2.read("http://localhost:8080/CognitiveApp2/files/output/TestRule2.owl", "RDF/XML");
	        model2.read("http://localhost:8080/CognitiveApp2/files/output/TestRule3.owl", "RDF/XML");
	        model2.read("http://localhost:8080/CognitiveApp2/files/output/TestRule4.owl", "RDF/XML");
	        model2.read("http://localhost:8080/CognitiveApp4b/files/output/ContextPancreas1.owl", "RDF/XML");
	        model2.read("http://localhost:8080/CognitiveApp4b/files/output/ContextCCE1.owl", "RDF/XML");
	        */
			
			//2. loading triples from SFB Triple Store 
			model2.read("http://aifb-ls3-vm2.aifb.kit.edu/rdfData/surgipediaExport.owl", "RDF/XML");
			//model2.read("http://aifb-ls3-vm2.aifb.kit.edu/rdfData/rdfData.txt", "N-TRIPLE");
					
	        // construct a query which would select the n3 rule file given a ruleURI
	        String querystring2 = Helper.getSparqlPatternForN3RuleFile(prefix, ruleName);
			QuerySolution soln2 = Helper.evaluationOfSPARQLQueryAgainstModel(querystring2, model2);

			if (soln2 == null) {
				System.out.println("Could not find the n3 rule file from rule URI \"" + rule + "\"");
			}
			else {
				n3RuleFile = soln2.getResource("n3RuleFile").toString();
				System.out.println("n3RuleFile: " + n3RuleFile);

				//--------------------------------------------------------------------------------------
				// this part describes the immediate reasoning  
				
				// we'll need the runtime environment to execute cwm commands
				Runtime rt = Runtime.getRuntime();
			
				//get the output folder of the Cognitive App via ServletContext method "getRealPath"
				String outputPath = context.getRealPath("/files/output/") + "/";
				
				// forming the appropriate cwm reasoning command for Windows Command line interpreter (cmd)
				String cmd = "cmd /C cwm.py " + patient + " --think=" + n3RuleFile + " --n3=qd/ --purge > " + outputPath + patientName + "_new.ttl";
				System.out.println("reasoning command: " + cmd);
				
				//execute it
				Process proc = rt.exec(cmd);		
			}
		}
	}

	
	@GET
	@Path("/descriptionHTML")
	@Produces(MediaType.TEXT_HTML)
	public String getHTMLDescription() {
		return "<html> " + "<title>" + "HTML Description Head" + "</title>"
	        + "<body><h1>" + "HTML Description Body" + "</body></h1>" + "</html> ";
	}

	@GET
	@Path("/description")
	@Produces("application/rdf+xml")
	public String getServiceDescriptionRDFXML(@Context final HttpServletResponse response, @Context final ServletContext context) throws Exception {
	
		//get the descriptions path via ServletContext method "getRealPath"
		String descriptionsPath = context.getRealPath("/files/descriptions/") + "/";

		//choose the "N3Reasoner_description.xml" file from the folder with descriptions and output it as response to @GET @Path "/description"
		Helper.printRDFDescriptionFromFile(descriptionsPath + "N3Reasoner_description.xml", response, context, "application/rdf+xml");
			
		return "";
	}

}
