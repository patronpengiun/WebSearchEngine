package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

class Evaluator {
	public final static double ONE = 1.0;
	private static double[] precisions = new double[3];
	private static double[] recalls = new double[3];
	private static double[] fMeasure = new double[3];
	private static double[] NDCG = new double[3];
	private static double[] precisionRecallPoint = new double[11];
	private static double reciprocal = 0.0;
	private static double averagePrecision = 0.0;
	private static List<Double> points = new ArrayList<Double>();
	private static String query;

	public static void main(String[] args) throws IOException {
		HashMap<String, HashMap<Integer, Double>> relevance_judgments = new HashMap<String, HashMap<Integer, Double>>();
		HashMap<String, HashMap<Integer, Double>> document_gain = new HashMap<String, HashMap<Integer, Double>>();
		if (args.length < 1) {
			System.out.println("need to provide relevance_judgments");
			return;
		}
		String p = args[0];
		// first read the relevance judgments into the HashMap
		readRelevanceJudgments(p, relevance_judgments, document_gain);
		// now evaluate the results from stdin
		evaluateStdin(relevance_judgments, document_gain);
		printResult();
	}

	private static void printResult() {
		String result = String.format("%s\t%-10f\t%-10f\t%-10f\t%-10f\t%-10f\t%-10f\t%-10f\t%-10f\t%-10f\t%-10f\t%-10f\t%-10f\t%-10f\t%-10f\t%-10f\t%-10f\t%-10f\t%-10f\t%-10f\t%-10f\t%-10f\t%-10f\t%-10f\t%-10f\t%-10f",
				query, precisions[0], precisions[1], precisions[2], recalls[0], recalls[1],
				recalls[2], fMeasure[0], fMeasure[1], fMeasure[2], precisionRecallPoint[0], precisionRecallPoint[1], precisionRecallPoint[2], precisionRecallPoint[3], precisionRecallPoint[4],
				precisionRecallPoint[5], precisionRecallPoint[6], precisionRecallPoint[7], precisionRecallPoint[8], precisionRecallPoint[9], precisionRecallPoint[10], averagePrecision, NDCG[0],
				NDCG[1], NDCG[2], reciprocal);
		System.out.println(result);

	}

