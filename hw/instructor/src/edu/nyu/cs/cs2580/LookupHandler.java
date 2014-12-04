package edu.nyu.cs.cs2580;

import java.io.*;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.google.gson.Gson;

public class LookupHandler implements HttpHandler{
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		String requestMethod = exchange.getRequestMethod();
	    if (!requestMethod.equalsIgnoreCase("POST")) { // POST requests only.
	      return;
	    }
	    
	    BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
	    String line = null;
	    StringBuilder builder = new StringBuilder();
	    while ((line = reader.readLine()) != null) {
	    	builder.append(line);
	    }
	    
	    String[] data = builder.toString().split("=");
	    String prefix = data[0];
	    String[] result = new String[]{prefix+"a", prefix+"b", prefix+"c"};
	    
	    Gson gson = new Gson();
	    String jsonStr = gson.toJson(result);
	    
	    Headers responseHeaders = exchange.getResponseHeaders();
	    responseHeaders.set("Content-Type", "application/json; charset=UTF-8");
	    exchange.sendResponseHeaders(200, 0); 
	    OutputStream responseBody = exchange.getResponseBody();
	    responseBody.write(jsonStr.getBytes("UTF-8"));
	    responseBody.close();
	}
}
