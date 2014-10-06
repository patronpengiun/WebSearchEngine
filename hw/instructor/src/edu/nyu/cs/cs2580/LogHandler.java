package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class LogHandler implements HttpHandler {
	public void handle(HttpExchange exchange) throws IOException {
		String requestMethod = exchange.getRequestMethod();
	    if (!requestMethod.equalsIgnoreCase("POST")){  // POST requests only.
	      return;
	    }
	    
	    InputStreamReader input = new InputStreamReader(exchange.getRequestBody());
	    BufferedReader reader = new BufferedReader(input);
	    String line = null;
	    StringBuilder builder = new StringBuilder();
	    while ((line = reader.readLine()) != null) {
	    	builder.append(line);
	    }
	    
	    String[] data = builder.toString().split("&");
	    String did = data[0].split("=")[1];
	    String session = data[1].split("=")[1];
	    String query = data[2].split("=")[1];
	    
	    LogService.log(did,session,"click",query);
	}
}