	public static void readRelevanceJudgments(String p, HashMap<String, HashMap<Integer, Double>> relevance_judgments, HashMap<String, HashMap<Integer, Double>> document_gain) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(p));
			try {
				String line = null;
				while ((line = reader.readLine()) != null) {
					// parse the query,did,relevance line
					Scanner s = new Scanner(line).useDelimiter("\t");
					String query = s.next();
					int did = Integer.parseInt(s.next());
					String grade = s.next();
					double rel = 0.0;
					double value = 0.0;
					// convert to binary relevance
					if ((grade.equals("Perfect")) || (grade.equals("Excellent")) || (grade.equals("Good"))) {
						rel = 1.0;
					}
					if (grade.equals("Perfect")) {
						value = 10.0;
					} else if (grade.equals("Excellent")) {
						value = 7.0;
					} else if (grade.equals("Good")) {
						value = 5.0;
					} else if (grade.equals("Fair")) {
						value = 1.0;
					}

					if (relevance_judgments.containsKey(query) == false) {
						HashMap<Integer, Double> qr = new HashMap<Integer, Double>();
						relevance_judgments.put(query, qr);

						HashMap<Integer, Double> gain = new HashMap<Integer, Double>();
						document_gain.put(query, gain);
					}
					HashMap<Integer, Double> qr = relevance_judgments.get(query);
					HashMap<Integer, Double> gain = document_gain.get(query);
					qr.put(did, rel);
					gain.put(did, value);
				}
			} finally {
				reader.close();
			}
		} catch (IOException ioe) {
			System.err.println("Oops " + ioe.getMessage());
		}
	}

	public static void evaluateStdin(HashMap<String, HashMap<Integer, Double>> relevance_judgments, HashMap<String, HashMap<Integer, Double>> document_gain) throws NumberFormatException, IOException {
		// only consider one query per call
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			String line = null;
			double RR = 0.0; // this represent all relevant file.
			int N = 0;
			boolean flag = true;
			double DCG = 0.0;
			query = null;
			List<Double> standardDCGs = null;
			while ((line = reader.readLine()) != null) {
				Scanner s = new Scanner(line).useDelimiter("\t");
				query = s.next();
				int did = Integer.parseInt(s.next());
				String title = s.next();
				double rel = Double.parseDouble(s.next());
				N++;
				if (relevance_judgments.containsKey(query) == false) {
					throw new IOException("query not found");
				}
				HashMap<Integer, Double> qr = relevance_judgments.get(query);
				if (qr.containsKey(did) != false) {
					RR += qr.get(did);
					if (RR == 1.0 && flag) {
						reciprocal = 1.0 / N;
						flag = false;
					}
				}

				storeEachPoint(RR);
				if (N == 1) {
					standardDCGs = computeDCGS(document_gain, query);
				}
				DCG = computeNDCG(document_gain, query, did, N, DCG, standardDCGs);
			}

			// for last query
			averagePrecision = computePrecisionAndRecall(RR);
			computeFAtK();
		}
		catch (Exception e){
			System.err.println("Error:" + e.getClass());
		}
	}

	private static double computePrecisionAndRecall(double RR) {
		if(RR == 0){
			recalls[0] = 0;
			recalls[1] = 0;
			recalls[2] = 0;
			averagePrecision = 0;
			computtePrecisionRecallGraph(0, 0);
			return averagePrecision;
		}
		double averagePrecision = 0.0;
		for (int i = 0; i < points.size(); i++) {
			if (i == 0) {
				precisions[0] = points.get(i) / 1;	
				recalls[0] = points.get(i) / RR;
			} else if (i == 4) {
				precisions[1] = points.get(i) / 5;
				recalls[1] = points.get(i) / RR;
			} else if (i == 9) {
				precisions[2] = points.get(i) / 10;
				recalls[2] = points.get(i) / RR;
			}
			if (i == 0 && points.get(i) == 1) {
				averagePrecision = 1.0;
			} else if (i > 0 && points.get(i) > points.get(i - 1)) {
				averagePrecision += points.get(i) / (i + 1);
			}
			
			computtePrecisionRecallGraph(points.get(i) / RR, points.get(i) / (i + 1));
			
		}
		averagePrecision /= RR;
		
		return averagePrecision;
	}

	private static void computtePrecisionRecallGraph(double recall, double precision) {
		for (double value = 0.0; value <= 1.0; value = value + 0.1) {
			if (Math.abs(recall - value) <= 0.000001) {
				int a = (int) Math.ceil(value / 0.1);
				precisionRecallPoint[a] = precision;
				break;
			}
		}
	}

	private static void computeFAtK() {
		double a = 0.5;
		for (int i = 0; i < 3; i++) {
			fMeasure[i] = 1 / (a / precisions[i] + (1 - a) / recalls[i]);
		}
	}

	private static void storeEachPoint(double RR) {
		points.add(RR);
	}

	public static double computeNDCG(HashMap<String, HashMap<Integer, Double>> document_gain, String query, int did, int n, double DCG, List<Double> standardDCGs) {
		if (!document_gain.containsKey(query))
			return 0;
		HashMap<Integer, Double> gain = document_gain.get(query);
		if (gain.containsKey(did)) {
			DCG += gain.get(did) / Math.log(n + 1);
		}

		double standardDCG = 0;
		if (n == 1 || n == 5 || n == 10) {
			if (0 == standardDCGs.size())
				standardDCG = 0;
			else if (n > standardDCGs.size())
				standardDCG = standardDCGs.get(standardDCGs.size() - 1);
			else
				standardDCG = standardDCGs.get(n - 1);

			if (n == 1) {
				NDCG[0] = standardDCG == 0 ? 0 : DCG / standardDCG;
			} else if (n == 5) {
				NDCG[1] = standardDCG == 0 ? 0 : DCG / standardDCG;
			} else if (n == 10) {
				NDCG[2] = standardDCG == 0 ? 0 :DCG / standardDCG;
			}
		}

		return DCG;
	}

	private static List<Double> computeDCGS(HashMap<String, HashMap<Integer, Double>> document_gain, String query) {
		List<Double> standardDCGs = new ArrayList<Double>();
		double standardDCG = 0.0;
		int number = 1;
		int perfect = 0;
		int good = 0;
		int excellent = 0;
		int fair = 0;
		HashMap<Integer, Double> gain = document_gain.get(query);
		for (Double value : gain.values()) {
			if (value == 10.0) {
				perfect++;

			} else if (value == 7.0) {
				excellent++;
			} else if (value == 5.0) {
				good++;
			} else if (value == 1.0) {
				fair++;
			}
		}

		while (perfect > 0) {
			standardDCG += 10.0 / Math.log(number + 1);
			standardDCGs.add(standardDCG);
			number++;
			perfect--;
		}
		while (excellent > 0) {
			standardDCG += 7.0 / Math.log(number + 1);
			standardDCGs.add(standardDCG);
			number++;
			excellent--;
		}
		while (good > 0) {
			standardDCG += 5.0 / Math.log(number + 1);
			standardDCGs.add(standardDCG);
			number++;
			good--;
		}
		while (fair > 0) {
			standardDCG += 1.0 / Math.log(number + 1);
			standardDCGs.add(standardDCG);
			number++;
			fair--;
		}
		return standardDCGs;
	}
}
