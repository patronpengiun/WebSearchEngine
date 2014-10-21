package edu.nyu.cs.cs2580;

import java.util.Scanner;
import java.util.Vector;

/**
 * @CS2580: implement this class for HW2 to handle phrase. If the raw query is
 * ["new york city"], the presence of the phrase "new york city" must be
 * recorded here and be used in indexing and ranking.
 */
public class QueryPhrase extends Query {

  public QueryPhrase(String query) {
    super(query);
  }
  
  @Override
  public void processQuery() {
	  if (_query == null) {
	      return;
	  }
	  
	  int firstQuote;
	  int nextQuote;
	  while ((firstQuote = _query.indexOf("\"")) != -1) {
		  nextQuote = _query.indexOf("\"",firstQuote+1);
		  String phrase = _query.substring(firstQuote+1,nextQuote);
		  Scanner sc = new Scanner(phrase);
		  Vector<String> temp = new Vector<String>();
		  while (sc.hasNext()) {
			  String token = sc.next();
			  temp.add(token);
			  _tokens.add(token);
		  }
		  sc.close();
		  if (temp.size() > 1)
			  _phrases.add(temp);
		  _query = _query.substring(0,firstQuote) + _query.substring(nextQuote+1);
	  }
	  Scanner sc = new Scanner(_query);
	  while (sc.hasNext())
		  _tokens.add(sc.next());
	  sc.close();
  }
}
