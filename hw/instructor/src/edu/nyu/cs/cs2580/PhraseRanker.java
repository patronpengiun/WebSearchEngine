package edu.nyu.cs.cs2580;

import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Vector;

public class PhraseRanker extends Ranker {
	public PhraseRanker(Index i) {
		super(i);
	}
	
	@Override
	public ScoredDocument runquery(String query, int did){
		System.out.println(did);
		
	    // Build query vector
	    Scanner s = new Scanner(query);
	    Vector < String > qv = new Vector < String > ();
	    while (s.hasNext()){
	      String term = s.next();
	      qv.add(term);
	    }
	    
	    Document d = _index.getDoc(did);
	    double score = d.getPhraseScore(qv);
	    
	    s.close();
	    return new ScoredDocument(did, d.get_title_string(), score);
	}
	
}
