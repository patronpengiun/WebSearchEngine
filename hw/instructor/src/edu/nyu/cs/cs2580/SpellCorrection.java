package edu.nyu.cs.cs2580;


import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.math.*;

public class SpellCorrection{
	public static void main(String[] args) throws IOException {
		SpellCorrection spell = new SpellCorrection();
		String word = "votka";
		long startTime = System.nanoTime();
		String see = spell.correct(word);
		long endTime = System.nanoTime();
		System.out.println(see);
		System.out.println("took "+(endTime - startTime)*1.0/1000000000L + " s"); 
		
		//----------------------
		
		String result = "none";
		int minDistance = Integer.MAX_VALUE;
		int maxWeight = Integer.MIN_VALUE;
		startTime = System.nanoTime();
		DamerauLevensteinMetric m = new DamerauLevensteinMetric(); 
		for (String str: spell.dict.keySet()) {
			int distance = m.getDistance(word, str);
			if (distance < minDistance) {
				minDistance = distance;
				result = str;
			} else if (distance == minDistance && spell.dict.get(str) > maxWeight) {
				maxWeight = spell.dict.get(str);
				result = str;
			}
		}
		endTime = System.nanoTime();
		System.out.println(result);
		System.out.println("took "+(endTime - startTime)*1.0/1000000000L + " s"); 
		System.out.println("edit distance: " + minDistance);
		
		// ---------------------
		
		NGramCorrector cc = new NGramCorrector(3);
		cc.construct(spell.dict.keySet());
		startTime = System.nanoTime();
		result = cc.correct(word, spell.dict);
		endTime = System.nanoTime();
		System.out.println(result);
		System.out.println("took "+(endTime - startTime)*1.0/1000000000L + " s");
		
		// ---------------------
		
		NGramCorrector ccc = new NGramCorrector(3);
		ccc.construct(spell.dict.keySet());
		startTime = System.nanoTime();
		result = ccc.correct(word, spell.dict);
		endTime = System.nanoTime();
		System.out.println(result);
		System.out.println("took "+(endTime - startTime)*1.0/1000000000L + " s");
	}
	
	private HashMap<String, Integer> dict = new HashMap<String, Integer>();

	 
	public static final char[] c = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h',
				'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u',
				'v', 'w', 'x', 'y', 'z'};
	
	public SpellCorrection() throws IOException {
		
			BufferedReader read = new BufferedReader(new FileReader("englishwords.txt"));
			Pattern p = Pattern.compile("\\w+");
			for(String temp = ""; temp != null; temp = read.readLine()){
				Matcher m = p.matcher(temp.toLowerCase());
				while(m.find()) 
					dict.put((temp = m.group()),(int)(Math.random()*2));
			}
			read.close();
		
	}
	
	private final ArrayList<String> edits(String word) {
		ArrayList<String> result = new ArrayList<String>();
		int len = word.length();
		//delete
		for(int i=0; i < word.length(); ++i) 
			result.add(word.substring(0, i) + word.substring(i+1));
		//transposition
		for(int i=0; i < word.length()-1; ++i) 
			result.add(word.substring(0, i) + word.charAt(i+1) + word.charAt(i) + word.substring(i+2));
		//alternate
		for(int i=0; i < word.length(); ++i) 
			for (int j = 0; j < 26; j++)
				result.add(word.substring(0, i) + c[j] + word.substring(i + 1, len));
		//insert
		for(int i=0; i <= word.length(); ++i) 
			for (int j = 0; j < 26; j++)
				result.add(word.substring(0, i) + c[j] + word.substring(i, len));
		return result;
	}
	
	public String correct(String word){
		if(dict.containsKey(word)) 
		      return word;

		    ArrayList<String> list = edits(word);  // Everything edit distance 1 from word
		    HashMap<Integer, String> candidates = new HashMap<Integer, String>();

		    // Find all things edit distance 1 that are in the dictionary.  Also remember
		    //   their frequency count from nWords.  
		    // (Note if equal frequencies the last one will be the one remembered.)
		    for(String s : list) 
		      if(dict.containsKey(s)) 
		        candidates.put(dict.get(s),s);

		    // If found something edit distance 1 return the most frequent word
		    if(candidates.size() > 0)   
		      return candidates.get(Collections.max(candidates.keySet()));

		    // Find all things edit distance 1 from everything of edit distance 1.  These
		    // will be all things of edit distance 2 (plus original word).  Remember frequencies
		    for(String s : list) 
		      for(String w : edits(s)) 
		        if(dict.containsKey(w)) 
		          candidates.put(dict.get(w),w);
		    
		    if(candidates.size() > 0)   
			      return candidates.get(Collections.max(candidates.keySet()));
		    
		    /*for(String s : list) 
			      for(String w : edits(s)) 
			    	  for (String next: edits(w))
			    		  if(dict.containsKey(next)) 
			    			  candidates.put(dict.get(next),next);*/
		    
		    // If found something edit distance 2 return the most frequent word.
		    // If not return the word with a "?" prepended.  (Original just returned the word.)
		    return candidates.size() > 0 ? 
		        candidates.get(Collections.max(candidates.keySet())) : "?" + word;
	}
	
}
