package org.ds.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.ds.logger.DSLogger;

/**
 * @author pjain11, mallapu2
 * Listens to a port and accepts connections
 *
 */
public class StartServer {
	public static void main(String[] args) {
		ServerSocket serverSocket=null;
		Server server = null;
		int id = 0;
		try{
			//Can accept upto 5 connections in parallel
			Executor executor = Executors.newFixedThreadPool(5);
			if(args.length==0){
				serverSocket = new ServerSocket(3456);	
			}
			//for accepting machine id
			else if(args.length==1) {
				id = Integer.parseInt(args[0]);
				serverSocket = new ServerSocket(3456);
			}else{
				serverSocket = new ServerSocket(Integer.parseInt(args[1]));
				id = Integer.parseInt(args[0]);
			}
			DSLogger.log("StartServer","main","Listening to "+serverSocket.getInetAddress()+":"+serverSocket.getLocalPort());
			System.out.println("Server started on port: "+serverSocket.getLocalPort());

			while(true){
				server = new Server(serverSocket.accept(), id);	
				executor.execute(server);
				DSLogger.log("StartServer", "main", "Connection established b/w "+server.getdSocket().getSocket().getLocalAddress()+":"+server.getdSocket().getSocket().getLocalPort()+" and "+server.getdSocket().getSocket().getInetAddress()+":"+server.getdSocket().getSocket().getPort());
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			try {
				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
