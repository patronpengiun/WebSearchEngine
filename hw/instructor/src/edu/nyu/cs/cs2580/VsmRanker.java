package edu.nyu.cs.cs2580;

import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Vector;

public class VsmRanker extends Ranker {
	public VsmRanker(Index i) {
		super(i);
	}
	
	@Override
	public ScoredDocument runquery(String query, int did){
		System.out.println(did);
		
		HashMap<Integer,Double> query_vsm;
		HashMap<Integer,Double> document_vsm;

	    // Build query vector
	    Scanner s = new Scanner(query);
	    Vector < String > qv = new Vector < String > ();
	    while (s.hasNext()){
	      String term = s.next();
	      qv.add(term);
	    }
	    query_vsm = Document.getQueryNormSpaceVector(qv, _index.numDocs());
	    
	    // Get the document vector.
	    Document d = _index.getDoc(did);
	    document_vsm = d.getNormSpaceVector(_index.numDocs());

	    double cosine = VectorHelper.cosineOf(query_vsm, document_vsm);

	    s.close();
	    return new ScoredDocument(did, d.get_title_string(), cosine);
	  }
}
