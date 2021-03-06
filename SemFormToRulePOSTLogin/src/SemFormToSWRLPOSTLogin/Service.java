package SemFormToSWRLPOSTLogin;

//import Query;
//import QueryExecution;
//import QuerySolution;
//import ResultSet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
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
	public void postStuff(String rdf, @Context final HttpServletResponse servletResponse, @Context final HttpServletRequest servletRequest, @Context final ServletContext context) throws IOException, SAXException, OWLOntologyCreationException, OWLOntologyStorageException {
		
		// Our PLAN
		/* String rdf is read (via POST from Postman) and has a rdf/xml structure that should satisfy the pattern in Helper.getSparqlInputPattern()
		  a) check if it satisfies the SPARQL input pattern 
		  b) if yes, then extract 
		  - request
		  - rule URI
		  from the POST
		  c) get the SMW page according to the rule URI 
		  d) interpret the RDF export of the SMW page and create the appropriate OWL file with SWRL rule and some annotations
		*/
		
		//Variables where we gonna store our request parameters
		String ruleURI=null;
		String requestURI="";
		
		String sparql_prefixes = ServiceHelper.getSparqlPrefixesAsString();		
		String sparql_input_pattern = ServiceHelper.getSparqlInputPattern();
		String querystring = sparql_prefixes + sparql_input_pattern;
		
		//check
		//System.out.println("Query:" + querystring);
		
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
			ruleURI = soln.getResource("ruleURI").toString();
			
			//check
			System.out.println("Rule URI: " + ruleURI);
			System.out.println("Request URI: " + requestURI);	
			
			//-----------------------------------------------------------------------------------------------
			//------------------------------------- handling rule -------------------------------------------
			String ruleName;
			
			//parse "TestRule2" from "http://surgipedia.sfb125.de/wiki/Special:URIResolver/TestRule2"
			if (ruleURI.contains("/")) {
				ruleName = ruleURI.substring(ruleURI.lastIndexOf("/")+1, ruleURI.length());
			}
			else { 
				ruleName = ruleURI;
			}				
				
			//-----------------------------------------------------------------------------------------------
			//------------------------------------- algo ----------------------------------------------------

			//get the output folder of the Cognitive App via ServletContext method "getRealPath"
			String outputPath = context.getRealPath("/files/output/") + "\\" + ruleName + "_without_annotations.owl";
			
			//-----------------------------------------------------------------------------------------------
			//                               TRYING TO LOG INTO THE SURGIPEDIA
			 /*
			 * 
			 * Therefore perform 3 steps
			 * 1) POST login=Nikolaus, password=klopman 
			 * 2) Get the response, convert it to String and parse from it the token which has been assigned after the step 1  
			 * 3) POST login=Nikolaus, password=klopman, token=blablabla
			 * 
			 * (this method is described by http://www.mediawiki.org/wiki/API:Login )
			 * */ 
			//-----------------------------------------------------------------------------------------------
			
			// ----------------------------------------- STEP 1 ---------------------------------------------
			
			HttpClient httpclient = HttpClients.createDefault();
			HttpPost httppost = new HttpPost("http://surgipedia.sfb125.de/api.php");

			// Request parameters and other properties.
			List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>(7);
			params.add(new BasicNameValuePair("action", "login"));
			params.add(new BasicNameValuePair("lgname", "Nikolaus"));
			params.add(new BasicNameValuePair("lgpassword", "klopman"));
			
			//we don't know the token yet but reserve a variable for it
			String token = null;
			String cookieprefix = null;
			String session = null;

			httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
			
			// execute
			HttpResponse response = httpclient.execute(httppost);
			
			// ------------------------------------------ STEP 2 -------------------------------------------- 
			
			// get the response
			HttpEntity entity = response.getEntity();			

			if (entity != null) {
			    InputStream instream = entity.getContent();
			    try {
			    	
			    	//converting the InputStream to String
			    	StringWriter writer = new StringWriter();
			    	IOUtils.copy(instream, writer, "UTF-8");
			    	String theString = writer.toString();
			    	
			    	//check
					System.out.println("");
			        System.out.println("The response of the 1st login POST request:");
			        System.out.println (theString);
			        
			        //going through the obtained response line by line 
				    BufferedReader buff = new BufferedReader(new StringReader(theString));
				    String line;
				    while ((line = buff.readLine()) != null) {
				    	if (Pattern.matches(".*token=&quot;[A-Za-z0-9]+&quot;.*", line)) {
				    		
				    		//parse the token value from the appropriate line
				    		Pattern patternTab = Pattern.compile(".*token=&quot;([A-Za-z0-9]+)&quot;.*");
							Matcher mtchTab = patternTab.matcher(line);
							while (mtchTab.find()) {
								//store it in the variable token
								token = mtchTab.group(1);
							}
							 
							//check
							System.out.println("");
							System.out.println("Token: " + token);
				   
							/* 
							 * parse further cookies set after the fist POST (imho they are not important for the success of the STEP 1)
							 * 
							
				    		//parse the cookieprefix value from the appropriate line
				    		Pattern patternTabCookie = Pattern.compile(".*cookieprefix=&quot;([a-zA-Z_0-9:,?\"./]+)&quot;.*"); 
				    		                                            
							Matcher mtchTabCookie = patternTabCookie.matcher(line);
							while (mtchTabCookie.find()) {
								//store it in the variable token
								cookieprefix = mtchTabCookie.group(1);
							}
							
							//check
							System.out.println("");
							System.out.println("cookieprefix: " + cookieprefix);
				    	
							//parse the sessionid value from the appropriate line
				    		Pattern patternTabSession = Pattern.compile(".*sessionid=&quot;([a-zA-Z_0-9:,?\"./]+)&quot;.*"); 
				    		                                            
							Matcher mtchTabSession = patternTabSession.matcher(line);
							while (mtchTabSession.find()) {
								
								session = mtchTabSession.group(1);
							}
							
							//check
							System.out.println("");
							System.out.println("sessionid: " + session);
				    	
				    		*/
				    	}
				    }			        
			    } finally {
			        instream.close();
			    }
			}
			
			// ------------------------------------------------ STEP 3 -------------------------------------------
			
			//further POST parameters which are possible but imho not really necessary
			//params.add(new BasicNameValuePair("lgcookieprefix", cookieprefix));
			//params.add(new BasicNameValuePair("lgsessionid", session));
			//params.add(new BasicNameValuePair("lgdomain", ".surgipedia.sfb125.de"));
			
			params.add(new BasicNameValuePair("lgtoken", token));		
			httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
			
			// execute
			HttpResponse response2 = httpclient.execute(httppost);
			
			// get the response
			HttpEntity entity2 = response2.getEntity();			

			
			if (entity2 != null) {
				InputStream instream2 = entity2.getContent();
			    try {
			    	
			    	//converting the InputStream to String
					StringWriter writer2 = new StringWriter();
					IOUtils.copy(instream2, writer2, "UTF-8");
					String theString2 = writer2.toString();
					
					//check
					System.out.println("");
			        System.out.println("The response of the 2nd login POST request:");
					System.out.println(theString2);
			    
			    } finally {
			        instream2.close();
			    }
			}	
			
			//now we should build a bridge between "http://surgipedia.sfb125.de/wiki/Special:URIResolver/TestRule2" and http://surgipedia.sfb125.de/wiki/Special:RDFExport/TestRule2
			String rdfExport =  "http://surgipedia.sfb125.de/wiki/Special:ExportRDF/" + ruleName;
			
			//check
			System.out.println("");
			System.out.println("RDF Export page");
			System.out.println(rdfExport);
			
			//getting the RDF export of the rule's page in SMW
			Scanner scanner = new Scanner(new URL(rdfExport).openStream(), "UTF-8").useDelimiter("\\A");
			String out = scanner.next();
		
			//check
			System.out.println("");
			System.out.println("RDF Export of the rule page:");
			System.out.println(out);
			
			//Wiki rule --> SWRL rule string
			RDFExportParser parser = new RDFExportParser();
			String rule = parser.CreateRule(out);
	    
			scanner.close();

			//check
			System.out.println("Rule: " + rule);
			System.out.println("Classes bank: " + parser.classesBank);
			System.out.println("Vars bank: " + parser.varsBank);

			//SWRL rule string --> SWRL rule file
			SWRLRuleFromStringCreator2 creator = new SWRLRuleFromStringCreator2();
			creator.fromStringToOWLRDFNotation(rule, outputPath, parser.helpIndividualsList);	
			
			//the output of creator is the input for the next step
			String inputPath = outputPath;

			/* Attention!! 
			 * 
			 * the command below will only create a result file in the Cognitive App output folder:  
			 * http://aifb-ls3-vm2.aifb.kit.edu:8080/SemFormToRule/files/output/
			 * 
			 * It isn't available for TripleStore Crawler there. 
			 * So I see following 3 different ways how to get the triples inside of this file into the TripleStore:
			 * 
			 * 1) To change the output path to the folder, which will be crawled by TripleStore Crawler (Surgipedia, XNAT)
			 * 2) To change the settings of the TripleStore Crawler so that it can crawl files from the folders located on the AIFB server (in this particular case: http://aifb-ls3-vm2.aifb.kit.edu:8080/SemFormToRule/files/output/)
			 * 3) To add the result file into the TripleStore on some other way 
			 * 
			 * */
			//get the output folder of the Cognitive App via ServletContext method "getRealPath"
			outputPath = context.getRealPath("/files/output/") + "\\" + ruleName + ".owl";
			
			//check
			System.out.println("");	
			System.out.println("OutputPath: " + outputPath);	
			
			//SWRL rule file --> SWRL rule file with annotations
			AnnotationPropertiesImplantator2 implantator = new AnnotationPropertiesImplantator2();
			implantator.action(inputPath, ruleName, parser.OPType, outputPath);
			
			//cleaning up
			//removing the rule file without annotations
			java.nio.file.Path p1 = Paths.get(inputPath);
			try {
			    Files.delete(p1);
			} catch (NoSuchFileException x) {
			    System.err.format("%s: no such" + " file or directory%n", p1);
			} catch (DirectoryNotEmptyException x) {
			    System.err.format("%s not empty%n", p1);
			} catch (IOException x) {
			    // File permission problems are caught here.
			    System.err.println(x);
			}
		}
	}
	
	@GET
	@Path("/description")
	@Produces("application/rdf+xml")
	public String getServiceDescriptionRDFXML(@Context final HttpServletResponse response, @Context final ServletContext context) throws Exception {
	
		//get the descriptions path via ServletContext method "getRealPath"
		String descriptionsPath = context.getRealPath("/files/descriptions/") + "/";

		//choose the appropriate file from the folder with descriptions and output it as response to @GET @Path "/description"
		ServiceHelper.printRDFDescriptionFromFile(descriptionsPath + "SemFormToRule_description.xml", response, context, "application/rdf+xml");
			
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
