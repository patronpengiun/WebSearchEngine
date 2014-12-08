package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.Vector;
import java.net.URLDecoder;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * Handles each incoming query, students do not need to change this class except
 * to provide more query time CGI arguments and the HTML output.
 * 
 * N.B. This class is not thread-safe. 
 * 
 * @author congyu
 * @author fdiaz
 */
class QueryHandler implements HttpHandler {

	/** html provide the html file displaying search results **/
	private static File html = new File("content/search.html");
	
	/** html_ending is the ending html tags**/
	private static String html_ending = "</body>\n</html>";
	
  /**
   * CGI arguments provided by the user through the URL. This will determine
   * which Ranker to use and what output format to adopt. For simplicity, all
   * arguments are publicly accessible.
   */
  public static class CgiArguments {
    // The raw user query
    public String _query = "";
    // How many results to return
    private int _numResults = 10;
    
    // The type of the ranker we will be using.
    public enum RankerType {
      NONE,
      FULLSCAN,
      CONJUNCTIVE,
      FAVORITE,
      COSINE,
      PHRASE,
      QL,
      LINEAR,
      COMPREHENSIVE,
    }
    public RankerType _rankerType = RankerType.NONE;
    
    // The output format.
    public enum OutputFormat {
      TEXT,
      HTML,
    }
    public OutputFormat _outputFormat = OutputFormat.TEXT;
	private int _numdocs = 10;
	private int _numterms = 10;

    public CgiArguments(String uriQuery) {
      String[] params = uriQuery.split("&");
      for (String param : params) {
        String[] keyval = param.split("=", 2);
        if (keyval.length < 2) {
          continue;
        }
        String key = keyval[0].toLowerCase();
        String val = keyval[1];
        if (key.equals("query")) {
          _query = val;
        } else if (key.equals("num")) {
          try {
            _numResults = Integer.parseInt(val);
          } catch (NumberFormatException e) {
            // Ignored, search engine should never fail upon invalid user input.
          }
        } else if (key.equals("ranker")) {
          try {
            _rankerType = RankerType.valueOf(val.toUpperCase());
          } catch (IllegalArgumentException e) {
            // Ignored, search engine should never fail upon invalid user input.
          }
        } else if (key.equals("format")) {
          try {
            _outputFormat = OutputFormat.valueOf(val.toUpperCase());
          } catch (IllegalArgumentException e) {
            // Ignored, search engine should never fail upon invalid user input.
          }
        } else if (key.equals("numdocs")){
        	try{
        		_numdocs = Integer.parseInt(val);
        	}catch (NumberFormatException e) {
                // Ignored, search engine should never fail upon invalid user input.
            }
        } else if (key.equals("numterms")){
        	try{
        		_numterms = Integer.parseInt(val);
        	}catch (NumberFormatException e) {
                // Ignored, search engine should never fail upon invalid user input.
            }
        }      
        
      }  // End of iterating over params
    }
  }

  // For accessing the underlying documents to be used by the Ranker. Since 
  // we are not worried about thread-safety here, the Indexer class must take
  // care of thread-safety.
  private Indexer _indexer;
  private FinalProject _pj;

  public QueryHandler(Options options, Indexer indexer, FinalProject pj) {
    _indexer = indexer;
    _pj = pj;
  }

  private void respondWithMsg(HttpExchange exchange, final String message)
      throws IOException {
    Headers responseHeaders = exchange.getResponseHeaders();
    String uriQuery = exchange.getRequestURI().getQuery();
    CgiArguments cgiArgs = new CgiArguments(uriQuery);
    String contentType = (cgiArgs._outputFormat == CgiArguments.OutputFormat.HTML) ? "text/html" : "text/plain";
    responseHeaders.set("Content-Type", contentType + "; charset=UTF-8");
    exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
    OutputStream responseBody = exchange.getResponseBody();
    responseBody.write(message.getBytes("UTF-8"));
    responseBody.close();
  }

  private void constructTextOutput(
		  final Vector<ScoredDocument> docs, StringBuffer response) {
	  for (ScoredDocument doc : docs) {
		  response.append(response.length() > 0 ? "\n" : "");
	      response.append(doc.asTextResult());
	  }
	  response.append(response.length() > 0 ? "\n" : "");
  }
  
