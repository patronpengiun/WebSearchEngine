package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.*;
import java.net.URLDecoder;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.google.gson.Gson;

public class LookupHandler implements HttpHandler{
	private FinalProject pj;
	
	public LookupHandler(FinalProject pj) {
		this.pj = pj;
	}
	
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
	    String prefix;
	    if (data.length > 1)
	    	prefix = URLDecoder.decode(data[1]);
	    else
	    	prefix = "";
	    
	    List<String> ret = pj.getLookup(prefix);
	    String[] result = new String[ret.size()];
	    Object[] objects = ret.toArray();
	    for (int i=0;i<objects.length;i++) {
	    	result[i] = (String)objects[i];
	    }
	    
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
