package edu.nyu.cs.cs2580;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import edu.nyu.cs.cs2580.IndexerInvertedCompressed.Pair;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedDoconly extends Indexer implements Serializable{
	
  private static final long serialVersionUID = 165377250909L;
  
  // Stores all document in memory
  private Vector<edu.nyu.cs.cs2580.Document> _documents = new Vector<edu.nyu.cs.cs2580.Document>();
	  
  // Maps each term to their integer representation
  private Map<String, Integer> _dictionary = new HashMap<String, Integer>();
  
  // All the information about a token
  private HashMap<Integer,TokenInfo> tmap = new HashMap<Integer,TokenInfo>();
  
  //PageRank and NumViews for each document
  private HashMap<String,Pair> scoreMap = new HashMap<String,Pair>();
  
  public IndexerInvertedDoconly(Options options) {
    super(options);
    System.out.println("Using Indexer: " + this.getClass().getSimpleName());
  }

  @Override
  public void constructIndex() throws IOException {
	  // load pageRank and numViews
	  CorpusAnalyzer analyzer = CorpusAnalyzer.Factory.getCorpusAnalyzerByOption(SearchEngine.OPTIONS);
	  HashMap<String,Double> pageRankMap = (HashMap<String,Double>)analyzer.load();
	  LogMiner miner = LogMiner.Factory.getLogMinerByOption(SearchEngine.OPTIONS);
	  HashMap<String,Integer> numViewsMap = (HashMap<String,Integer>)miner.load();
	  for (Map.Entry<String, Double> e: pageRankMap.entrySet()) {
		  scoreMap.put(e.getKey(), new Pair(e.getValue(),numViewsMap.get(e.getKey())));
	  }
	  pageRankMap.clear();
	  numViewsMap.clear();
	  
	  int num_pieces = 0;
	  
	  Stemmer stemmer = new Stemmer();
	  File corpusDir = new File(_options._corpusPrefix);
	  File[] docs = corpusDir.listFiles();
	  int numOfDoc = docs.length;
	  int count=0,temp=0;
	  for (File file: docs) {
		  if (!CorpusAnalyzer.isValidDocument(file))
			  continue;
		  
		  count++;
		  temp++;
		  System.out.println(docs.length + " / " + count);
		  if (file.isFile()) {
			  constructSingleDocument(file.getAbsolutePath(),stemmer);
		  }
		  if (temp > numOfDoc / 20) {
			  temp = 0;
			  num_pieces++;
			  dump(num_pieces);
		  }
	  }
	  num_pieces++;
	  dump(num_pieces);
	  
	  System.out.println("merging pieces");
	  merge(num_pieces);
	  
	  System.out.println("store documents idx");
	  String documentIndexFile = _options._indexPrefix + "/invertedOnlyDoc.idx";
	  ObjectOutputStream writer =
		  new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(documentIndexFile)));
	  writer.writeObject(_documents);
	  writer.close();
		  
	  System.out.println("store dictionary idx");
	  String termIndexFile = _options._indexPrefix + "/invertedOnlyTerm.idx";
	  writer = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(termIndexFile)));
	  writer.writeObject(_dictionary);
	  writer.close();
	  
	  String totalFreqFileName = _options._indexPrefix + "/invertedOnlyFreq.idx";
	  RandomAccessFile freqFile = new RandomAccessFile(totalFreqFileName, "rw");
	  freqFile.writeLong(_totalTermFrequency);
	  freqFile.close();
  }

  @Override
  public void loadIndex() throws IOException, ClassNotFoundException {
	  System.out.println("loading indexedDocVector");
	  String documentIndexFile = _options._indexPrefix + "/invertedOnlyDoc.idx";
	  ObjectInputStream reader =
		  new ObjectInputStream(new BufferedInputStream(new FileInputStream(documentIndexFile)));
	  this._documents = (Vector<edu.nyu.cs.cs2580.Document>)reader.readObject();
	  reader.close();
	  
	  System.out.println("loading dictionary");
	  String termIndexFile = _options._indexPrefix + "/invertedOnlyTerm.idx";
	  reader = new ObjectInputStream(new BufferedInputStream(new FileInputStream(termIndexFile)));
	  this._dictionary = (Map<String, Integer>)reader.readObject();
	  reader.close();
	  
	  this._numDocs = _documents.size();
	  String totalFreqFileName = _options._indexPrefix + "/invertedOnlyFreq.idx";
	  RandomAccessFile freqFile = new RandomAccessFile(totalFreqFileName, "r");
	  this._totalTermFrequency = freqFile.readLong();
	  System.out.println("total corpus freq: " + this._totalTermFrequency);
	  freqFile.close();
	  
	  this.cachedPtrArray = new int[_dictionary.size()];
	  Arrays.fill(cachedPtrArray,0);
  }

  @Override
  public edu.nyu.cs.cs2580.Document getDoc(int docid) {
    return _documents.get(docid);
  }

  /**
   * In HW2, you should be using {@link DocumentIndexed}
   */
  @Override
  public edu.nyu.cs.cs2580.Document nextDoc(Query query, int docid) {
	  Vector<String> tokens = query._tokens;
	  if (0 == tokens.size())
		  return null;
	  boolean flag = true;
	  int prev = 0;
	  int max = -1;
	  for (int i=0;i<tokens.size();i++) {
		  Integer pos = next(tokens.get(i),docid);
		  if (null == pos)
			  return null;
		  if (flag && i != 0 && pos != prev)
			  flag = false;
		  prev = pos;
		  max = Math.max(max,pos);
	  }
	  if (flag)
		  return _documents.get(prev);
	  else
		  return nextDoc(query,max-1);
  }
  
  private int[] cachedPtrArray;
  
  private Integer next(String token, int docid) {
	  Integer idx = _dictionary.get(token);
	  if (null == idx)
		  return null;
	  
	  int cachedPtr = cachedPtrArray[idx];
	  ArrayList<IntPair> pl;
	  if (!tmap.containsKey(idx)) {
		  try {
			  tmap.put(idx, fetchInfo(idx));
		  }
		  catch(Exception e) {}
	  }
	  pl = tmap.get(idx).postingList;
	  if (pl.get(pl.size()-1).first <= docid)
		  return null;
	  
	  if (pl.get(0).first > docid) {
		  cachedPtrArray[idx] = 0;
		  return pl.get(0).first;
	  }
	  
	  if (cachedPtr > 0 && pl.get(cachedPtr-1).first > docid)
		  cachedPtr = 0;
	  while (pl.get(cachedPtr).first <= docid)
		  cachedPtr++;
	  cachedPtrArray[idx] = cachedPtr;
	  return pl.get(cachedPtr).first;
  }

  @Override
  public int corpusDocFrequencyByTerm(String term) {
	Integer idx = _dictionary.get(term);
	  if (null == idx)
		  return 0;
	  else
		  return cachedInfo(idx).docFreq;
  }

  @Override
  public int corpusTermFrequency(String term) {
	  Integer idx = _dictionary.get(term);
	  if (null == idx)
		  return 0;
	  else
		  return cachedInfo(idx).corpusFreq;
  }

  @Override
  public int documentTermFrequency(String term, int docid) {
	  if (docid >= _numDocs)
		  return 0;
	  
	  Integer idx = _dictionary.get(term);
	  if (idx == null)
		  return 0;
	  
	  TokenInfo info = cachedInfo(idx);
	  ArrayList<IntPair> pl = info.postingList;
	  for (int i=0;i<pl.size();i++) {
		  if (pl.get(i).first == docid)
			  return pl.get(i).second;
		  if (pl.get(i).first > docid)
			  return 0;
	  }
	  return 0;
  }
  
  private TokenInfo cachedInfo(int idx) {
	  if (tmap.containsKey(idx))
		  return tmap.get(idx);
	  else {
		  TokenInfo info = null;
		  try {
			  info = fetchInfo(idx);
		  }
		  catch(Exception e) {}
		  tmap.put(idx, info);
		  return info;
	  }
  }
  
  private void constructSingleDocument(String path, Stemmer stemmer) {
	  int doc_id = _documents.size();
	  long total_words = 0;
	  DocumentIndexed doc = new DocumentIndexed(doc_id);
	  String url = CorpusAnalyzer.convertToUTF8(new File(path).getName());
      doc.setUrl(url);
      doc.setPageRank((float)scoreMap.get(url).pageRank);
      doc.setNumViews(scoreMap.get(url).numViews);
	  
	  try {
		  org.jsoup.nodes.Document document = Jsoup.parse(new File(path), null);
		  doc.setTitle(document.title());
		  String text = document.body().text();
		  document = null;
		  Scanner[] sr = new Scanner[2];
		  sr[0] = new Scanner(text).useDelimiter("\\s+");
		  sr[1] = new Scanner(doc.getTitle()).useDelimiter("\\s+");
		  HashSet<Integer> uniq_set = new HashSet<Integer>(); // keep track of all unique terms in this document
		  for (Scanner scanner: sr) {
			  while (scanner.hasNext()) {
				  String token = stem(scanner.next(),stemmer);
				  if (token.equals(""))
					  continue;
				  else {
					  _totalTermFrequency++;
					  total_words++;
					  int idx;
					  if (_dictionary.containsKey(token)) {
						  idx = _dictionary.get(token);
					  } else {
						  idx = _dictionary.size();
						  _dictionary.put(token, idx);
					  }
					  uniq_set.add(idx);
					  if (tmap.get(idx) == null)
						  tmap.put(idx, new TokenInfo(doc_id));
					  tmap.get(idx).corpusFreq += 1;
					  ArrayList<IntPair> plist = tmap.get(idx).postingList;
					  if (plist.get(plist.size()-1).first == doc_id)
						  plist.get(plist.size()-1).second += 1;
					  else
						  plist.add(new IntPair(doc_id,1));
				  }
			  }
			  scanner.close();
		  }
		  for (int tid: uniq_set) {
			  tmap.get(tid).docFreq += 1;
		  }
		  doc.set_totalWords(total_words);
		  _documents.add(doc);
	  }
	  catch (IOException e) {
		  System.err.println(e.getMessage());
	  }
  }
  
  private void dump(int n) throws IOException {
	  String indexFile = _options._indexPrefix + "/invertedDocOnly_" + Integer.toString(n) + ".idx";
	  System.out.println("Store index piece " + Integer.toString(n) + " to: " + indexFile);
	  BufferedWriter writer = new BufferedWriter(new FileWriter(indexFile));
	  Integer[] keys = tmap.keySet().toArray(new Integer[tmap.keySet().size()]);
	  Arrays.sort(keys);
	  for (int key: keys) {
		  TokenInfo info = tmap.get(key);
		  writer.write(key + " " + info.corpusFreq + " " + info.docFreq);
		  for (IntPair pair: info.postingList)
			  writer.write(" " + pair.first + "," + pair.second);
		  writer.newLine();
	  }
	  tmap.clear();
	  writer.close();
	  System.gc();
  }
  
  private void merge (int num) throws IOException{
	  BufferedReader[] readers= new BufferedReader[num];
	  for (int i=1;i<=num;i++) {
		  String partialFile = _options._indexPrefix + "/invertedDocOnly_" + Integer.toString(i) + ".idx";
		  BufferedReader reader = new BufferedReader(new FileReader(partialFile));
		  readers[i-1] = reader;
	  }
	  
	  String mergedFile = _options._indexPrefix + "/invertedDocOnlyMerged.idx";
	  BufferedWriter writer = new BufferedWriter(new FileWriter(mergedFile));
	  
	  String offsetFileName = _options._indexPrefix + "/invertedDocOnlyOffset.idx";
	  RandomAccessFile offsetFile = new RandomAccessFile(offsetFileName, "rw");
	  
	  boolean[] toMove = new boolean[num];
	  Arrays.fill(toMove, true);
	  int min_id = Integer.MAX_VALUE;
	  int[] ids = new int[num];
	  TokenInfo[] infos = new TokenInfo[num];
	  long offset = 0;
	  
	  while(true) {
		  min_id = Integer.MAX_VALUE;
		  for (int i=0;i<num;i++) {
			  if (toMove[i]) {
				  String line = readers[i].readLine();
				  if (line == null) {
					  ids[i] = Integer.MAX_VALUE;
					  infos[i] = null;
				  } else {
					  ids[i] = getId(line);
					  infos[i] = getInfo(line);
				  }
				  toMove[i] = false;
			  }
			  if (ids[i] < min_id)
				  min_id = ids[i];
		  }
		  if (min_id == Integer.MAX_VALUE)
			  break;
		  ArrayList<TokenInfo> toMerge = new ArrayList<TokenInfo>();
		  for (int i=0;i<num;i++) {
			  if (ids[i] == min_id) {
				  toMerge.add(infos[i]);
				  toMove[i] = true;
			  }
		  }
		  long size = writeToIndex(min_id,toMerge,writer);
		  offset += size;
		  offsetFile.writeLong(offset);
	  }
	  for (BufferedReader r: readers)
		  r.close();
	  writer.close();
	  offsetFile.close();
	  
	  // delete all the partial files
	  for (int i = 1;i <= num;i++) {
		  String partialFile = _options._indexPrefix + "/invertedDocOnly_" + Integer.toString(i) + ".idx";
		  File file = new File(partialFile);
		  file.delete();
	  }
  }
  
  private TokenInfo fetchInfo(int id) throws FileNotFoundException, IOException{
	  long offset;
	  String offsetFileName = _options._indexPrefix + "/invertedDocOnlyOffset.idx";
	  RandomAccessFile offsetFile = new RandomAccessFile(offsetFileName, "r");
	  if (id == 0)
		  offset = 0;
	  else {
		  offsetFile.seek(8L * (id - 1));
		  offset = offsetFile.readLong();
	  }
	  
	  String indexFileName = _options._indexPrefix + "/invertedDocOnlyMerged.idx";
	  RandomAccessFile indexFile = new RandomAccessFile(indexFileName,"r");
	  indexFile.seek(offset);
	  String line = indexFile.readLine();
	  offsetFile.close();
	  indexFile.close();
	  return getInfo(line);
  }
  
  private int getId(String line) {
	  Scanner s = new Scanner(line);
	  int ret = Integer.parseInt(s.next());
	  s.close();
	  return ret;
  }
  
  private TokenInfo getInfo(String line) {
	  TokenInfo info = new TokenInfo();
	  Scanner s = new Scanner(line);
	  s.next();
	  info.corpusFreq = Integer.parseInt(s.next());
	  info.docFreq = Integer.parseInt(s.next());
	  info.postingList = new ArrayList<IntPair>();
	  while (s.hasNext()) {
		  String[] e = s.next().split(",");
		  info.postingList.add(new IntPair(Integer.parseInt(e[0]),Integer.parseInt(e[1])));
	  }
	  s.close();
	  return info;
  }
  
  private long writeToIndex(int termId,ArrayList<TokenInfo> list,BufferedWriter writer) throws IOException{
	  long result = 0L;
	  TokenInfo info = new TokenInfo();
	  info.corpusFreq = 0;
	  info.docFreq = 0;
	  info.postingList = new ArrayList<IntPair>();
	  for (TokenInfo i: list) {
		  info.corpusFreq += i.corpusFreq;
		  info.docFreq += i.docFreq;
		  info.postingList.addAll(i.postingList);
	  }
	  String temp = termId + " " + info.corpusFreq + " " + info.docFreq;
	  writer.write(temp);
	  result += temp.length();
	  for (IntPair pair: info.postingList) {
		  temp = " " + pair.first + "," + pair.second;
		  result += temp.length();
		  writer.write(temp);
	  }
	  result++;
	  writer.newLine();
	  return result;
  }
  
  private String stem(String origin, Stemmer stemmer) {
	  String lower = origin.toLowerCase();
      stemmer.add(lower.toCharArray(), lower.length());
      stemmer.stem();
      return stemmer.toString();
  }
  
  class IntPair {
	  public int first;
	  public int second;
	  public IntPair(int a,int b) {
		  first = a;
		  second = b;
	  }
  }
  
  class TokenInfo {
	  public int corpusFreq;
	  public int docFreq;
	  public ArrayList<IntPair> postingList;
	  
	  public TokenInfo() {}
	  
	  public TokenInfo(int doc_id) {
		  corpusFreq = 0;
		  docFreq = 0;
		  postingList = new ArrayList<IntPair>();
		  postingList.add(new IntPair(doc_id,0));
	  }
  }
  
  //class to store pageRank and numViews
  class Pair {
	  double pageRank;
	  int numViews;
	  Pair(double p, int n) {
		  pageRank = p;
		  numViews = n;
	  } 
  }
}
