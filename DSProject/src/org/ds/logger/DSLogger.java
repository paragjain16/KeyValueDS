package org.ds.logger;

import org.apache.log4j.Logger;

/**
 * @author pjain11, mallapu2
 *
 * Logger class to log activities of program for debugging purposes
 * Each server logs its activities in a local log file
 */
public class DSLogger {
		 static Logger log = Logger.getLogger(DSLogger.class.getName());
		 
		 public static void log(String className, String methodName, String msg){
			 log.debug(className+": "+methodName+"- "+msg);
		 }

}
