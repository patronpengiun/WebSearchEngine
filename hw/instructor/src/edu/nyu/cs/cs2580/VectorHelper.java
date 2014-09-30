package edu.nyu.cs.cs2580;

import java.util.HashMap;

public class VectorHelper {
	public static double innerProduct(HashMap<Integer,Double> a, HashMap<Integer,Double> b) {
		double result = 0;
		for (int idx: a.keySet()) {
			if (b.containsKey(idx)) {
				result += a.get(idx) * b.get(idx);
			}
		}
		return result;
	}
	
	public static double lengthOf(HashMap<Integer,Double> vector) {
		double sum = 0;
		for (double e: vector.values()) {
			sum += Math.pow(e, 2);
		}
		return Math.sqrt(sum);
	}
	
	public static void normalize(HashMap<Integer,Double> vector) {
		double length = lengthOf(vector);
		  
		if (length != 0) {
			for (int key: vector.keySet()) {
				vector.put(key, vector.get(key) / length);
			}
		}
	}
	
	public static double cosineOf(HashMap<Integer,Double> a, HashMap<Integer,Double> b) {
		return innerProduct(a,b) / lengthOf(a) / lengthOf(b);
	}
}
