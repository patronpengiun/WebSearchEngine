package edu.nyu.cs.cs2580;


import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.math.*;

/*public class SpellCorrection {
	public static void main(String[] args) throws IOException {
		SpellCorrector spell = new SpellCorrector();
		ArrayList<String> see = spell.correct("akk");
		System.out.println(see.toString());
	}
}*/



public class SpellCorrection{
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
	
	public ArrayList<String> correct(String word){
		ArrayList<String> result = new ArrayList<String>();
		if (dict.containsKey(word)) {
			result.add(word);
			return result;
		}
		ArrayList<String> list = edits(word);
		HashMap<Integer, ArrayList<String>> candidates = new HashMap<Integer, ArrayList<String>>();
		for(String s : list) {
			if(dict.containsKey(s)) {
				ArrayList<String> temp = new ArrayList<String>();
				if (candidates.get(dict.get(s)) != null){
					temp  = candidates.get(dict.get(s));
				}			
				temp.add(s);
				candidates.put(dict.get(s),temp);
			}
		}
		
		//if we cannot find words with edit distance of 1		
		
		if(candidates.isEmpty()){
			ArrayList<String> biglist = new ArrayList<String>();
			for(String s : list) {
				ArrayList<String> temp = edits(s);
				for (String str : temp){
					if (!biglist.contains(str))
						biglist.add(str);
				}
			}
			list = biglist;

			for(String s : biglist) {
				if(dict.containsKey(s)) {
					ArrayList<String> temp = new ArrayList<String>();
					if (candidates.get(dict.get(s)) != null){
						temp  = candidates.get(dict.get(s));
					}			
					temp.add(s);
					candidates.put(dict.get(s),temp);
				}
			}
		}	
		result = candidates.get(Collections.max(candidates.keySet()));
		return result;
	}
	
}
