package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.*;

public class BhattacharyyaHelper {
	private HashMap<String,HashMap<String,Double>> expandedQueries;
	private ArrayList<String> queries;
	private String outputfileName;
	
	public BhattacharyyaHelper(String inputfileName, String outputfileName) {
		try {
			loadExpandedQueries(inputfileName);
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
		this.outputfileName = outputfileName;
	}
	
	public void generateOutput() throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputfileName),"UTF-8"));
		for (String token1: queries) {
			for (String token2: queries) {
				if (!token1.equals(token2)) {
					writer.write(token1);
					writer.write("\t");
					writer.write(token2);
					writer.write("\t");
					writer.write(Double.toString(getCoefficient(token1,token2)));
					writer.newLine();
				}
			}
		}
		writer.close();
	}
	
	private void loadExpandedQueries(String filename) throws IOException {
		expandedQueries = new HashMap<String,HashMap<String,Double>>();
		queries = new ArrayList<String>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
		String line = null;
		while ((line = reader.readLine()) != null) {
			String[] arr = line.split(":");
			expandedQueries.put(arr[0], loadSingleExpandedQuery(arr[1]));
			queries.add(arr[0]);
		}
		reader.close();
	}
	
	private double getCoefficient(String qa, String qb) {
		HashMap<String,Double> expanded_a = expandedQueries.get(qa);
		HashMap<String,Double> expanded_b = expandedQueries.get(qb);
		HashSet<String> set = new HashSet<String>(expanded_a.keySet());
		set.addAll(expanded_b.keySet());
		
		double sum = 0;
		for (String token: set) {
			double prob_a = expanded_a.get(token) != null ? expanded_a.get(token) : 0.0;
			double prob_b = expanded_b.get(token) != null ? expanded_b.get(token) : 0.0;
			sum += Math.sqrt(prob_a * prob_b);
		}
		return sum;
	}
	
	private HashMap<String,Double> loadSingleExpandedQuery(String filename) throws IOException{
		HashMap<String,Double> result = new HashMap<String,Double>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
		String line = null;
		while ((line = reader.readLine()) != null) {
			String[] arr = line.split("\t");
			result.put(arr[0], Double.parseDouble(arr[1]));
		}
		reader.close();
		return result;
	}
}
