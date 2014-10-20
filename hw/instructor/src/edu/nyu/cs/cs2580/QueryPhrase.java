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
  
  public Vector<String> _tokens = new Vector<String>();
  public Vector<Vector<String>> _phrases = new Vector<Vector<String>>(); 

  @Override
  public void processQuery() {
	  if (_query == null) {
	      return;
	  }
	  
	  char[] arr = _query.toCharArray();
	  boolean inPhrase = false;
	  StringBuilder tokenBuilder = new StringBuilder();
	  StringBuilder PhraseBuilder = new StringBuilder();
	  for (int i=0;i<arr.length;i++) {
		  if (arr[i] == '"') {
			  if (inPhrase) {
				  String token = tokenBuilder.toString();
				  if (!token.equals(""))
					  _tokens.add(token);
			  } else {
				  inPhrase = true;
			  }
		  }
	  }
  }
}
