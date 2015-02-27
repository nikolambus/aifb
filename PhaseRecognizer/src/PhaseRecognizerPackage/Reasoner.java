package PhaseRecognizerPackage;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.SWRLRule;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.util.InferredOntologyGenerator;
import org.semanticweb.owlapi.util.OWLEntityRemover;

import com.clarkparsia.pellet.owlapiv3.PelletReasoner;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

public class Reasoner {

    // Use here the same IRI as your SMW or Surgipedia
  	public IRI basicIRI = IRI.create("http://surgipedia.sfb125.de/wiki/Special:URIResolver/");	
    // Use special IRI for individuals
   	public IRI indIRI = IRI.create("http://surgipedia.sfb125.de/wiki/Special:URIResolver/Individual#");	
   	
  	public OWLOntologyManager m1 = OWLManager.createOWLOntologyManager();
  	public OWLOntologyManager m2 = OWLManager.createOWLOntologyManager();
  	public OWLOntologyManager m3 = OWLManager.createOWLOntologyManager();

	public OWLObjectProperty previousPhase;
	public OWLNamedIndividual detected = null;
  	
    public Boolean action(String ruleFile, String contextFile, String contextURI, String opType, String outputPath) throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
    	
    	/*load at first the rule and the context files as 2 ontologies. Note: they have the same basic IRI. 
    	It isn't allowed to load 2 different ontologies with the same IRI into the same manager. So we need different manager:
    	m1 - for ruleOnto, m2 - for contextOnto and m3 - for the new mergedOnto*/    
    	OWLOntology ruleOnto = m1.loadOntology(IRI.create(ruleFile));
    	OWLOntology contextOnto = m2.loadOntology(IRI.create(contextFile));
    	OWLOntology mergedOnt = m3.createOntology(basicIRI);
    	
    	/* Copy all logical axioms (discard annotation properties) from the rule ontology and all axioms from the context ontology into the new one */
		m3.addAxioms(mergedOnt, ruleOnto.getLogicalAxioms());
		m3.addAxioms(mergedOnt, contextOnto.getAxioms());
		
		// Now we would like to set OP type specific PreviousPhase properties and create appropriate individuals
		// Therefore we create an instance of a special class which is responsible for it
		OPTypeAboxesCreator creator = new OPTypeAboxesCreator();
		
		if (opType.equalsIgnoreCase("PancreasResection")) {
			//create classes, individuals and previousPhase-relationships for phases 
			Set<OWLAxiom> phases = creator.setPancreasIndividualsAndPreviousPhaseProperties();
		    m3.addAxioms(mergedOnt, phases);
		}

		//CCE = Cholecystectomy
		if (opType.equalsIgnoreCase("CCE")) {
		    Set<OWLAxiom> phases = creator.setCCEIndividualsAndPreviousPhaseProperties();
		    m3.addAxioms(mergedOnt, phases);
		}
		
		//AE = Adrenoectomy
		if (opType.equalsIgnoreCase("AE")) {
			Set<OWLAxiom> phases = creator.setAEIndividualsAndPreviousPhaseProperties();
		    m3.addAxioms(mergedOnt, phases);
		}
		
		//initialize the reasoner with help of the Pellet-package
		PelletReasoner reasoner = PelletReasonerFactory.getInstance().createReasoner(mergedOnt);		
	
		//Now it should perform reasoning
		reasoner.getKB().realize();
			
		//Now let us fill the results of the reasoning in our ontology variable
		InferredOntologyGenerator generator = new InferredOntologyGenerator(reasoner);
		generator.fillOntology(m3, mergedOnt);
		
		// remove rules if any
		removeRules(mergedOnt);
        
    	//Now we just finding out which individual was asserted to the DetectedPhase-class 
	    
        //1.step: we need one reasoner more - an OWLreasoner
        OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
	    OWLReasoner owlreasoner = reasonerFactory.createReasoner(mergedOnt);
	    
	    //2.step: defining the class DetectedPhase
	    OWLDataFactory factory = m3.getOWLDataFactory();
	    OWLClass detectedPhase = factory.getOWLClass(IRI.create(basicIRI + "DetectedPhase"));
        
	    //3.step: get the instances of the class DetectedPhase
	    NodeSet<OWLNamedIndividual> individualsNodeSet = owlreasoner.getInstances(detectedPhase, true);
        Set<OWLNamedIndividual> detectedIndis = individualsNodeSet.getFlattened();
        
        //If there are some
        if (!detectedIndis.isEmpty()) {
        	//Get the first instance from the class currentPhase with help of "Iterator" and "next" command
            Iterator<OWLNamedIndividual> i = detectedIndis.iterator();
            detected = i.next();
            System.out.println ("Detected phase is: \n" + detected);
           
            /* Following 2 lines allow to add an annotation property to a RDF node without creation of named individual
    	  	 * Use it for specifying the relationship between PhaseContext-URI and DetectedPhase
    	  	 */
    		OWLAnnotationProperty BelongsToPhase = factory.getOWLAnnotationProperty(IRI.create(basicIRI + "Property-3ABelongsToPhase"));
    	    m3.addAxiom(mergedOnt, factory.getOWLAnnotationAssertionAxiom(BelongsToPhase, IRI.create(contextURI), detected.getIRI()));	
            
            //Now save ontology in a new file 
            File output = new File(outputPath);
            output.createNewFile();
            m3.saveOntology(mergedOnt, IRI.create(output.toURI()));
            return true;
        }
        else {
        	//System.out.println("No Phase could be detected.");
        	return false;
        }
    } 
    
    //check do we have rules. If yes, remove them. The result is a patient file.
    public void removeRules (OWLOntology o) {
    	Set<SWRLRule> rulesSet = o.getAxioms(AxiomType.SWRL_RULE);
    	if (!rulesSet.isEmpty()) {
    		for (SWRLRule rule : rulesSet) {
				m3.removeAxiom(o, rule);
    		}
    	}
    }
    
    /* Optionally we can also define a method which would check how many instances the class DetectedPhase conclude. 
     * If more than one (should be two), remove the more old one. It would adopt the same principle as in the 
     * SWRLReasoner/Reasoner.java/saveCurrentPhase and SWRLReasoner/Reasoner.java/removeOldPhase 
     * 
     * But actually there is no need in this mechanism because each new context is sent via new POST command and there is a new story
     * */
}
