package edu.nyu.cs.cs2580;

import java.io.*;
import java.util.*;

import org.jsoup.Jsoup;


public class NGramExtractor {
	private TreeMap<String,Integer> map;
	private HashMap<String,Integer> finalMap;
	private HashSet<String> stopWords;
	
	public static void main(String[] args) throws IOException { 
		NGramExtractor extractor = new NGramExtractor();
		extractor.build();
	}
	
	public void build() throws IOException {
		String[] rawStopWords = new String[] {
				"i", "a", "about", "an", "are", "as", "at", "be", "by",  
				"for", "from", "how", "in", "is", "it", "of", "on", "or", 
				"that", "the", "this", "to", "was", "what", "when", "where",
				"who", "will", "with", "and", "[edit]", "^"
			};
		stopWords = new HashSet<String>(Arrays.asList(rawStopWords));
		map = new TreeMap<String,Integer>();
		
		File corpusDir = new File("data/wiki");
		File[] docs = corpusDir.listFiles();
		
		int ptr = 0;
		int temp = 0;
		int num_pieces = 0;
		for (File doc: docs) {
			if (CorpusAnalyzer.isValidDocument(doc)) {
				extractNgram(doc);
			}
			System.out.print(++ptr);
			System.out.println(" / " + docs.length);
			
			if (++temp > docs.length / 10) {
				temp = 0;
				num_pieces++;
				dump(num_pieces);
				map.clear();
				System.gc();
			}
		}
		dump(++num_pieces);
		map.clear();
		System.gc();
		
		finalMap = new HashMap<String,Integer>();
		merge(num_pieces);
		
		System.out.println("size: " + finalMap.size());
		
		try {
			ObjectOutputStream writer =
					new ObjectOutputStream(new BufferedOutputStream(
							new FileOutputStream("data/index/ngramMap.idx")));
			writer.writeObject(finalMap);
			writer.close();
		}
		catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}
	
	private void dump(int num) throws IOException {
		String indexFile = "data/index/temp" + Integer.toString(num);
		System.out.println("Store ngram map piece " + Integer.toString(num) + " to: " + indexFile);
		BufferedWriter writer = new BufferedWriter(new FileWriter(indexFile));
		String[] keys = map.keySet().toArray(new String[map.keySet().size()]);
		Arrays.sort(keys);
		for (String key: keys) {
			writer.write(Integer.toString(map.get(key)) + " " + key);
			writer.newLine();
		}
		writer.close();
	}
	
	private void merge(int num) throws IOException {
		BufferedReader[] readers= new BufferedReader[num];
		for (int i=1;i<=num;i++) {
			String partialFile = "data/index/temp" + Integer.toString(i);
			BufferedReader reader = new BufferedReader(new FileReader(partialFile));
			readers[i-1] = reader;
		}
		
		boolean[] toMove = new boolean[num];
		Arrays.fill(toMove, true);
		String min_token = null;
		String[] tokens = new String[num];
		int[] weights = new int[num];
		long offset = 0;
		  
		while(true) {
			min_token = null;
			for (int i=0;i<num;i++) {
				if (toMove[i]) {
					String line = readers[i].readLine();
					if (line == null) {
						tokens[i] = null;
						weights[i] = 0;
					} else {
						tokens[i] = getToken(line);
						weights[i] = getWeight(line);
					}
					toMove[i] = false;
				}
				if (tokens[i] != null) {
					if (min_token == null || tokens[i].compareTo(min_token)<0)
						min_token = tokens[i];
				}
					
			}
			if (min_token == null)
				break;
			int sum = 0;
			for (int i=0;i<num;i++) {
				if (tokens[i] != null && tokens[i].equals(min_token)) {
					sum += weights[i];
					toMove[i] = true;
				}
			}
			if (sum >= 10)
				finalMap.put(min_token, sum);
		}
		for (BufferedReader r: readers)
			r.close();
		  
		// delete all the partial files
		for (int i = 1;i <= num;i++) {
			String partialFile = "data/index/temp" + Integer.toString(i);
			File file = new File(partialFile);
			file.delete();
		}
	}
	
	private String getToken(String line) {
		String weight = line.split(" ")[0];
		return line.substring(weight.length()+1);
	}
	
	private int getWeight(String line) {
		return Integer.parseInt(line.split(" ")[0]);
	}

	private void extractNgram(File doc) {
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
		
		extractWithWeight(10,title,5);
		extractWithWeight(1,text,5);
	}
	
	private void extractWithWeight(int weight, String text, int N) {
		Scanner sc = new Scanner(text).useDelimiter("\\s");
		LinkedList<String> ngram = new LinkedList<String>();
		while (sc.hasNext()) {
			String token = sc.next().toLowerCase();
			
			if (token.length() == 0)
				continue;
			
			char last = token.charAt(token.length()-1);
			if (last == '.' || last == '"' || last == ',' || last == ':' || last == ';' || 
					last == '“' || last == '”' || last == ')')
				token = token.substring(0,token.length()-1);
			
			if (token.length() == 0)
				continue;
			
			if (ngram.size() < N) {
				ngram.addLast(token);
				String temp = ngram.getFirst();
				boolean flag = true;
				for (String str: ngram) {
					if (flag) {
						flag = false;
						continue;
					}
					temp += " " + str;
				}
				if (map.containsKey(temp))
					map.put(temp, map.get(temp)+weight);
				else 
					map.put(temp, weight);
				
			} else {
				ngram.removeFirst();
				ngram.addLast(token);
				
				String temp = ngram.getFirst();
				boolean flag = true;
				for (String str: ngram) {
					if (flag) {
						flag = false;
						continue;
					}
					
					temp += " " + str;
					if (map.containsKey(temp))
						map.put(temp, map.get(temp)+weight);
					else 
						map.put(temp, weight);
				}
			}
		}
		sc.close();
	}
	
}

