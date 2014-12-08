package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;

public class FinalProject {
	private TreeMap<String,Integer> dict;
	private HashMap<String,Integer> freqDict;
	private PrefixTree tree;
	private PrefixTree history;
	private DamerauLevensteinMetric dl = new DamerauLevensteinMetric();
	private NGramCorrector nc;
	
	private HashMap<String,Integer> biMap;
	
	public void buildTree(String corpusPath, String indexPath) {
		dict = new TreeMap<String,Integer>();
		File corpusDir = new File(corpusPath);
		File[] docs = corpusDir.listFiles();
		for (File doc: docs) {
			if (CorpusAnalyzer.isValidDocument(doc)) {
				buildFromDoc(doc);
			}
		}
		
		freqDict = new HashMap<String,Integer>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader("englishwords.txt"));
			Pattern p = Pattern.compile("\\w+");
			for(String temp = ""; temp != null; temp = reader.readLine()){
				Matcher m = p.matcher(temp.toLowerCase());
				while(m.find()) 
					freqDict.put((temp = m.group()),1);
			}
			reader.close();
		}
		catch (IOException e) {
			System.err.println(e.getMessage());
		}
		
		for (Map.Entry<String, Integer> e: dict.entrySet()) {
			if (freqDict.containsKey(e.getKey())) {
				freqDict.put(e.getKey(), e.getValue());
			} else if (e.getValue() > 100 && NGramCorrector.isValid(e.getKey())) {
				freqDict.put(e.getKey(), e.getValue());
			}
		}
		
		System.out.println("freqDict size: " + freqDict.size());
		
		// store freqDict
		try {
			ObjectOutputStream writer =
					new ObjectOutputStream(new BufferedOutputStream(
							new FileOutputStream(indexPath + "/freqDict.idx")));
			writer.writeObject(freqDict);
			writer.close();
		}
		catch (IOException e) {
			System.err.println(e.getMessage());
		}
		
		// build and store nGrams
		NGramCorrector nc = new NGramCorrector(3);
		nc.construct(freqDict.keySet());
		
		freqDict.clear();
		
		tree = new PrefixTree();
		for (Map.Entry<String, Integer> e: dict.entrySet()) {
			if (e.getValue() >= 10)
				tree.add(e.getKey(), e.getValue());
		}
		
		dict.clear();
		
		HashMap<String,Integer> biMap = null;
		try {
			ObjectInputStream reader =
					new ObjectInputStream(new BufferedInputStream(new FileInputStream("data/index/bigramMap.idx")));
			Object ret = null;
			ret = reader.readObject();
			biMap = (HashMap<String,Integer>)ret;
			reader.close();
		}
		catch (Exception e) {
			System.err.println(e.getMessage());
		}
		for (Map.Entry<String,Integer> e : biMap.entrySet()) {
			tree.add(e.getKey(), e.getValue());
		}
		
		try {
			ObjectOutputStream writer =
					new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(indexPath + "/tree.idx")));
			writer.writeObject(tree);
			writer.close();
		}
		catch (IOException e) {
			System.err.println(e.getMessage());
		}
		
		tree = null;
	}
	
	
	
	/*
	private void buildDict(String path, String indexPath) {
		dict = new TreeMap<String,Integer>();
		File corpusDir = new File(path);
		File[] docs = corpusDir.listFiles();
		for (File doc: docs) {
			if (CorpusAnalyzer.isValidDocument(doc)) {
				buildFromDoc(doc);
			}
		}
		try {
			ObjectOutputStream writer =
					new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(indexPath + "/tempDict.idx")));
			writer.writeObject(dict);
			writer.close();
		}
		catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}*/
	
	/*
	private void loadDict(String indexPath) {
		try {
			ObjectInputStream reader =
					new ObjectInputStream(new BufferedInputStream(new FileInputStream(indexPath + "/tempDict.idx")));
			Object ret = null;
			ret = reader.readObject();
			dict = (TreeMap<String,Integer>)ret;
			reader.close();
		}
		catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}*/
	
	/*
	public void buildTreeFromMap(String indexPath) {
		loadDict(indexPath);
		
		PrefixTree t = new PrefixTree();
		for (Map.Entry<String, Integer> e: dict.entrySet()) {
			if (e.getValue() >= 10)
				t.add(e.getKey(), e.getValue());
		}
		
		dict.clear();
		tree = t;
		System.gc();
		
		
		
		try {
			ObjectOutputStream writer =
					new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("tree")));
			writer.writeObject(t);
			writer.close();
		}
		catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}
	*/
	
	public void loadTree(String indexPath) {
		// load freqDict
		try {
			ObjectInputStream reader =
					new ObjectInputStream(new BufferedInputStream(new FileInputStream(indexPath + "/freqDict.idx")));
			Object ret = null;
			ret = reader.readObject();
			freqDict = (HashMap<String,Integer>)ret;
			reader.close();
		}
		catch (Exception e) {
			System.err.println(e.getMessage());
		}
		
		// load prefix tree
		try {
			ObjectInputStream reader =
					new ObjectInputStream(new BufferedInputStream(new FileInputStream(indexPath + "/tree.idx")));
			Object ret = null;
			ret = reader.readObject();
			tree = (PrefixTree)ret;
			reader.close();
		}
		catch (Exception e) {
			System.err.println(e.getMessage());
		}
		
		// load ngram mapping
		nc = new NGramCorrector(3);
		nc.load();
		
		//initialize prefix tree for query history
		history = new PrefixTree();
	}
	
	public void recordQuery(String query) {
		history.add(query, (int)(System.currentTimeMillis() / 1000));
		tree.update(query);
	}
	
	public String correct(String word) {
		if (freqDict.containsKey(word))
			return word;
		
		if (word.length() <= 5) {
			String result = word;
			int minDistance = Integer.MAX_VALUE;
			int maxWeight = Integer.MIN_VALUE;
			for (String str: freqDict.keySet()) {
				int distance = dl.getDistance(word, str);
				if (distance < minDistance) {
					minDistance = distance;
					result = str;
				} else if (distance == minDistance && freqDict.get(str) > maxWeight) {
					maxWeight = freqDict.get(str);
					result = str;
				}
			}
			return result;
		}
		
		return nc.correct(word, freqDict);
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
	
	public List<String> getLookup(String prefix) {
		List<String> result = tree.searchPrefix(prefix, 5);
		return result;
	}
	
	public List<String> getHistory(String prefix) {
		List<String> result = history.searchPrefix(prefix, 2);
		return result;
	}
	/*
	public static void main(String[] args) throws IOException{
		FinalProject p = new FinalProject();
		
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String line = null;
		while ((line = reader.readLine()) != null) {
			List<String> result = p.tree.searchPrefix(line, 5);
			for (String s: result) {
				System.out.println(s);
			}
		}
		reader.close();
		
		
	}
	*/
}
