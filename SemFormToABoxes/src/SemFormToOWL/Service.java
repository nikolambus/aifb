package SemFormToOWL;
//import Query;
//import QueryExecution;
//import QuerySolution;
//import ResultSet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;




import java.net.URL;
import java.util.Scanner;

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

import org.semanticweb.owlapi.model.OWLOntologyCreationException;
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
		  - ABoxes (patient/context) URI
		  from the POST
		  c) get the SMW page according to the URI 
		  d) interpret the RDF export of the SMW page and create the appropriate OWL file with ABoxes and some annotations
		*/
		
		//Variables where we gonna store our request parameters
		String requestURI="";
		
		//This variable will host the URI for ABoxes (axioms referring to the OWL individuals) that can represent patient data, context data, etc.  
		String ABoxesURI = null;
		
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
			ABoxesURI = soln.getResource("ABoxesURI").toString();
			
			//check
			//System.out.println("Request URI: " + requestURI);	
			System.out.println("ABoxes URI: " + ABoxesURI);

			//parse "Patient1" from "http://surgipedia.sfb125.de/wiki/Special:URIResolver/Patient1"
			String ABoxesName = ABoxesURI.substring(ABoxesURI.lastIndexOf("/")+1, ABoxesURI.length());
			
			//get RDF Export of "http://surgipedia.sfb125.de/wiki/Special:URIResolver/Patient1"
			String rdfExport = "http://surgipedia.sfb125.de/wiki/Special:ExportRDF/" + ABoxesName;
			
			//getting access to the text of the RDF export from the rule's page in SMW
			Scanner scanner = new Scanner(new URL(rdfExport).openStream(), "UTF-8").useDelimiter("\\A");
			String rdfExportText = scanner.next();
				
			//get the output folder of the Cognitive App via ServletContext method "getRealPath"
			String outputPath = context.getRealPath("/files/output/") + "/" + ABoxesName + ".owl";
		
			//parse the rdfExport text and save the result OWL patient file to the outputPath
			RDFExportParser parser = new RDFExportParser();
			parser.buildABoxes(rdfExportText, ABoxesURI, outputPath);
			scanner.close();
		}	
	}
	
	/*
	@GET
	@Path("/descriptionRDF")
	@Produces("application/rdf+xml")
	public String getDescription(@Context final HttpServletResponse servletResponse, @Context final HttpServletRequest servletRequest, @Context final ServletContext context) throws Exception {

		//get the descriptions path via ServletContext method "getRealPath" (see explanation how does it work at the end)
		String descriptionsPath = context.getRealPath("/files/descriptions/") + "/";

		//choose the "LF_turtle.ttl" file from the folder with descriptions and output it as response to @GET @Path "/descriptionTTL"
		ServiceHelper.printRDFDescriptionFromFile(descriptionsPath + "SF_description.xml", servletResponse, context, "application/rdf+xml");
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