/**
 * 
 */
package org.ds.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ds.logger.DSLogger;
import org.ds.networkConf.XmlParseUtility;
import org.ds.operations.ContactServer;
import org.ds.operations.ServerOperation;
import org.ds.socket.DSocket;


/**
 * @author pjain11, mallapu2
 * Server class handles local grep invocation 
 * and also contacts remote servers if it receives request from 
 * local shell for grep
 *
 */
public class Server implements Runnable{
	
	private String action;
	private DSocket dSocket;
	private Socket tempSocket;
	private String hostName;
	private boolean peerMsg = false;
	private List<String> inputStrList;
	private Map<String,String> inputStrMap; 
	private List<Integer> activeServers ;
	private int machineId;
	
	public Server(Socket socket, int machineId) throws UnknownHostException, IOException{
		this.dSocket = new DSocket(socket);
		this.machineId = machineId;
		activeServers = new ArrayList<Integer>();
		inputStrMap = new HashMap<String,String>();
	}
	public void run(){
		try{
			 
			 DSLogger.log("Server", "run()", "Entered");
			 inputStrList=dSocket.readMultipleLines();
			 

			 for(String inputStr:inputStrList){
				 inputStrMap.put(inputStr.split("~!")[0],inputStr.split("~!")[1]);
				 DSLogger.log("Server", "run()", "Read from Socket: "+inputStr);
			 }
			 
			 String jUnitLogFile="DSJUNITLog.tmp";
			 String normalLogFile="machine."+machineId+".log";
			 //used for junit test file generation
			 if(inputStrMap.containsKey("generateLog")){
				 //generate a temp log file
		            File file = new File(jUnitLogFile);
					FileOutputStream fos = new FileOutputStream(file, false);
					PrintStream out=new PrintStream(fos);
					out.print("logQuery:log from machine "+machineId+"\n");
					if(out!=null){
						out.close();
					}
			 }
			 //check for request - is it from local shell or remote server
			 if(inputStrMap.get("local").equals("true")){ // i.e if local:true exists
				 peerMsg = true;
			 }
			 //class to invoke grep command in a process thread
			 ServerOperation so;
			 if(inputStrMap.containsKey("generateLog")){
			   so = new ServerOperation(jUnitLogFile, machineId);
			 }
			 else{
			  so = new ServerOperation(normalLogFile, machineId);
			 }
			 //pass parameters
			 so.processInputSearchParams(inputStrMap, dSocket.getSocket());
			 

			 //if the request is from local shell
			 //try contacting remote servers in parallel threads
			 if(peerMsg){
				 List<String> network = XmlParseUtility.getNetworkServerIPAddrs();
				 Thread[] clients = new Thread[network.size()];
				 ContactServer[] contactServers = new ContactServer[network.size()];
				 inputStrMap.put("local", "false");
				 //read network xml to get addresses of remote machines
				 //the servers that are up put them in list of active servers
				 for(int i=0; i<network.size(); i++){
					 DSLogger.log("Server", "run()", "Server at "+dSocket.getSocket().getLocalAddress()+":"+dSocket.getSocket().getLocalPort()+" Contacting "+network.get(i));
					 try{
						 tempSocket = new Socket(network.get(i).split(":")[0], Integer.parseInt(network.get(i).split(":")[1]));
						 activeServers.add(i);
						 contactServers[i] = new ContactServer(network.get(i).split(":")[0], Integer.parseInt(network.get(i).split(":")[1]), inputStrMap, new DSocket(tempSocket));
					     DSLogger.log("Server", "run()", "Contact success, Starting thread "+i);
						 clients[i] = new Thread(contactServers[i]);
						 clients[i].start();
						 DSLogger.log("Server", "run", "Connection established b/w "+tempSocket);
					 }catch(IOException e){
						 DSLogger.log("Server", "run", "Server "+network.get(i)+" is down");
						 
					 }
				     
				 }

				 //wait for active servers to finish operation
				 for(int i=0; i<activeServers.size(); i++){
					 clients[activeServers.get(i)].join();
					 contactServers[activeServers.get(i)].getSocket().close();
				 }
			 
			 }
			 DSLogger.log("Server","run","Exiting");
			 
		}catch(Exception e){
			//e.printStackTrace();
			DSLogger.log("Server","run", e.toString());
		}finally{
			 try {
				dSocket.close();
				if(inputStrMap.containsKey("generateLog")){
					File file = new File("DSJUNITLog.tmp");
					if(file.exists()){
						file.delete();
					}
				}
			} catch (IOException e) {
				DSLogger.log("Server", "run", e.getMessage());
				//e.printStackTrace();
			}
		}
	}
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
	public DSocket getdSocket() {
		return dSocket;
	}
	public void setdSocket(DSocket dSocket) {
		this.dSocket = dSocket;
	}
	public String getHostName() {
		return hostName;
	}
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

}
