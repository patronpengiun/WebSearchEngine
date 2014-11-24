package edu.nyu.cs.cs2580;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Spearman {
	
	/**
	 * Parameters: serialized pagerank.idx and numviews.idx
	 * 
	 * Computes the Spearman correlation of given pagerank and numviews
	 * 
	 * Usage:java -cp Spearman data/index/pagerank.idx data/index/numviews.idx
	 * 
	 * **/
	public static void main(String[] args) {

		try {
			Map<String, Float> pagerankMap = deserializePagerank(args[0]);
			Map<String, Integer> numviewMap = deserializeNumviews(args[1]);
			Object[] sortedPr = sortPagerank(pagerankMap);
			Object[] sortedNv = sortNumviews(numviewMap);
			
			Map<String, Integer> pageranks = mapToPagerank(pagerankMap, sortedPr);
			Map<String, Integer> numviews = mapToNumviews(numviewMap, sortedNv);
			
			float score = getScore(pageranks, numviews);
			System.out.println("Spearman Score: " + score);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static float getScore(Map<String, Integer> pageranks,
			Map<String, Integer> numviews) {

		float z = 0;
		float zsum = 0;
		Set<String> prset = pageranks.keySet();
		
		for (String url: prset) {
			zsum += pageranks.get(url);
		}
		z = zsum / pageranks.size();
		
		float sum = 0;
		float xProductSum = 0;
		float yProductSum = 0;
		
		int numviewSize = numviews.keySet().size();
		for (String durl : prset) {
			Integer xk = pageranks.get(durl);
			Integer yk = null;
			if (numviews.containsKey(durl))
				yk = numviews.get(durl);
			else
				yk = numviewSize++;
			sum += ((xk - z) * (yk - z));
			xProductSum += Math.pow((double)(pageranks.get(durl) - z), 2);
			yProductSum += Math.pow((double)(numviews.get(durl) - z), 2);
		}
	
		return sum / (float)Math.sqrt(xProductSum * yProductSum);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Float> deserializePagerank(String string)
			throws IOException {
		Map<String, Float> pageranks = new HashMap<String, Float>();
		try {
			FileInputStream fileIn = new FileInputStream(string);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			pageranks = (Map<String, Float>) in.readObject();

			in.close();
			fileIn.close();       
		} catch(IOException i) {
		         i.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return pageranks;
	}
	
	@SuppressWarnings("unchecked")
	private static Map<String, Integer> deserializeNumviews(String string)
			throws NumberFormatException, IOException {
		Map<String, Integer> numviews = new HashMap<String, Integer>();
		try {
			FileInputStream fileIn = new FileInputStream(string);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			numviews = (Map<String, Integer>) in.readObject();
			in.close();
			fileIn.close();       
		} catch(IOException i) {
		         i.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return numviews;
	}
	
	private static Object[] sortPagerank(Map<String, Float> pagerankMap) {
		
        PagerankComparator pc =  new PagerankComparator(pagerankMap);
        TreeMap<String,Float> sorted_map = new TreeMap<String, Float>(pc);

        sorted_map.putAll(pagerankMap);
        
        Object[] a = sorted_map.keySet().toArray();
     
        return a;
	}
	
	private static Object[] sortNumviews(Map<String, Integer> numviewMap) {
		
        NumviewComparator nvc =  new NumviewComparator(numviewMap);
        TreeMap<String,Integer> sorted_map = new TreeMap<String, Integer>(nvc);

        sorted_map.putAll(numviewMap);
        
        Object[] a = sorted_map.keySet().toArray();
        
        return a;
	}
	
	private static Map<String, Integer> mapToPagerank(Map<String, Float> pagerankMap, Object[] sortedPr) {
		Map<String, Integer> result = new HashMap<String, Integer>();
		for (int i = 0; i < sortedPr.length; i++) {
			String key = (String) sortedPr[i];
			result.put(key, Integer.valueOf(i + 1));
		}		
		return result;
	}
	
	private static Map<String, Integer> mapToNumviews(Map<String, Integer> numviewMap, Object[] sortedNv) {
		Map<String, Integer> result = new HashMap<String, Integer>();
		for (int i = 0; i < sortedNv.length; i++) {
			String key = (String) sortedNv[i];
			result.put(key, Integer.valueOf(i + 1));
		}
		return result;
	}

}