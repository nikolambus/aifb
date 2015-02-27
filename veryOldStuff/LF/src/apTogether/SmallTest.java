package apTogether;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class SmallTest {

	public static void main(String[] args) throws IOException {
		
		//* File creation *// 
		//File file1 = new File("COSTA"); 
		//file1.createNewFile();
		
		//* File copy *//
        Path pathSource = Paths.get("universal_patient_diff_dates.ttl");
        Path pathTarget = Paths.get("0.all");
	    Files.copy(pathSource, pathTarget, StandardCopyOption.REPLACE_EXISTING);

		//* Run 2 commands subsequently *//
		//Runtime.getRuntime().exec("cmd /C cwm.py universal_patient_diff_dates.ttl --think=Rules_all/rule_SF_all.n3 > BIBA.ttl && cmd /C cwm.py BIBA.ttl --think=rule_childpugh_all.n3 > BIBA2.ttl");
	
	}

}
