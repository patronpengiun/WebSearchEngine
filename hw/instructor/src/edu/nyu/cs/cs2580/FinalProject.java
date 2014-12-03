package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.*;

import org.jsoup.Jsoup;

public class FinalProject {
	private TreeMap<String,Integer> dict;
	
	public void buildDict() {
		dict = new TreeMap<String,Integer>();
		File corpusDir = new File("data/wiki");
		File[] docs = corpusDir.listFiles();
		for (File doc: docs) {
			if (CorpusAnalyzer.isValidDocument(doc)) {
				buildFromDoc(doc);
			}
		}
		try {
			ObjectOutputStream writer =
					new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("temp")));
			writer.writeObject(dict);
			writer.close();
		}
		catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}
	
	public void loadDict() {
		try {
			ObjectInputStream reader =
					new ObjectInputStream(new BufferedInputStream(new FileInputStream("temp")));
			Object ret = null;
			ret = reader.readObject();
			dict = (TreeMap<String,Integer>)ret;
			reader.close();
		}
		catch (Exception e) {}
		
		
		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("mapResult"),"UTF-8"));
			for (Map.Entry<String, Integer> e: dict.entrySet()) {
				if (e.getValue() < 100)
					continue;
				
				writer.write(e.getKey());
				writer.write("\t");
				writer.write(Integer.toString(e.getValue()));
				writer.newLine();
			}
			writer.close();
		}
		catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}
	
	private void buildFromDoc(File doc) {
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
		while (sc.hasNext()) {
			String token = sc.next().toLowerCase();
			if (token.length() == 0 || token.charAt(0)-'a'<0 || token.charAt(0)-'z'>0)
				continue;
			char last = token.charAt(token.length()-1);
			if (last == '.' || last == '"' || last == ',' || last == ':' || last == ';')
				token = token.substring(0,token.length()-1);
			if (dict.containsKey(token))
				dict.put(token, dict.get(token)+10);
			else 
				dict.put(token, 10);
		}
		sc.close();
		
		Scanner scc = new Scanner(text).useDelimiter("\\s");
		while (scc.hasNext()) {
			String token = scc.next().toLowerCase();
			if (token.length() == 0 || token.charAt(0)-'a'<0 || token.charAt(0)-'z'>0)
				continue;
			char last = token.charAt(token.length()-1);
			if (last == '.' || last == '"' || last == ',' || last == ':' || last == ';')
				token = token.substring(0,token.length()-1);
			if (dict.containsKey(token))
				dict.put(token, dict.get(token)+1);
			else 
				dict.put(token, 1);
		}
		scc.close();
	}
	
	public static void main(String[] args) {
		FinalProject p = new FinalProject();
		//p.buildDict();
		p.loadDict();
	}
}
