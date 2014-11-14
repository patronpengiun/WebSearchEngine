package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Spearman {

	public static void main(String[] args) {

		try {
			List<PageRank> pageRankList = initializePageRanks(args[0]);
			List<NumViews> numViewsList = initializeNumViews(args[1]);
			double score = getScore(pageRankList, numViewsList);
			System.out.println("Spearman Score: " + score);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static double getScore(List<PageRank> pageRankList,
			List<NumViews> numViewsList) {
		Collections.sort(pageRankList, new PageRankComparator());
		Collections.sort(numViewsList, new NumViewsComparator());

		Map<Integer, Integer> prRanks = new HashMap<Integer, Integer>();
		Map<Integer, Integer> nvRanks = new HashMap<Integer, Integer>();

		int i = 1;
		for (PageRank d : pageRankList) {
			prRanks.put(d.getDocid(), i);
			i++;
		}
		pageRankList.clear();

		i = 1;
		for (NumViews d : numViewsList) {
			nvRanks.put(d.getDocid(), i);
			i++;
		}

		numViewsList.clear();
		double sum = 0;
		Set<Integer> set = prRanks.keySet();

		for (int did : set) {
			int xk = prRanks.get(did);
			int yk;
			if (nvRanks.containsKey(did))
				yk = nvRanks.get(did);
			else
				yk = i++;

			sum += ((xk - yk) * (xk - yk));
		}

		double num = 6 * sum;
		double denom = set.size() * (Math.pow(set.size(), 2) - 1);
		double output = (1 - (num / denom));

		return output;
	}

	private static List<NumViews> initializeNumViews(String string)
			throws NumberFormatException, IOException {
		List<NumViews> numViewsList = new ArrayList<NumViews>();
		BufferedReader ois = new BufferedReader(new FileReader(string));
		String o;

		while (((o = ois.readLine()) != null)) {
			String[] eachLine = o.split(" ");
			String docName = eachLine[0];
			int docid = Integer.parseInt(eachLine[2]);
			int temp = Integer.parseInt(eachLine[1]);

			NumViews numViews = new NumViews(docid, docName, temp);
			numViewsList.add(numViews);
		}
		ois.close();

		return numViewsList;
	}

	private static List<PageRank> initializePageRanks(String string)
			throws IOException {
		List<PageRank> pageRankList = new ArrayList<PageRank>();

		StringBuilder builder = new StringBuilder(string);
		BufferedReader ois = new BufferedReader(new FileReader(
				builder.toString()));
		String o;

		while (((o = ois.readLine()) != null)) {
			String[] eachLine = o.split(" ");
			int docid = Integer.parseInt(eachLine[0]);
			double pr = Double.parseDouble(eachLine[1]);

			PageRank tmp = new PageRank(docid, pr);
			pageRankList.add(tmp);
		}
		ois.close();

		return pageRankList;
	}
}