package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class SearchHandler implements HttpHandler{

	private static String html = "content/search.html";
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		String requestMethod = exchange.getRequestMethod();
	    if (!requestMethod.equalsIgnoreCase("POST")) { // POST requests only.
	      return;
	    }
	    
//	    BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
	    FileInputStream in = new FileInputStream(html);
	    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
	    
	    String line = null;
	    
	    StringBuilder builder = new StringBuilder();
	    while ((line = reader.readLine()) != null) {
	    	builder.append(line);
	    }
	    reader.close();
	    
	    String body = builder.toString();
    
	    Headers responseHeaders = exchange.getResponseHeaders();
	    responseHeaders.set("Content-Type", "text/html; charset=UTF-8");
	    exchange.sendResponseHeaders(200, 0); 
	    OutputStream responseBody = exchange.getResponseBody();
	    responseBody.write(body.getBytes("UTF-8"));
	    responseBody.close();
	}

}
