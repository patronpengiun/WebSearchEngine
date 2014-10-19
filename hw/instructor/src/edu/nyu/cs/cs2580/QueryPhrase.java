package edu.nyu.cs.cs2580;

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

  @Override
  public void processQuery() {
	  char[] a = _query.toCharArray();
  }
}
