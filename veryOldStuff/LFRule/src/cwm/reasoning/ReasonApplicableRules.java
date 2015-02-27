package cwm.reasoning;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javax.naming.spi.DirStateFactory.Result;

public class ReasonApplicableRules {

	public static void main(String[] args) throws IOException, InterruptedException {
		
		// define the patient file (input from user)
		String patient = "universal_patient_diff_dates.ttl";
		runIt(patient);
	}
	
	public static void runIt(String patient) throws IOException, InterruptedException {
		if (patient == null)
			System.out.println("no patient!");
		else {
		/*find applicable rules for a certain patient 
		cwm.py command line command creates a helpfile that will be processed below
		*/
		String commandShowRules = "cwm.py " + patient + " --filter=show_applicable_rules.n3 --n3=q > helpfile";
		Runtime runtime = Runtime.getRuntime();
		Process process = runtime.exec("cmd /C" + commandShowRules);
		
		//Here we gonna store the obtained applicable rules
	    List<String> applicableRules = new ArrayList<String>();

	    //waiting till the file "helpfile" via cmd and cwm.py is created
	    Thread.sleep(1000);
	    
	    //reading a file line by line till the end of the file and picking out the rule names 
	    BufferedReader br = new BufferedReader(new FileReader("helpfile"));
	    String line;
	    String rule = null;
	    while ((line = br.readLine()) != null) {
	    	if (line.contains(":rule_")) {
	    		int ulineindex = line.lastIndexOf(':');
	    		rule = line.substring(ulineindex + 1, line.length()-1);
	    		if (rule.charAt(rule.length()-1) == ' ') 
	    			rule = rule.substring(0, rule.length()-1);
		    	
	    		//add the obtained rule name to the list of applicable rules
	    		applicableRules.add(rule);
	    	}
		}
	    br.close();
	    
	    //here our command will land
	    String commandExecuteRules = "";
	    
	    //define the name of the result file (input from user)
	    String result = "resultPatient.ttl";
	    
	    //firstly determine the number of applicable rules
	    int rulesNum = applicableRules.size();

	    //copy patient data to the 0.all in order to get into the loop
        Path pathSource = Paths.get(patient);
        Path pathTarget = Paths.get("0.all");
	    Files.copy(pathSource, pathTarget, StandardCopyOption.REPLACE_EXISTING);
	    
	    //now we start by the index 0 and create a chained command to execute all the rules subsequently  
	    //i - source patient index
	    int i=0;
	    while (i < rulesNum) {
	    	
	    	//j - result patient index
	    	int j=i+1;
	    	commandExecuteRules = commandExecuteRules + "cwm.py " + i + ".all" + " --think=Rules_all/" + applicableRules.get(i) + "_all.n3" + " --n3=q > " + j + ".all" + " && cmd /C ";
			i++;
	    }

    	System.out.println(commandExecuteRules);
	    //this command goes through the list of the found rules and execute for each of them its ALL version on the given patient  
		runtime.exec("cmd /C" + commandExecuteRules);

		//waiting till cmd and cwm are ready
		Thread.sleep(5000);
		
		//copy the last created file (result) to the specified result file
        Path pathResultDigit = Paths.get(i + ".all");
        Path pathResultName = Paths.get(result);
	    Files.copy(pathResultDigit, pathResultName, StandardCopyOption.REPLACE_EXISTING);
	    
	    //cleaning proposal (Not ready yet)
	    /*
	    int k=0;
	    
	    while (k < rulesNum) {
	    	Path helpPath = Paths.get(k + ".all");
	    	Files.delete(helpPath);
	    	k++;
	    }
	    */
		}
	}
	
	public static void runIt(File file) throws IOException, InterruptedException {
	
		if (file == null)
			System.out.println("no patient!");
		else {
		
		//file --> string
		String patient = file.toString();
	
		/*find applicable rules for a certain patient 
		cwm.py command line command creates a helpfile that will be processed below
		*/
		String commandShowRules = "cwm.py " + patient + " --filter=show_applicable_rules.n3 --n3=q > helpfile";
		Runtime runtime = Runtime.getRuntime();
		Process process = runtime.exec("cmd /C" + commandShowRules);
		
		//Here we gonna store the obtained applicable rules
	    List<String> applicableRules = new ArrayList<String>();

	    //waiting till the file "helpfile" via cmd and cwm.py is created
	    Thread.sleep(1000);
	    
	    //reading a file line by line till the end of the file and picking out the rule names 
	    BufferedReader br = new BufferedReader(new FileReader("helpfile"));
	    String line;
	    String rule = null;
	    while ((line = br.readLine()) != null) {
	    	if (line.contains(":rule_")) {
	    		int ulineindex = line.lastIndexOf(':');
	    		rule = line.substring(ulineindex + 1, line.length()-1);
	    		if (rule.charAt(rule.length()-1) == ' ') 
	    			rule = rule.substring(0, rule.length()-1);
		    	
	    		//add the obtained rule name to the list of applicable rules
	    		applicableRules.add(rule);
	    	}
		}
	    br.close();
	    
	    //here our command will land
	    String commandExecuteRules = "";
	    
	    //define the name of the result file (input from user)
	    String result = "resultPatient.ttl";
	    
	    //firstly determine the number of applicable rules
	    int rulesNum = applicableRules.size();

	    //copy patient data to the 0.all in order to get into the loop
        Path pathSource = Paths.get(patient);
        Path pathTarget = Paths.get("0.all");
	    Files.copy(pathSource, pathTarget, StandardCopyOption.REPLACE_EXISTING);
	    
	    //now we start by the index 0 and create a chained command to execute all the rules subsequently  
	    //i - source patient index
	    int i=0;
	    while (i < rulesNum) {
	    	
	    	//j - result patient index
	    	int j=i+1;
	    	commandExecuteRules = commandExecuteRules + "cwm.py " + i + ".all" + " --think=Rules_all/" + applicableRules.get(i) + "_all.n3" + " --n3=q > " + j + ".all" + " && cmd /C ";
			i++;
	    }

    	System.out.println(commandExecuteRules);
	    //this command goes through the list of the found rules and execute for each of them its ALL version on the given patient  
		runtime.exec("cmd /C" + commandExecuteRules);

		//waiting till cmd and cwm are ready
		Thread.sleep(3000);
		
		//copy the last created file (result) to the specified result file
        Path pathResultDigit = Paths.get(i + ".all");
        Path pathResultName = Paths.get(result);
	    Files.copy(pathResultDigit, pathResultName, StandardCopyOption.REPLACE_EXISTING);
	    
	    //cleaning proposal (Not ready yet)
	    /*
	    int k=0;
	    
	    while (k < rulesNum) {
	    	Path helpPath = Paths.get(k + ".all");
	    	Files.delete(helpPath);
	    	k++;
	    }
	    */
		}
	}
}
