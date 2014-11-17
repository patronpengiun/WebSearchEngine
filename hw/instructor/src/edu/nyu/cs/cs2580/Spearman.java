package edu.nyu.cs.cs2580;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Spearman {
	
	/**
	 * Parameters: serialized pagerank.idx and numviews.idx
	 * 
	 * Computes the Spearman correlation of given pagerank and numviews
	 * 
	 * Usage:java -cp Spearman index/pagerank.idx indes/numviews.idx
	 * 
	 * **/
	public static void main(String[] args) {

		try {
			Map<String, Float> pageRankList = deserializePagerank(args[0]);
			Map<String, Integer> numViewsList = deserializeNumviews(args[1]);
			float score = getScore(pageRankList, numViewsList);
			System.out.println("Spearman Score: " + score);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static float getScore(Map<String, Float> pageranks,
			Map<String, Integer> numviews) {

		float z = 0;
		float zsum = 0;
		Set<String> prset = pageranks.keySet();
		
		for (String url: prset) {
			zsum += pageranks.get(url);
		}
		z = zsum / pageranks.size();
//		System.out.println("z: " + z);
		
		float sum = 0;
		float xProductSum = 0;
		float yProductSum = 0;
		
		int numviewSize = numviews.keySet().size();
		for (String durl : prset) {
			Float xk = pageranks.get(durl);
			Integer yk = null;
			if (numviews.containsKey(durl))
				yk = numviews.get(durl);
			else
				yk = numviewSize++;
			sum += ((xk - z) * (xk - z));
			xProductSum += Math.pow((double)(pageranks.get(durl) - z), 2);
			yProductSum += Math.pow((double)(numviews.get(durl) - z), 2);
			System.out.println("Sum: " + sum);
			System.out.println("xProductSum: " + xProductSum);
			System.out.println("yProductSum: " + yProductSum);
		}
	
		return sum / (xProductSum * yProductSum);
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
			// TODO Auto-generated catch block
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
}