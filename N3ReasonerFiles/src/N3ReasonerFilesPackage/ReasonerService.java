package N3ReasonerFilesPackage;
//import Query;
//import QueryExecution;
//import QuerySolution;
//import ResultSet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;




import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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

import org.mindswap.pellet.jena.PelletReasonerFactory;

@Path("/")
public class ReasonerService {

	@Context
	UriInfo uriInfo;
	@Context
	Request request;

	@POST
	@Path("/input")
	@Consumes("application/rdf+xml")
	public void postStuff(String rdf, @Context final HttpServletResponse servletResponse, @Context final HttpServletRequest servletRequest, @Context final ServletContext context) throws IOException, SAXException, InterruptedException {
		
		// our PLAN
		/* String rdf is read (via POST from Postman) and has a rdf/xml structure that should satisfy the pattern in Helper.getSparqlInputPattern()
		  a) check if it satisfies the SPARQL input pattern 
		  b) if yes, then extract 
		  - request
		  - rule file
		  - patient file
		  from the POST
		  c) execute the rule file on the patient file 

		*/
		
		//Variables where we gonna store our request parameters
		String patientFile=null;
		String ruleFile=null;
		String requestURI="";
		
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
			ruleFile = soln.getResource("ruleFile").toString();
			patientFile = soln.getResource("patientFile").toString(); 
		
			//check
			System.out.println("Patient File: " + patientFile);
			System.out.println("Rule File: " + ruleFile);
			

			//--------------------------------------------------------------------------------------------------
			//------------------------------------- handling patient -------------------------------------------
			
			// finding the name of patient file 
			String patientName;
			if (patientFile.contains("/")) {
				// we do not need the extension within a patient name
				if (patientFile.contains(".")) 
					patientName = patientFile.substring(patientFile.lastIndexOf("/")+1, patientFile.lastIndexOf("."));
				else 
					patientName = patientFile.substring(patientFile.lastIndexOf("/")+1, patientFile.length());
			}
			else {
				// we do not need the extension within a patient name
				if (patientFile.contains(".")) 
					patientName = patientFile.substring(0, patientFile.indexOf("."));
				else 
					patientName = patientFile;
			}
			
			/* Attention!! 
			 * 
			 * the command below will only create a result file in the Cognitive App output folder:  
			 * http://aifb-ls3-vm2.aifb.kit.edu:8080/N3ReasonerFiles/files/output/
			 * 
			 * If you would like to save your new patient data file to another location (e.g. XNAT) just assign another path to the variable outputPath    
			 * */
			//get the output folder of the Cognitive App via ServletContext method "getRealPath"
			String outputPath = context.getRealPath("/files/output/") + "/";
			
				//--------------------------------------------------------------------------------------
				// this part describes the immediate reasoning for Windows  
				//--------------------------------------------------------------------------------------

				/*
			
				// we'll need the runtime environment to execute cwm commands
				Runtime rt = Runtime.getRuntime();
				
				// forming the appropriate cwm reasoning command for Windows Command line interpreter (cmd)
				 String cmd = "cmd /C cwm.py " + patientFile + " --think=" + ruleFile + " --n3=qd/ --purge > " + outputPath + patientName + "_new.ttl";
				 System.out.println("reasoning command: " + cmd); 
				
				//execute it
				Process proc = rt.exec(cmd);		
				
				*/
				
				//--------------------------------------------------------------------------------------
				// this part describes the immediate reasoning for Linux 
				//--------------------------------------------------------------------------------------
			
				//!!!
				//specify here the path to the cwm reasoner
				String cwmPath = "/home/niko/Schreibtisch/cwm-1.2.1/cwm";

				//Build command 
		        List<String> commands = new ArrayList<String>();
		        //Add arguments
		        commands.add("/bin/sh");
		        commands.add("-c");
		        commands.add("python " + cwmPath + " " + patientFile + " --think=" + ruleFile + " --n3=qd/ --purge > " + outputPath + patientName + "_new.ttl");
		        System.out.println(commands);

		        //Run command
		        ProcessBuilder pb = new ProcessBuilder(commands);
		        pb.redirectErrorStream(true);
		        Process process = pb.start();

		        //Check result
		        if (process.waitFor() == 0) {
		            System.out.println("Success!");
		            System.exit(0);
		        }

		        //Abnormal termination: Log command parameters and output and throw ExecutionException
		        System.err.println(commands);
		        System.exit(1);
				
				//--------------------------------------------------------------------------------------
				
				/* only for the project B01
				 * wait till the result file is created
				 * then write the found eligible in a extra file */		
	        	Thread.sleep(7500);
				EligibleRingParser parser = new EligibleRingParser();
				parser.findEligibleRingValue(outputPath, patientName);			
		}
	}
	
	@GET
	@Path("/description")
	@Produces("application/rdf+xml")
	public String getServiceDescriptionRDFXML(@Context final HttpServletResponse response, @Context final ServletContext context) throws Exception {
	
		//get the descriptions path via ServletContext method "getRealPath"
		String descriptionsPath = context.getRealPath("/files/descriptions/") + "/";

		//choose the "N3ReasonerFiles_description.xml" file from the folder with descriptions and output it as response to @GET @Path "/description"
		Helper.printRDFDescriptionFromFile(descriptionsPath + "N3ReasonerFiles_description.xml", response, context, "application/rdf+xml");
			
		return "";
	}
	
	@GET
	@Path("/descriptionHTML")
	@Produces(MediaType.TEXT_HTML)
	public String getHTMLDescription() {
		return "<html> " + "<title>" + "HTML Description Head" + "</title>"
	        + "<body><h1>" + "HTML Description Body" + "</body></h1>" + "</html> ";
	}
}
