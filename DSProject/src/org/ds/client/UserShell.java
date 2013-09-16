package org.ds.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.ds.logger.DSLogger;
import org.ds.networkConf.XmlParseUtility;
import org.ds.socket.DSocket;

/**
 * @author pjain11, mallapu2
 *
 *Client program to take input expression
 *and invoke distributed grep
 *
 */
public class UserShell {

	private static final int PORT_NUMBER = 3456;
    static String key;
	static String value;
	static String pattern;
	static boolean hasKey = false, hasValue = false,generateLog=false;;
	
	/**
	 * @param args
	 * main method to accept command line arguments
	 * 
	 */
	public static void main(String[] args) {
		Options options = new Options();
		options.addOption("k", true, "key");
		options.addOption("v", true, "value");
		options.addOption("h", true, "help");
		options.addOption("l", false,"generate log");    
		options.addOption("help", true, "help");

		CommandLineParser parser = new PosixParser();
		CommandLine cmd = null;
		
		//Parsing command line arguments
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		 
		if (cmd.hasOption("k")) {
			hasKey = true;
			key = cmd.getOptionValue("k");
		}

		if (cmd.hasOption("v")) {
			hasValue = true;
			value = cmd.getOptionValue("v");
		}
		
		if(cmd.hasOption("l")){
			generateLog=true;
		}
		
		//No key or value argument specified 
		//search for pattern in file
		if(!hasKey && !hasValue){
			pattern=args[0];
		}
		
		
		long startTime = System.currentTimeMillis();
		System.out.println("**********EXECUTING DISTRIBUTED GREP**************");
		
		//Invoke Distributed Grep
		
		search();
		long endTime=System.currentTimeMillis();
        System.out.println("**********RESULTS OF DISTRIBUTED GREP WRITTEN TO FILE: "+"DSResults.tmp");		
        
		System.out.println("**********TIME TAKEN FOR DISTRIBUTED GREP IS "+(endTime-startTime)+"**************");
		
		//Logger to log activities in program
		DSLogger.log("UserShell", "main", "TIME TAKEN FOR DISTRIBUTED GREP IS "+(endTime-startTime));
		

	}

	/*
	 * Entry point method to start search across distributed systems
	 * */
	private static void search() {
		DSLogger.log("UserShell", "search", "Entering");
		try {
			//establishing connection to server using socket 
			DSocket server = new DSocket("127.0.0.1", PORT_NUMBER);
			DSLogger.log("UserShell", "search", "Connection established to Server");
			
			//Building up a list of input parameters 
			//to know what appropriate actions to perform 
			//while running grep
			
			List<String> strList=new ArrayList<String>();
            
            strList.add(new String("local~!true"));
            if(hasKey){
            strList.add("key~!"+key);
            }
            
            if(hasValue){
                strList.add("value~!"+value);
            }
            
            if(generateLog){
            	strList.add("generateLog~!true");
            }
            
            if(!hasKey && !hasValue){
            	strList.add("pattern~!"+pattern);
            }
            strList.add("end~!#!"+"end");
            server.writeMultipleLines(strList);			
			
            //File which contains results
            //Writing results for local grep in chunks 512 bytes 
            
            File file = new File("DSResults.tmp");
			FileOutputStream fos = new FileOutputStream(file, false);
			DSLogger.log("UserShell", "search", file.getCanonicalPath());
			byte[] b = new byte[512];
			int len=0;
			while((len=server.getIn().read(b))!=-1){
				 fos.write(b, 0, len);
			}
			DSLogger.log("UserShell", "search", "Merging Log files");
			
			//Getting address of all other servers in network to check their output files
			//If it exists merge the data form temp file to above mentioned results file
			List<String> network = XmlParseUtility.getNetworkServerIPAddrs();
			File tempFile;
			FileInputStream tempFis;
			
			for(int i=0; i< network.size(); i++){
				if((tempFile = new File("DSTempLog"+network.get(i)+".tmp")).exists()){
					tempFis = new FileInputStream(tempFile);
					while((len=tempFis.read(b))!=-1){
						 fos.write(b, 0, len);
					}
					tempFis.close();
					tempFile.delete();
				}
				
			}
			
			DSLogger.log("UserShell", "search", "Exiting");
			fos.close();
          	server.close();
			
		} 
		catch (UnknownHostException e) {
			System.out.println("Cannot find host.");
			DSLogger.log("UserShell", "search", e.getMessage());
		} catch (IOException e) {
			System.out.println("Error establishing connection to host.");			
			DSLogger.log("UserShell", "search", e.getMessage());
		}
		
	}

}