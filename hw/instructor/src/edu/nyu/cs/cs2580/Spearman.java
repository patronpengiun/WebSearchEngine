package edu.nyu.cs.cs2580;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Spearman {

	public static void main(String[] args) {

		try {
			Map<String, Double> pageRankList = deserializePagerank(args[0]);
			Map<String, Integer> numViewsList = deserializeNumviews(args[1]);
			double score = getScore(pageRankList, numViewsList);
			System.out.println("Spearman Score: " + score);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static double getScore(Map<String, Double> pageranks,
			Map<String, Integer> numviews) {

		double z = 0;
		double zsum = 0;
		Set<String> prset = pageranks.keySet();
		
		for (String url: prset) {
			zsum += pageranks.get(url);
		}
		z = zsum / pageranks.size();
		
		double sum = 0;
		double xProductSum = 0;
		double yProductSum = 0;
		
		int numviewSize = numviews.keySet().size();
		for (String durl : prset) {
			Double xk = pageranks.get(durl);
			Integer yk = null;
			if (numviews.containsKey(durl))
				yk = numviews.get(durl);
			else
				yk = numviewSize++;

			sum += ((xk - z) * (xk - z));
			xProductSum += Math.sqrt(pageranks.get(durl) - z);
			yProductSum += Math.sqrt(numviews.get(durl) - z);
		}
	
		return sum / (xProductSum * yProductSum);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Double> deserializePagerank(String string)
			throws IOException {
		Map<String, Double> pageranks = new HashMap<String, Double>();
		try {
			FileInputStream fileIn = new FileInputStream(string);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			pageranks = (Map<String, Double>) in.readObject();

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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return numviews;
	}
}