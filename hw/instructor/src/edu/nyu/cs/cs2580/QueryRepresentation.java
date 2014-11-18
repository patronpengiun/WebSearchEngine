package edu.nyu.cs.cs2580;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;

//query form http://<HOST>:<PORT>/prf?query=<QUERY>&ranker=<RANKER-TYPE>&numdocs=<INTEGER>&numterms=<INTEGER>

public class QueryRepresentation {
	public static Map<String, Integer> map;
	
	public static Map<String, Double> compute(Vector<ScoredDocument> scoredDocs, Query query, Indexer indexer, int _numTerms ){
		map = new HashMap<String, Integer>();
		int totalWords = 0;
		try{
			for (ScoredDocument doc : scoredDocs) {
				int docid = doc.get_docId();
				DocumentIndexed documentIndexed = (DocumentIndexed) (indexer
						.getDoc(docid));
				System.out.println(documentIndexed.word_freq().size());
				totalWords += documentIndexed.get_totalWords();
			}
			//calculate word frequency
			for (ScoredDocument doc : scoredDocs) {
				int docid = doc.get_docId();
				DocumentIndexed documentIndexed = (DocumentIndexed) (indexer
						.getDoc(docid));
				Map<String, Integer> curWordFrequency = countFreq(doc) ;
				for (String term : curWordFrequency.keySet()){
					int frequency = curWordFrequency.get(term);
					if (map.containsKey(term)){
						frequency += map.get(term);
					}
					map.put(term, frequency);
				}
			}
			System.out.println(map.size()+"???????");
			System.out.println(totalWords);
			
			return firstMTerm(totalWords,query,_numTerms);
		} catch(Exception e){
			
		}
		
		return null;
	}
	
	private static Map<String, Integer> countFreq(ScoredDocument doc){
		Map<String, Integer> result = new HashMap<String, Integer>();
		try{		
				
				Scanner scanner = new Scanner(new File(doc.get_doc().getUrl()));
				Scanner sc = scanner.useDelimiter("\\s+");
				Stemmer stemmer = new Stemmer();
				while (sc.hasNext()){
					  String token = stem(sc.next(),stemmer);
					 if(token.equals("")) {
						  continue;
					  } else {
						  	if(map.containsKey(token)){
						  		int tmp = result.get(token) + 1;
						  		result.put(token, tmp);
						  	}
						  	else{
						  		result.put(token, 1);
							  	}
					}
				}
				scanner.close();
		}	catch(Exception e){
			
		}
		return result;
	}
	
	 private static String stem(String origin, Stemmer stemmer) {
		  String lower = origin.toLowerCase();
	      stemmer.add(lower.toCharArray(), lower.length());
	      stemmer.stem();
	      return stemmer.toString();
	  }	
	
	
	private static Map<String, Double> firstMTerm(int totalWords, Query query, int m ){

		Map<String, Double> probabilityMap = new HashMap<String, Double>();

		for (String term : map.keySet()) {
			int wordFreq = map.get(term);
			double probability = wordFreq / (double) totalWords;
			probabilityMap.put(term, probability);
		}
		
		List<Entry<String, Double>> list = sortProbabilityMap(probabilityMap);
		
		double firstMTermProb = 0;
		for (int i = 0 ; i < m && i < list.size() ; i ++){
			firstMTermProb += list.get(i).getValue();
		}
		
		TreeMap<String, Double> normalizedMap = new TreeMap<String, Double>((Map<? extends String, ? extends Double>) new TreeMapComp());
		for (int i = 0 ; i < m && i < list.size() ; i ++){
			normalizedMap.put(list.get(i).getKey(), list.get(i).getValue() / firstMTermProb);
		}

		return normalizedMap;
	}


	private static List<Entry<String, Double>> sortProbabilityMap(Map<String, Double> probabilityMap) {
		List<Map.Entry<String, Double>> list =
			    new ArrayList<Map.Entry<String, Double>>(probabilityMap.entrySet());

			Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {   
			    public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {      
			    	if (o2.getValue() - o1.getValue() > 0){
			    		return 1;
			    	}else if (o2.getValue() - o1.getValue() < 0){
			    		return  -1;
			    	}else{
			    		return 0;
			    	}
			    }
			}); 
			return list;
		
	}
	
	private static class TreeMapComp implements Comparator<Map.Entry<String, Double>>{

		@Override
		public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
			if (o2.getValue() - o1.getValue() > 0){
	    		return 1;
	    	}else if (o2.getValue() - o1.getValue() < 0){
	    		return  -1;
	    	}else{
	    		return 0;
	    	}
		}
		
	}
	
}
