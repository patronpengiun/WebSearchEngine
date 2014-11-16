package edu.nyu.cs.cs2580;

import java.util.Collections;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW3 based on your {@code RankerFavorite}
 * from HW2. The new Ranker should now combine both term features and the
 * document-level features including the PageRank and the NumViews. 
 */
public class RankerComprehensive extends Ranker {

  public RankerComprehensive(Options options,
      CgiArguments arguments, Indexer indexer) {
    super(options, arguments, indexer);
    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
  }

  @Override
  public Vector<ScoredDocument> runQuery(Query query, int numResults) {
	  Queue<ScoredDocument> rankQueue = new PriorityQueue<ScoredDocument>();
	  Document doc = null;
	  int docid = -1;
	  double alpha = 0.8;
	  double beta = 0.1;
	  double sigma = 0.01;
	  while ((doc = _indexer.nextDoc(query, docid)) != null) {
		  rankQueue.add(new ScoredDocument(doc, 
				  	alpha*getQLScore(query,doc) + beta * doc.getPageRank() + sigma * doc.getNumViews()));
		  if (rankQueue.size() > numResults) {
			  rankQueue.poll();
		  }
	      docid = doc._docid;
	  }
	  
	  Vector<ScoredDocument> results = new Vector<ScoredDocument>();
	  ScoredDocument scoredDoc = null;
	  while ((scoredDoc = rankQueue.poll()) != null) {
		  results.add(scoredDoc);
	  }
	  Collections.sort(results, Collections.reverseOrder());
	  return results;
  }
  
  
  //get QL rank score
  private double getQLScore(Query query, Document document) {
	  DocumentIndexed doc = (DocumentIndexed)document;
	  double result = 0;
	  double lambda = 0.5;
	  for (String token: query._tokens) {
		  int seen_factor = _indexer.documentTermFrequency(token, doc._docid);
		  double prob = (1-lambda) * seen_factor / doc.get_totalWords() 
				  + lambda * _indexer.corpusTermFrequency(token) / _indexer._totalTermFrequency;
		  result += Math.log(prob) / Math.log(2);
	  }
	  return result;
  }
}
