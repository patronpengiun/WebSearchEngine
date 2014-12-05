package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ContentHandler implements HttpHandler {
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		String requestMethod = exchange.getRequestMethod();
	    if (!requestMethod.equalsIgnoreCase("GET")) { // GET requests only.
	      return;
	    }
	    String[] temp = exchange.getRequestURI().getPath().split("/");
	    String[] name = temp[temp.length-1].split("\\.");
	    String type = name[name.length-1].equals("css") ? "text/css; charset=UTF-8" : "text/html; charset=UTF-8";
	    FileInputStream in = new FileInputStream("content/" + temp[temp.length-1]);
	    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
	    
	    String line = null;
	    
	    StringBuilder builder = new StringBuilder();
	    while ((line = reader.readLine()) != null) {
	    	builder.append(line);
	    	builder.append("\n");
	    }
	    reader.close();
	    
	    String body = builder.toString();
    
	    Headers responseHeaders = exchange.getResponseHeaders();
	    responseHeaders.set("Content-Type", type);
	    exchange.sendResponseHeaders(200, 0); 
	    OutputStream responseBody = exchange.getResponseBody();
	    responseBody.write(body.getBytes("UTF-8"));
	    responseBody.close();
	}
}
