package cwm.reasoning;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import com.google.common.io.CharStreams;

public class ReasonGivenRule {
	
	// Method for CDL-Execution
	@SuppressWarnings("deprecation")
	public static void RunCommandLineTool(String patient, String rule, String new_patient) throws IOException {

		String cmdCommandLine = ReasonGivenRule.getProperties("cmd");
		//System.out.println (cmdCommandLine);
				
		/*String command1 = cmdCommandLine + " cd KITPARASHA && mkdir KITOTSTOI";
		System.out.println(command1);*/
		
		//to change the folder where rule execution should take place + execute the rule 
		String command1 = cmdCommandLine + " cd D:\\DiplArbeit\\OurWork\\Eclipse_workSPACE2\\LFRule && cwm.py " + patient + " --think=" + rule + " > " + new_patient;
		System.out.println(command1);

		//to execute the rule as default in the folder where Eclipse is installed
		String command2 = cmdCommandLine + " cwm.py " + patient + " --think=" + rule + " > " + new_patient;
		System.out.println(command2);
		
		// Initiate Runtime
		Runtime runtime = Runtime.getRuntime();
		Process process1 = runtime.exec(command1);
		Process process2 = runtime.exec(command2);
	}	
	
	public static String getProperties(String key) throws IOException {
		Reader reader = null;
		String propertyFilePath = null;
			
		propertyFilePath = "C:/Users/Administrator/Desktop/ansi.properties";
		reader = new FileReader(propertyFilePath);
		Properties prop = new Properties();
		
		prop.load(reader);
		
		return prop.getProperty(key);
		//reader.close();	
	}
	
}
