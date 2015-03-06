package N3ReasonerFilesPackage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;

import org.semanticweb.owlapi.model.IRI;

public class EligibleRingParser {

	
	/* search in the file <outputPath + PatientName + "_new.ttl"> for the observation with the factor "eligible_ring"
	 * write the found factor value in the file <outputPath + reasoner_output_eligibleRingSet.txt> */
	public void findEligibleRingValue(String outputPath, String PatientName) throws IOException {
		
		String inputFile = outputPath + PatientName + "_new.ttl";
		String outputFile = outputPath + "reasoner_output_eligibleRingSet.txt";
		
		//creating outputFile
		File output = new File(outputFile);
        output.createNewFile();
		
		//reader to red from a file
		BufferedReader br = new BufferedReader(new FileReader(inputFile));
		
		//writer to write in a file
		PrintWriter writer = new PrintWriter(outputFile, "UTF-8");
		
		//help variables at the start of the loop
		//current line
		String line;
		//is true if we are within the "eligible_ring observation"
		Boolean elRingObs = false;
		
		//going through the patient file line by line 
		while ((line = br.readLine()) != null) {
			if (Pattern.matches("\\s*sp:Property-3Ahas_Heart_Factor hft-version:eligible_ring;", line)) {
				elRingObs = true;
			}
			//parse the obsValue 'ring1, ring2, ring3' from 'sp:Property-3AobsValue "ring1, ring2, ring3" ]'
			if ((elRingObs) && (Pattern.matches("\\s*sp:Property-3AobsValue \\s*.+", line))) {
				
				int startQuoteSign = line.indexOf('"');
				String eligibleRings = line.substring(startQuoteSign+1, line.indexOf('"', startQuoteSign+1));
				
				//parse ring1
				//parse ring2
				//parse ring3
				//from 'ring1, ring2, ring3'

				String ring = "";
				int startIndex = 0;
				int currentCommaIndex = eligibleRings.indexOf(',');
				int lastCommaIndex = eligibleRings.lastIndexOf(',');
				
				// if there are no commas in the file, just print the single ring
				if (currentCommaIndex==-1) {
					writer.println(eligibleRings);
				}
				
				// there are some commas
				else {
					
					//choose the string between the left border (should be the first character of the ring) and the right border (should be a comma)
					ring = eligibleRings.substring(startIndex, currentCommaIndex);  

					//print the found ring to the outputFile
					writer.println(ring);

					// until we have reached the last comma
					while (currentCommaIndex!=lastCommaIndex) {
						
						//iterate left border
						startIndex = currentCommaIndex + 2;
						
						//iterate right border
						currentCommaIndex = eligibleRings.indexOf(',', currentCommaIndex+1);	
						
						//choose the string between the left border (should be the first character of the ring) and the right border (should be a comma)
						ring = eligibleRings.substring(startIndex, currentCommaIndex);  
					
						//print the found ring to the outputFile
						writer.println(ring);
					}
					
					//we have reached the last comma -> the right border is the last character of the string
					ring = eligibleRings.substring(currentCommaIndex + 2, eligibleRings.length());
					writer.println(ring);
				}
				
				// mark "the observation with eligible rings is already processed"
				elRingObs = false;
			}
		}
		writer.close();
		br.close();	 
	}
	
}