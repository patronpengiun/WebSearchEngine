package edu.nyu.cs.cs2580;

import java.util.Scanner;
import java.util.Vector;

public class LinearRanker extends Ranker{
	public LinearRanker(Index i) {
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
	    
	    double arg1 = 0.3;
	    double arg2 = 0.3;
	    double arg3 = 0.3;
	    double arg4 = 0.0001;
	    
	    Document d = _index.getDoc(did);
	    double cosine = VectorHelper.cosineOf(Document.getQueryNormSpaceVector(qv, _index.numDocs()),d.getNormSpaceVector(_index.numDocs()));
	    double likelihood = d.getQueryLikelihood(qv,0.5);
	    double phraseCount = d.getPhraseScore(qv);
	    double numViews = d.get_numviews();
	    
	    double score = arg1 * cosine + arg2 * likelihood + arg3 * phraseCount + arg4 * numViews;

	    s.close();
	    return new ScoredDocument(did, d.get_title_string(), score);
	}
}
