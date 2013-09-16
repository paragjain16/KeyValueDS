package org.ds.junit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import junit.framework.Assert;

import org.ds.client.UserShell;
import org.ds.logger.DSLogger;
import org.junit.Test;
/**
 * @author pjain11, mallapu2
 * 
 * JUnit class to test distributed grep when a known server is down
 * On invocation every server will generate sample log file
 * and then a distributed grep will run on these files whose data is known
 * a file containing expected results is created locally 
 * and the results of grep are matched with the file
 * if they are same then test passes otherwise fails 
 */
public class GrepTestMachineDown {
	private int machines = 3;
	@Test
	public final void testMainServerDown() {
		File expectedFile=writeExpectedFileServerDown();
		String pattern="logQuery";
		String[] args={"-k"+pattern,"-l"};
		UserShell.main(args);
		File resultsfile=new File("DSResults.tmp");
		boolean diffExists=runDiff(resultsfile,expectedFile);
		Assert.assertFalse(diffExists);
	}

	private File writeExpectedFileServerDown() {
		File file=new File("DSExpectedResult.tmp");
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file, false);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		PrintStream out=new PrintStream(fos);
		for(int i=1; i<=machines; i++){
			//out.print("\n********** Results for machine "+i+" **********\n");
			out.print("logQuery:log from machine "+i+"\n");
		}
		/*out.print("logQuery:log from "+"/127.0.0.1:3456\n");
		 List<String> networkAddrList = XmlParseUtility.getNetworkServerIPAddrs();
         for(String networkAddr:networkAddrList){
        	 out.print("logQuery:log from "+"/"+networkAddr+"\n");
         }*/
         if(out!=null){
        	 out.close();
         }
         return file;
		
	}
	private boolean runDiff(File resultsFile, File expectedFile) {
		DSLogger.log("GrepTest", "runDiff","Entering");
		String cmd = "sort "+resultsFile.getName()+" > sortedResuts.tmp";
		ProcessBuilder pb = new ProcessBuilder("/bin/sh","-c",cmd);
		Process proc =null;
		
		try {
			proc = pb.start();
		} catch (IOException e) {
			DSLogger.log("GrepTest", "runDiff", e.getMessage());
			e.printStackTrace();
		} 
		DSLogger.log("GrepTest", "runDiff", "before");
		try {
			proc.waitFor();
			DSLogger.log("GrepTest", "runDiff", "wait");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		pb = new ProcessBuilder("diff","sortedResuts.tmp",expectedFile.getName());
		try {
			proc = pb.start();
		} catch (IOException e) {
			DSLogger.log("GrepTest", "runDiff", e.getMessage());
			e.printStackTrace();
		} 
		DSLogger.log("GrepTest", "runDiff", "before");
		try {
			proc.waitFor();
			File file = new File("sortedResuts.tmp");
			if(file.exists()){
				file.delete();
			}
			DSLogger.log("GrepTest", "runDiff", "wait");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		DSLogger.log("GrepTest", "runDiff", "after wait for");
		int status = proc.exitValue();
		DSLogger.log("GrepTest", "runDiff", "Exit status:"+status);
		//proc.destroy();
		if(status==0){
			return false;
		}else {
			return true;
		}
	}
}
