package edu.nyu.cs.cs2580;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;

import org.jsoup.Jsoup;

public class BigramTest {
	public static void main(String[] args) throws IOException { 
		BigramTest bt = new BigramTest();
		bt.build();
	}
	
	private HashMap<String,Integer> biMap;
	private HashSet<String> stopWords;
	
	public void build() {
		String[] rawStopWords = new String[] {
				"i", "a", "about", "an", "are", "as", "at", "be", "by",  
				"for", "from", "how", "in", "is", "it", "of", "on", "or", 
				"that", "the", "this", "to", "was", "what", "when", "where",
				"who", "will", "with", "and", "[edit]", "^"
			};
		stopWords = new HashSet<String>(Arrays.asList(rawStopWords));
		
		File corpusDir = new File("data/wiki");
		File[] docs = corpusDir.listFiles();
		
		biMap = new HashMap<String,Integer>();
		int ptr = 0;
		for (File doc: docs) {
			if (CorpusAnalyzer.isValidDocument(doc)) {
				extractBigram(doc);
			}
			System.out.print(++ptr);
			System.out.println(" / " + docs.length);
		}
		
		HashMap<String,Integer> map = new HashMap<String,Integer>();
		for (Map.Entry<String, Integer> e: biMap.entrySet()) {
			if (e.getValue() >= 10) {
				map.put(e.getKey(), e.getValue());
			}
		}
		System.out.println(map.size());
		
		try {
			ObjectOutputStream writer =
					new ObjectOutputStream(new BufferedOutputStream(
							new FileOutputStream("data/index/bigramMap.idx")));
			writer.writeObject(map);
			writer.close();
		}
		catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}
	
	private void extractBigram(File doc) {
		String title = null;
		String text = null;
		try {
			org.jsoup.nodes.Document document = Jsoup.parse(doc, "UTF-8");
			title = document.title();
			text = document.body().text();
			document = null;		    	  
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
		
		Scanner sc = new Scanner(title).useDelimiter("\\s");
		String prev = null;
		while (sc.hasNext()) {
			String token = sc.next().toLowerCase();
			
			char last = token.charAt(token.length()-1);
			if (last == '.' || last == '"' || last == ',' || last == ':' || last == ';')
				token = token.substring(0,token.length()-1);
			
			if (prev == null) {
				prev = token;
				continue;
			}
			
			String bigram = prev + " " + token;
			
			if (!stopWords.contains(token) && !stopWords.contains(prev)) {
				if (biMap.containsKey(bigram))
					biMap.put(bigram, biMap.get(bigram)+10);
				else 
					biMap.put(bigram, 10);
			}
			
			prev = token;
		}
		sc.close();
		
		prev = null;
		Scanner scc = new Scanner(text).useDelimiter("\\s");
		while (scc.hasNext()) {
			String token = scc.next().toLowerCase();
			
			if (token.length() == 0)
				continue;
			
			char last = token.charAt(token.length()-1);
			if (last == '.' || last == '"' || last == ',' || last == ':' || last == ';')
				token = token.substring(0,token.length()-1);
			
			if (prev == null) {
				prev = token;
				continue;
			}
			
			String bigram = prev + " " + token;
			
			if (!stopWords.contains(token) && !stopWords.contains(prev)) {
				if (biMap.containsKey(bigram))
					biMap.put(bigram, biMap.get(bigram)+1);
				else 
					biMap.put(bigram, 1);
			}
			
			prev = token;
		}
		scc.close();
	}
}
