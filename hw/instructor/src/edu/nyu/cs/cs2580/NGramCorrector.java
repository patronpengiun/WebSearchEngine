package edu.nyu.cs.cs2580;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;

public class NGramCorrector {
	private int N;
	private List[] nGrams;
	
	public NGramCorrector(int n) {
		this.N = n;
	}
	
	public String correct(String word, HashMap<String,Integer> map) {
		int minDistance = Integer.MAX_VALUE;
		int maxWeight = Integer.MIN_VALUE;
		String result = word;
		DamerauLevensteinMetric m = new DamerauLevensteinMetric();
		for (int i=0;i+N<=word.length();i++) {
			List list = getList(word.substring(i,i+N));
			if (list == null)
				continue;
			for (Object o: list) {
				String str = (String)o;
				int distance = m.getDistance(word, str);
				if (distance < minDistance) {
					minDistance = distance;
					result = str;
				} else if (distance == minDistance && map.get(str) > maxWeight) {
					maxWeight = map.get(str);
					result = str;
				}
			}
		}
		return result;
	}
	
	private List getList(String ngram) {
		int index = 0;
		for (int i=0;i<N;i++) {
			index = index * 26 + ngram.charAt(i) - 'a';
		}
		return nGrams[index];
	}
	
	public void construct(Set<String> set) {
		int size = 1;
		for (int i=0;i<N;i++) {
			size *= 26;
		}
		nGrams = new List[size];
		
		for (String str: set) {
			str = str.toLowerCase();
			if (str.length() < N || !isValid(str))
				continue;
			
			for (int i=0;i+N<=str.length();i++) {
				int index = 0;
				for (int j=0;j<N;j++) {
					index = index * 26 + str.charAt(i+j) - 'a';
				}
				if (nGrams[index] == null)
					nGrams[index] = new LinkedList<String>();
				nGrams[index].add(str);
			}
		}
		
		try {
			ObjectOutputStream writer =
					new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("data/index/ngram.idx")));
			writer.writeObject(nGrams);
			writer.close();
		}
		catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}
	
	public void load() {
		try {
			ObjectInputStream reader =
					new ObjectInputStream(new BufferedInputStream(new FileInputStream("data/index/ngram.idx")));
			Object ret = null;
			ret = reader.readObject();
			nGrams = (List[])ret;
			reader.close();
		}
		catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}
	
	static public boolean isValid(String str) {
		for (int i=0;i<str.length();i++) {
			if (str.charAt(i)-'a' < 0 || str.charAt(i)-'z' > 0)
				return false;
		}
		return true;
	}
}





