package edu.nyu.cs.cs2580;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.Map.Entry;

import org.jsoup.Jsoup;

//query form http://<HOST>:<PORT>/prf?query=<QUERY>&ranker=<RANKER-TYPE>&numdocs=<INTEGER>&numterms=<INTEGER>

public class PrfCalculator {
	private Vector<ScoredDocument> _docs;
	private String _corpusPrefix;
	private Indexer _indexer;
	private long _totalWordCount;
	private HashMap<String,Integer> _uniqMap;
	private int _numWords;
	private final HashSet<String> stopWords;
	private final Stemmer stemmer= new Stemmer();
	
	public PrfCalculator(Vector<ScoredDocument> docs, String corpusPrefix, Indexer indexer, int numWords) {
		_docs = docs;
		_corpusPrefix = corpusPrefix;
		_indexer = indexer;
		_numWords = numWords;
		
		String[] rawStopWords = new String[] {
			"i", "a", "about", "an", "are", "as", "at", "be", "by",  
			"for", "from", "how", "in", "is", "it", "of", "on", "or", 
			"that", "the", "this", "to", "was", "what", "when", "where",
			"who", "will", "with", "and"
		};
		for (int i=0;i<rawStopWords.length;i++) {
			stemmer.add(rawStopWords[i].toCharArray(), rawStopWords[i].length());
			stemmer.stem();
			rawStopWords[i] = stemmer.toString();
		}
		stopWords = new HashSet<String>(Arrays.asList(rawStopWords));
	}
	
	public List<ProbEntry> compute() {
		_totalWordCount = 0;
		_uniqMap = new HashMap<String,Integer>();
		for (ScoredDocument scoredDoc: _docs) {
			try {
				processSingleDocument(scoredDoc.get_doc());
			}
			catch (IOException e) {}
		}
		
		PriorityQueue<FreqEntry> q = new PriorityQueue<FreqEntry>();
		for (Map.Entry<String,Integer> e: _uniqMap.entrySet()) {
			if (stopWords.contains(e.getKey()))
				continue;
			q.add(new FreqEntry(e.getKey(),e.getValue()));
			if (q.size() > _numWords)
				q.poll();
		}
		
		LinkedList<ProbEntry> result = new LinkedList<ProbEntry>();
		while (!q.isEmpty()) {
			FreqEntry e = q.poll();
			result.addFirst(new ProbEntry(e.token, 1.0 * e.freq / _totalWordCount));
		}
		return result;
	}
	
	private void processSingleDocument(Document doc) throws IOException{
		_totalWordCount += ((DocumentIndexed)doc).get_totalWords();
		
		String offsetFileName = _indexer._options._indexPrefix + "/auxOffset.idx";
		RandomAccessFile offsetFile = new RandomAccessFile(offsetFileName,"r");
		String auxFileName = _indexer._options._indexPrefix + "/aux.idx";
		RandomAccessFile auxFile = new RandomAccessFile(auxFileName,"r");
		
		long offset;
		long next_offset;
		int id = doc._docid;
		if (id == 0)
			offset = 0;
		else {
			offsetFile.seek(8L * (id - 1));
			offset = offsetFile.readLong();
		}
		offsetFile.seek(8L * id);
		next_offset = offsetFile.readLong();
		
		int size = (int)(next_offset - offset);
		auxFile.seek(offset);
		byte[] temp = new byte[size];
		for (int i=0;i<size;i++) {
			temp[i] = auxFile.readByte();
		}
		String record = new String(temp,"UTF-8");
		Scanner s = new Scanner(record).useDelimiter("\t");
		while (s.hasNext()) {
			String token = s.next();
			int freq = Integer.parseInt(s.next());
			if (_uniqMap.containsKey(token)) 
				_uniqMap.put(token, _uniqMap.get(token)+freq);
			else
				_uniqMap.put(token,freq);
		}
		s.close();
		offsetFile.close();
		auxFile.close();
	}
	

	class FreqEntry implements Comparable<FreqEntry> {
		String token;
		int freq;
		public FreqEntry(String t, int f) {
			token = t;
			freq = f;
		}
		
		@Override 
		public int compareTo(FreqEntry e) {
			if (this.freq == e.freq) {
				return 0;
			}
			return (this.freq > e.freq) ? 1 : -1;
		}
	}
	
	static class ProbEntry {
		String token;
		double prob;
		public ProbEntry(String t, double p) {
			token = t;
			prob = p;
		}
	}
}
