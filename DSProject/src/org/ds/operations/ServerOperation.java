/**
 * 
 */
package org.ds.operations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.ds.logger.DSLogger;

/**
 * @author pjain11, mallapu2
 * 
 * Parses the input parameters
 * and runs the grep in different
 * configuration as per the parameters
 * 
 */
public class ServerOperation {

	String FILENAME;
	private int machineId;
	private boolean headerPrinted = false;

	public ServerOperation(String fILENAME, int machineId) {
		super();
		FILENAME = fILENAME;
		this.machineId = machineId;
	}

	private Socket clientSocket;
    
	//Run Grep based on the specified pattern
	public void runGrep(String pattern) throws IOException {
		DSLogger.log("ServerOperation", "runGrep", "Entering...");
		DSLogger.log("ServerOperation", "runGrep", "Running grep for pattern "
				+ pattern);
		ProcessBuilder pb = new ProcessBuilder("grep", pattern, FILENAME);
		Process proc = null;
		try {
			proc = pb.start();
		} catch (IOException e) {
			DSLogger.log("ServerOperation", "runGrep", e.getMessage());
			e.printStackTrace();
		}
		
		if(!FILENAME.equals("DSJUNITLog.tmp") && !headerPrinted){
			byte[] tempB = ("\n********** Results for machine "+machineId+" **********\n").getBytes();
			clientSocket.getOutputStream().write(tempB, 0, tempB.length);
			headerPrinted = true;
		}
		InputStream is = proc.getInputStream();
		
		byte b[] = new byte[512];

		int len = 0;
		while ((len = is.read(b)) != -1) {
			clientSocket.getOutputStream().write(b, 0, len);
		}
		try {
			proc.waitFor();
		} catch (InterruptedException e) {
			DSLogger.log("ServerOperation", "runGrep", e.getMessage());
			e.printStackTrace();
		}
		proc.destroy();
		DSLogger.log("ServerOperation", "runGrep", "Exiting..");

	}

	public void processInputSearchParams(Map<String, String> inputStrMap,
			Socket socket) throws IOException {

		this.clientSocket = socket;
		DSLogger.log("ServerOperation", "processInputParams", "Entering");
		for (String key : inputStrMap.keySet()) {
			DSLogger.log("ServerOperation", "processInputParams",
					"Key-Value Pair:" + key + ":" + inputStrMap.get(key));
		}
		boolean hasKey = inputStrMap.containsKey("key");
		boolean hasValue = inputStrMap.containsKey("value");
		String cmd = null;
		//if both key and value are specified, get the matching keys and matching values and grep for the combination.
		if (hasKey && hasValue) {
			String keyPattern = inputStrMap.get("key");
			String valuePattern = inputStrMap.get("value");
			DSLogger.log("ServerOperation", "processInputParams",
					"Input Param for key:" + keyPattern + " and value:"
							+ valuePattern);
			cmd="cut -d ':' -f 1 "+FILENAME+" | grep "+keyPattern+" | sort -u"; 
			Set<String> matchingKeys=processPartialCmd(cmd);
			cmd="cut -d ':' -f 2- "+FILENAME+" | grep "+valuePattern+" | sort -u";
			Set<String> matchingValues=processPartialCmd(cmd);
			for(String key:matchingKeys){
				for(String value:matchingValues){
					cmd = "^" + key+ ":"+value;
					runGrep(cmd);
				}
			}
			
		} else if (hasKey) {       //if only key is specified, get the matching keys based on the key expression, and then grep on the matching keys
			String keyPattern = inputStrMap.get("key");

			DSLogger.log("ServerOperation", "processInputParams",
					"Input Param for key:" + keyPattern);
			
			cmd="cut -d ':' -f 1 "+FILENAME+" | grep "+keyPattern+" | sort -u"; // cut the log file based on delimiter and pick the first field(key)
			Set<String> matchingKeys=processPartialCmd(cmd);
			for(String key:matchingKeys){
			cmd = "^" + key+ ":.*";
			runGrep(cmd);
			}
			
		} else if (hasValue) {    //if only value is specified, get the matching keys based on the key expression, and then grep on the matching keys
			String valuePattern = inputStrMap.get("value");			
			DSLogger.log("ServerOperation", "processInputParams",
					"Input Param for value:" + valuePattern);
			cmd="cut -d ':' -f 2- "+FILENAME+" | grep "+valuePattern+" | sort -u"; // cut the log file based on delimiter and pick the second-last field(value)
			Set<String> matchingValues=processPartialCmd(cmd);
			for(String value:matchingValues){
			cmd = ".*:"+value;
			runGrep(cmd);
			}			
		} else if (!hasKey && !hasValue) { // if neither key nor value is specified, run the grep using the pattern.
			String pattern = inputStrMap.get("pattern");
			DSLogger.log("ServerOperation", "processInputParams",
					"Input Param for pattern:" + pattern);
			runGrep(pattern);
		}

		DSLogger.log("ServerOperation", "processInputParams", "Exiting");

	}
	
	    //Return a set of complete keys based on the partial key match
		public Set<String> processPartialCmd(String cmd,String... value) throws IOException{
	        DSLogger.log("ServerOperation", "processPartialCmd", "Entering");
	        DSLogger.log("ServerOperation", "processPartialCmd", "Partial Command: "+cmd);
			ProcessBuilder pb = new ProcessBuilder("/bin/sh","-c",cmd);
			Process proc =null;
			try {
				proc = pb.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			InputStream is = proc.getInputStream();
			BufferedReader bReader=new BufferedReader(new InputStreamReader(is));
			Set<String> matchingStrList=new HashSet<String>();
			String matchStr=null;
			while((matchStr=bReader.readLine())!=null){
				matchingStrList.add(matchStr);
			}
			try {
				proc.waitFor();
			} catch (InterruptedException e) {
				DSLogger.log("ServerOperation", "runGrep", e.getMessage());
				e.printStackTrace();
			}
			DSLogger.log("ServerOperation", "processPartialCmd", "Exiting");
			return matchingStrList;
		}
}
