package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.HashMap;
import java.util.Scanner;

class Evaluator {
	public final static double ONE = 1.0;
	private static double[] precisions = new double[3];
	private static double[] recalls = new double[3];
	private static double[] fMeasure = new double[3];
	private static double[] precisionRecallPoint = new double[11];
	private static double reciprocal = 0.0;
	private static List<Double> points = new ArrayList<Double>();

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
	}

	public static void readRelevanceJudgments(String p,
			HashMap<String, HashMap<Integer, Double>> relevance_judgments,
			HashMap<String, HashMap<Integer, Double>> document_gain) {
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
					if ((grade.equals("Perfect"))
							|| (grade.equals("Excellent"))
							|| (grade.equals("Good"))) {
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
					HashMap<Integer, Double> qr = relevance_judgments
							.get(query);
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

	public static void evaluateStdin(
			HashMap<String, HashMap<Integer, Double>> relevance_judgments,
			HashMap<String, HashMap<Integer, Double>> document_gain) {
		// only consider one query per call
		try {
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					System.in));
			String line = null;
			double RR = 0.0; // this represent all relevant file.
			int N = 0;
			boolean flag = true;
			double DCG = 0.0;
			String query = null;
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
				DCG = computeDCG(document_gain, query, did, N, DCG);
			}

			// for last query
			double averagePrecision = computePrecisionAndRecall(RR);
			computeFAtK();
			double NDCG = computeNDCG(document_gain, query, DCG);
			System.out.print(Arrays.toString(precisionRecallPoint));
			System.out.print(Arrays.toString(precisions));
			System.out.print(Arrays.toString(recalls));
			System.out.print(reciprocal);
			System.out.print(averagePrecision);
			System.out.print(Arrays.toString(fMeasure));
			System.out.print(NDCG);
			// System.out.println(Double.toString(RR/N));
		} catch (Exception e) {
			System.err.println("Error:" + e.getMessage());
		}
	}

	private static double computePrecisionAndRecall(double RR) {
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
			computtePrecisionRecallGraph(points.get(i) / RR, points.get(i)
					/ (i + 1));
		}
		averagePrecision /= RR;
		return averagePrecision;
	}

	private static void computtePrecisionRecallGraph(double recall,
			double precision) {
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

	public static double computeDCG(
			HashMap<String, HashMap<Integer, Double>> document_gain,
			String query, int did, int n, double DCG) {
		if (!document_gain.containsKey(query))
			return 0;
		HashMap<Integer, Double> gain = document_gain.get(query);
		if (gain.containsKey(did)) {
			DCG += gain.get(did) / Math.log(n + 1);
		}
		return DCG;
	}

	private static double computeNDCG(
			HashMap<String, HashMap<Integer, Double>> document_gain,
			String query, double DCG) {
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
			number++;
			perfect--;
		}
		while (excellent > 0) {
			standardDCG += 7.0 / Math.log(number + 1);
			number++;
			excellent--;
		}
		while (good > 0) {
			standardDCG += 5.0 / Math.log(number + 1);
			number++;
			good--;
		}
		while (fair > 0) {
			standardDCG += 1.0 / Math.log(number + 1);
			number++;
			fair--;
		}
		return DCG / standardDCG;
	}

}