  private void constructHtmlOutput(
		  final Vector<ScoredDocument> scoredDocs, StringBuffer response, boolean flag, String corrected) {
	  FileInputStream in = null;
	  try {
			in = new FileInputStream(html);
	  } catch (FileNotFoundException e) {
			e.printStackTrace();
	  }
	  BufferedReader reader = new BufferedReader(new InputStreamReader(in));
	    
	  String line = null;
	    
	  StringBuilder builder = new StringBuilder();
	  try {
		  while ((line = reader.readLine()) != null) {
			  builder.append(line);
			  builder.append("\n");
		  }
	  } catch (IOException e) {
		e.printStackTrace();
	  }
	  try {
		  reader.close();
	  } catch (IOException e) {
		e.printStackTrace();
	  }
	  response.append(builder.toString() + "\n");
	  if (flag)
		  response.append(HtmlUtil.generateSpellCorrection(corrected));
	  
	  
	  for (ScoredDocument doc : scoredDocs) {
		  String url = doc.get_doc().getUrl();
		  String title = doc.get_doc().getTitle();
		  response.append("<a href='http://en.wikipedia.org/wiki/" + url + "' target='_blank'>" + title + "</a></p>" + "\n");
//	      response.append(doc.asTextResult());
	  }
	  response.append(response.length() > 0 ? "\n" : "");	  
	  response.append(html_ending);	 
  }

  public void handle(HttpExchange exchange) throws IOException {
    String requestMethod = exchange.getRequestMethod();
    if (!requestMethod.equalsIgnoreCase("GET")) { // GET requests only.
      return;
    }

    // Print the user request header.
    Headers requestHeaders = exchange.getRequestHeaders();
    System.out.print("Incoming request: ");
    for (String key : requestHeaders.keySet()) {
      System.out.print(key + ":" + requestHeaders.get(key) + "; ");
    }
    System.out.println();

    // Validate the incoming request.
    String uriQuery = exchange.getRequestURI().getQuery();
    String uriPath = exchange.getRequestURI().getPath();
    if (uriPath == null || uriQuery == null) {
      respondWithMsg(exchange, "Something wrong with the URI!");
    }
    if (!uriPath.equals("/search")  && !uriPath.equals("/prf")) {
      respondWithMsg(exchange, "Only /search or /prf is handled!");
    }
    System.out.println("Query: " + uriQuery);

    // Process the CGI arguments.
    CgiArguments cgiArgs = new CgiArguments(uriQuery);
    if (cgiArgs._query.isEmpty()) {
      respondWithMsg(exchange, "No query is given!");
    }

    // Create the ranker.
    Ranker ranker = Ranker.Factory.getRankerByArguments(
        cgiArgs, SearchEngine.OPTIONS, _indexer);
    if (ranker == null) {
      respondWithMsg(exchange,
          "Ranker " + cgiArgs._rankerType.toString() + " is not valid!");
    }

    // Processing the query.
    Query processedQuery = new QueryPhrase(cgiArgs._query);
    processedQuery.processQuery();

    // Ranking.
    if(uriPath.equals("/search")){
    	Vector<ScoredDocument> scoredDocs =
    			ranker.runQuery(processedQuery, cgiArgs._numResults);
    	
    	String original = URLDecoder.decode(cgiArgs._query).toLowerCase();
    	_pj.recordQuery(original);
    	boolean[] flag = new boolean[1];
    	String corrected = correct(original,flag);
    	if (flag[0])
    		System.out.println("correct query from " + cgiArgs._query + " to " + corrected);
    			
    	StringBuffer response = new StringBuffer();
    	switch (cgiArgs._outputFormat) {
    	case TEXT:
    		constructTextOutput(scoredDocs, response);
    		break;
    	case HTML:
    		// @CS2580: Plug in your HTML output
    		constructHtmlOutput(scoredDocs, response, flag[0], corrected);
    		break;
    	default:
    		// nothing
    	}
    
    	respondWithMsg(exchange, response.toString());
    	System.out.println("Finished query: " + cgiArgs._query);
    }
    else if (uriPath.equals("/prf")){
    	Vector<ScoredDocument> scoredDocs =
    			ranker.runQuery(processedQuery, cgiArgs._numdocs);

    	System.out.println(scoredDocs.size());
    	PrfCalculator calculator = new PrfCalculator(scoredDocs,
    			_indexer._options._corpusPrefix, _indexer, cgiArgs._numterms);
    	List<PrfCalculator.ProbEntry> list = calculator.compute();
		StringBuffer response = new StringBuffer();
		
		double sum = 0.0;
		for(PrfCalculator.ProbEntry e: list) {
			sum += e.prob;
		}
		for(PrfCalculator.ProbEntry e: list) {
			response.append(e.token + "\t" + e.prob/sum + "\n");
		}
		respondWithMsg(exchange, response.toString());
		System.out.println("Finished query: " + cgiArgs._query);
    }
    
    
  }


  private String correct(String original, boolean[] flag) {
	  boolean f = false;
	  String[] temp = original.split("\\s");
	  
	  if (temp.length == 0) {
		  flag[0] = false;
		  return "";
	  }
	  
	  String result = "";
	  for (int i=0;i<temp.length;i++) {
		  if (!NGramCorrector.isValid(temp[i])) {
			  result += temp[i] + " ";
		  } else {
			  String corrected = _pj.correct(temp[i]);
			  if (!corrected.equals(temp[i]))
				  f = true;
			  result += corrected + " ";
		  }
	  }
	  flag[0] = f;
	  return result.substring(0,result.length()-1);
  }
}

