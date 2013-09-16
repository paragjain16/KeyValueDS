package org.ds.operations;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ds.logger.DSLogger;
import org.ds.socket.DSocket;

/**
 * @author pjain11, mallapu2
 * 
 * Class to act as proxy for remote servers
 * local server creates as many instances of this class 
 * as number of entries in network_configuration.xml
 * all instances runs in parallel and contact the remote servers 
 * to pass parameters collected from User
 * and invokes grep cmd on remote servers and also collects back the result
 */
public class ContactServer implements Runnable {
	private String address;
	private int port;
	private String pattern;
	private String output;
	private DSocket socket;
	private File file;
	Map<String, String> inputStrMap; 
	

	public ContactServer(String address, int port, Map<String, String> inputStrMap, DSocket socket){
		this.address = address;
		this.port = port;
		this.inputStrMap = inputStrMap;
		this.socket = socket;
		DSLogger.log("ContactServer", "ContactServer", "Received Parameters in Constructor : "+address+port);
	}
	
	public void run(){
		try {
			 DSLogger.log("ContactServer", "run", "Entering");
		     List<String> strList=new ArrayList<String>();
	         for(String key:inputStrMap.keySet()){
	        	 strList.add(key+"~!"+inputStrMap.get(key));
	         }
	         strList.add("end~!#!"+"end");
	         //sending input parameters to remote server
	         
	         socket.writeMultipleLines(strList);
	         
	         //Creating file to write the grep results
			 file = new File("DSTempLog"+this.getAddress()+":"+this.getPort()+".tmp");
			 FileOutputStream fos = new FileOutputStream(file, false);
			 DSLogger.log("ContactServer", "run", file.getCanonicalPath());
			 byte[] b = new byte[512];
			 int len=0;
			 while(true){
				 len = getSocket().getIn().read(b);
				 if(len==-1){
					 break;
				 }
				 fos.write(b, 0, len);
			 }
			 DSLogger.log("ContactServer", "run", "Exiting");
			 DSLogger.log("ContactServer", "run", "Closing Socket "+socket.getSocket().getLocalAddress()+":"+socket.getSocket().getPort());

		} catch (UnknownHostException e) {
			//e.printStackTrace();
			DSLogger.log("ContactServer", "run", e.getMessage());
		} catch (IOException e) {
			//e.printStackTrace();
			DSLogger.log("ContactServer", "run", e.getMessage());
		}
		
	}
	/*
	 * Setter and getters method
	 * */
	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}
	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public String getOutput() {
		return output;
	}

	public void setOutput(String output) {
		this.output = output;
	}

	public DSocket getSocket() {
		return socket;
	}

	public void setSocket(DSocket socket) {
		this.socket = socket;
	}

}
