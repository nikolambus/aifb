package MappingN3toSWRL;
//import Query;
//import QueryExecution;
//import QuerySolution;
//import ResultSet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;







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

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;


@Path("/")
public class Service {

	@Context
	UriInfo uriInfo;
	@Context
	Request request;

	@POST
	@Path("/input")
	@Consumes("application/rdf+xml")
	public void postStuff(String rdf, @Context final HttpServletResponse servletResponse, @Context final HttpServletRequest servletRequest, @Context final ServletContext context) throws IOException, SAXException {
		
		// Our PLAN
		/* String rdf is read (via POST from Postman) and has a rdf/xml structure that should satisfy the pattern in Helper.getSparqlInputPattern()
		a) check if it satisfies the SPARQL input pattern 
		b) if yes, then extract 
			- request
			- rule file
		   from the POST
		c) build from the obtained n3 file the appropriate swrl rule file 
		*/
		
		//Variables where we gonna store our request parameters
		String ruleFile=null;
		String requestURI="";
		
		String sparql_prefixes = ServiceHelper.getSparqlPrefixesAsString();		
		String sparql_input_pattern = ServiceHelper.getSparqlInputPattern();
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
		
		QuerySolution soln = ServiceHelper.evaluationOfSPARQLQueryAgainstModel(querystring, model);
		
		if (soln==null) 
			System.out.println("Input SPARQL pattern has not been satisfied.");			
		else {
			
			//Return the value of the named variable in this binding, casting to a Resource. 
			requestURI = soln.getResource("request").toString();
			ruleFile = soln.getResource("n3ruleFile").toString();	
			
			//check
			//System.out.println("Request URI: " + requestURI);	
			//System.out.println("N3RuleFile: " + ruleFile);		
			
			//-----------------------------------------------------------------------------------------------
			//------------------------------------- handling rule -------------------------------------------
			String ruleName;
			
			/* at first we find the name of our rule. 
			 * Commonly our N3 rule is given as an address. We should parse it.
			 * We do not need the prefix before '/'
			 */
			if (ruleFile.contains("/")) {
				// we do not need the extension within a rule name
				if (ruleFile.contains(".")) 
					ruleName = ruleFile.substring(ruleFile.lastIndexOf("/")+1, ruleFile.lastIndexOf("."));
				else 
					ruleName = ruleFile.substring(ruleFile.lastIndexOf("/")+1, ruleFile.length());
			}
			else {
				// we do not need the extension within a rule name
				if (ruleFile.contains(".")) 
					ruleName = ruleFile.substring(0, ruleFile.indexOf("."));
				else 
					ruleName = ruleFile;
			}

			//-----------------------------------------------------------------------------------------------
			//------------------------------------- preparations -------------------------------------------
			
			// this will be the name of our result RDF/XML file
			String ruleRDF = ruleName + ".owl";
			
			//get the output folder of the Cognitive App via ServletContext method "getRealPath"
			String outputPath = context.getRealPath("/files/output/") + "/" + ruleRDF;
		
			//check
			System.out.println("n3 rule file: " + ruleFile);
			System.out.println("rule name: " + ruleName);
			System.out.println("new swrl rule file: " + outputPath);
			System.out.println("");
		
			//-----------------------------------------------------------------------------------------------
			//------------------------------------- action -------------------------------------------
			
			N3_RDF_Mapper7 mapper = new N3_RDF_Mapper7();
			mapper.action(ruleFile, ruleName, outputPath);
		}	
	}
	
	/*
	@GET
	@Path("/descriptionTTL")
	@Produces("application/rdf+turtle")
	public String getDescription(@Context final HttpServletResponse servletResponse, @Context final HttpServletRequest servletRequest, @Context final ServletContext context) throws Exception {

		//get the descriptions path via ServletContext method "getRealPath" (see explanation how does it work at the end)
		String descriptionsPath = context.getRealPath("/files/descriptions/") + "/";

		//choose the "LF_turtle.ttl" file from the folder with descriptions and output it as response to @GET @Path "/descriptionTTL"
		ServiceHelper.printRDFDescriptionFromFile(descriptionsPath + "LF_turtle.ttl", servletResponse, context, "application/rdf+turtle");
		return "";
	}
	*/
	
	@GET
	@Path("/descriptionHTML")
	@Produces(MediaType.TEXT_HTML)
	public String getHTMLDescription() {
		return "<html> " + "<title>" + "HTML Description Head" + "</title>"
	        + "<body><h1>" + "HTML Description Body" + "</body></h1>" + "</html> ";
	}
}
