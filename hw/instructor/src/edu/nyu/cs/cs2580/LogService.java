package edu.nyu.cs.cs2580;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;

public class LogService {
	private static String logPath = null;
	
	public static void SetLogFilePath(String path) {
		logPath = path;
	}
	
	public static void log(String did, String session, String type, String query) {
		try {
			FileWriter writer = new FileWriter(logPath, true);
		    writer.append(session + "\t" + query + "\t" + did + "\t" + type + "\t" + new Date().toString() + "\n");
		    writer.close();
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		}
	}
	
}
