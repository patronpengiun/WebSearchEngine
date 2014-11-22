package edu.nyu.cs.cs2580;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

import org.jsoup.Jsoup;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedCompressed extends Indexer implements Serializable {
	
      private static final long serialVersionUID = 1077111905740085031L;

	  // Maps each term to their integer representation
	  private Map<String, Integer> _dictionary = new HashMap<String, Integer>(); 

	  // Stores all Document in memory.
	  private Vector<DocumentIndexed> _documents = new Vector<DocumentIndexed>();
	  
	  // All the information about a token
	  private HashMap<Integer,TokenInfo> tmap = new HashMap<Integer,TokenInfo>();
	  
	  // PageRank and NumViews for each document
	  private HashMap<String,Pair> scoreMap = new HashMap<String,Pair>();
	  
	  // Provided for serialization
	  public IndexerInvertedCompressed() { }

	  public IndexerInvertedCompressed(Options options) {
	    super(options);
	    System.out.println("Using Indexer: " + this.getClass().getSimpleName());
	  }

	  @Override
	  public void constructIndex() throws IOException {
		  // load pageRank and numViews
		  CorpusAnalyzer analyzer = CorpusAnalyzer.Factory.getCorpusAnalyzerByOption(SearchEngine.OPTIONS);
		  HashMap<String,Float> pageRankMap = (HashMap<String,Float>)analyzer.load();
		  LogMiner miner = LogMiner.Factory.getLogMinerByOption(SearchEngine.OPTIONS);
		  HashMap<String,Integer> numViewsMap = (HashMap<String,Integer>)miner.load();
		  for (Map.Entry<String, Float> e: pageRankMap.entrySet()) {
			  scoreMap.put(e.getKey(), new Pair(e.getValue(),numViewsMap.get(e.getKey())));
		  }
		  pageRankMap.clear();
		  numViewsMap.clear();
		  
		  int num_pieces = 0;
		  String auxOffset = _options._indexPrefix + "/auxOffset.idx";
		  RandomAccessFile auxOffsetFile = new RandomAccessFile(auxOffset, "rw");
		  long offset = 0l;
		  
		  try {
	          String corpus = _options._corpusPrefix;
	          System.out.println("Construct index from: " + corpus);

	          File corpusFiles = new File(corpus);
	          File[] listFiles = corpusFiles.listFiles();
	          int numOfDoc = listFiles.length;
	          int count = 0, temp = 0;
	          
	          Stemmer stemmer = new Stemmer();
	          
	          for (File file : listFiles) {
	        	  if (!CorpusAnalyzer.isValidDocument(file))
	    			  continue;
	        	  
	        	  count++;
	        	  temp++;
	        	  System.out.println(listFiles.length + " / " + count);       	  
	              
	              if (file.isFile()) {
	            	  long size = processDocument(file, stemmer);
	            	  offset += size;
	            	  auxOffsetFile.writeLong(offset);
	    		  }
	    		  if (temp > numOfDoc / 40) {
	    			  temp = 0;
	    			  num_pieces++;
	    			  dump(num_pieces);
	    		  }
	          }		               
	      } catch (Exception e) {
	          e.printStackTrace();
	      }

		  auxOffsetFile.close();
		  
		  num_pieces++;
		  dump(num_pieces);
		  
		  System.out.println("merging pieces");
		  merge(num_pieces);
		  
		  System.out.println("store documents idx");
		  String documentIndexFile = _options._indexPrefix + "/invertedCompressDoc.idx";
		  ObjectOutputStream writer =
			  new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(documentIndexFile)));
		  writer.writeObject(_documents);
		  writer.close();
			  
		  System.out.println("store dictionary idx");
		  String termIndexFile = _options._indexPrefix + "/invertedCompressTerm.idx";
		  writer = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(termIndexFile)));
		  writer.writeObject(_dictionary);
		  writer.close();
		  
		  String totalFreqFileName = _options._indexPrefix + "/invertedCompressFreq.idx";
		  RandomAccessFile freqFile = new RandomAccessFile(totalFreqFileName, "rw");
		  freqFile.writeLong(_totalTermFrequency);
		  freqFile.close();
	  }

	  private long processDocument(File file, Stemmer stemmer) throws IOException{
		  long ret = 0l;
		  int doc_id = _documents.size();
		  long total_words = 0;
	      DocumentIndexed doc = new DocumentIndexed(doc_id);
	      String url = CorpusAnalyzer.convertToUTF8(file.getName());
	      doc.setUrl(url);
	      doc.setPageRank(scoreMap.get(url).pageRank);
	      doc.setNumViews(scoreMap.get(url).numViews);
	      
	      // aux map for PRF 
	      HashMap<String,Integer> auxMap = new HashMap<String,Integer>();
	      String auxFile = _options._indexPrefix + "/aux.idx";
	      BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(auxFile,true));
	      
	         
		  // the text of parsed document
		  String text = "";	
	      try {
	    	  org.jsoup.nodes.Document document = Jsoup.parse(file, "UTF-8");
	    	  doc.setTitle(document.title());
			  text = document.body().text();
			  document = null;		    	  
	      } catch (IOException e) {
			  System.err.println(e.getMessage());
		  }
		  
	      // the uniq term set for this document
		  Set<Integer> uniq_set = new HashSet<Integer>();
		  Scanner[] sr = new Scanner[2];
		  sr[0] = new Scanner(doc.getTitle()).useDelimiter("\\s+");
		  sr[1] = new Scanner(text).useDelimiter("\\s+");
		  int offset = 0;
		  for (Scanner scanner: sr) {
			  while(scanner.hasNext()) {
				  			  
				  // stem each term
				  String token = stem(scanner.next(),stemmer);
				  
				  if(token.equals("")) {
					  continue;
				  } else {
					  if (auxMap.containsKey(token)) 
						  auxMap.put(token, auxMap.get(token)+1);
					  else
						  auxMap.put(token, 1);
					  
					  
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
					  ArrayList<Posting> plist = tmap.get(idx).postingList;
					  if (plist.get(plist.size()-1).docid == doc_id)
						  plist.get(plist.size()-1).oc.add(offset);
					  else {
						  Posting posting = new Posting(doc_id);
						  posting.oc.add(offset);
						  plist.add(posting);
					  }
					  offset++;
				  }			  			  
			  }
			  scanner.close();
		  }
		  for (int tid: uniq_set) {
			  tmap.get(tid).docFreq += 1;
		  }
		  doc.set_totalWords(total_words);
		  _documents.add(doc);
		  
		  // write to aux.idx
		  try {
			  for (Map.Entry<String, Integer> e: auxMap.entrySet()) {
				  String temp = e.getKey() + "\t" + e.getValue() + "\t";
				  byte[] b = temp.getBytes("UTF-8");
				  ret += b.length;
				  writer.write(b);
			  }
		  }
		  catch (IOException e) {
			  
		  }
		  auxMap.clear();
		  writer.close();
		  return ret;
	  }
	  
	  private void dump(int n) throws IOException {
		  String indexFile = _options._indexPrefix + "/invertedCompress_" + Integer.toString(n) + ".idx";
		  System.out.println("Store index piece " + Integer.toString(n) + " to: " + indexFile);
		  BufferedWriter writer = new BufferedWriter(new FileWriter(indexFile));
		  Integer[] keys = tmap.keySet().toArray(new Integer[tmap.keySet().size()]);
		  Arrays.sort(keys);
		  for (int key: keys) {
			  TokenInfo info = tmap.get(key);
			  writer.write(key + " " + info.corpusFreq + " " + info.docFreq);
			  for (Posting posting: info.postingList)
				  writer.write(" " + posting.docid + "," + toOccuranceListString(posting.oc));
			  writer.newLine();
		  }
		  tmap.clear();
		  writer.close();
		  System.gc();
	  }
	  
	  private String toOccuranceListString(ArrayList<Integer> list) {
		  String result = "";
		  
		  for(int i = 0; i < list.size(); i++) {
			  if (i == list.size() - 1)
				  result += list.get(i);
			  else 
				  result += list.get(i) + ",";
		  }
		  return result;
	  }

	  private void merge(int num) throws IOException{
		  BufferedReader[] readers= new BufferedReader[num];
		  for (int i = 1;i <= num;i++) {
			  String partialFile = _options._indexPrefix + "/invertedCompress_" + Integer.toString(i) + ".idx";
			  BufferedReader reader = new BufferedReader(new FileReader(partialFile));
			  readers[i-1] = reader;
		  }
		  
		  String mergedFile = _options._indexPrefix + "/invertedCompressMerged.idx";
		  DataOutputStream writer = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(mergedFile)));
		  
		  String offsetFileName = _options._indexPrefix + "/invertedCompressOffset.idx";
		  RandomAccessFile offsetFile = new RandomAccessFile(offsetFileName, "rw");
		  
		  boolean[] toMove = new boolean[num];
		  Arrays.fill(toMove, true);
		  int min_id = Integer.MAX_VALUE;
		  int[] ids = new int[num];
		  TokenInfo[] infos = new TokenInfo[num];
		  long offset = 0;
		  
		  while(true) {
			  min_id = Integer.MAX_VALUE;
			  for (int i = 0; i < num; i++) {
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
			  for (int i = 0; i < num; i++) {
				  if (ids[i] == min_id) {
					  toMerge.add(infos[i]);
					  toMove[i] = true;
				  }
			  }
			  long size = writeToIndex(min_id, toMerge, writer);
			  offset += size;
			  offsetFile.writeLong(offset);
		  }
		  for (BufferedReader r: readers)
			  r.close();
		  writer.close();
		  offsetFile.close();
		  
		  // delete all the partial files
		  for (int i = 1;i <= num;i++) {
			  String partialFile = _options._indexPrefix + "/invertedCompress_" + Integer.toString(i) + ".idx";
			  File file = new File(partialFile);
			  file.delete();
		  }
	  }
	  
	  private long writeToIndex(int termId,ArrayList<TokenInfo> list,DataOutputStream writer) throws IOException{
		  long result = 0L;
		  TokenInfo info = new TokenInfo();
		  info.corpusFreq = 0;
		  info.docFreq = 0;
		  info.postingList = new ArrayList<Posting>();
		  for (TokenInfo i: list) {
			  info.corpusFreq += i.corpusFreq;
			  info.docFreq += i.docFreq;
			  info.postingList.addAll(i.postingList);
		  }
		  
		  byte[] temp;
		  
		  temp = vByte(termId);
		  writer.write(temp);
		  result += temp.length;
		  temp = vByte(info.corpusFreq);
		  writer.write(temp);
		  result += temp.length;
		  temp = vByte(info.docFreq);
		  writer.write(temp);
		  result += temp.length;
		  for (Posting p: info.postingList) {
			  temp = vByte(p.docid);
			  writer.write(temp);
			  result += temp.length;
			  temp = vByte(p.oc.size());
			  writer.write(temp);
			  result += temp.length;
			  for (int o: p.oc) {
				  temp = vByte(o);
				  writer.write(temp);
				  result += temp.length;
			  }
		  }
		  return result;
	  }
	  
	  private TokenInfo fetchInfo(int id) throws FileNotFoundException, IOException{
		  long offset;
		  long next_offset;
		  String offsetFileName = _options._indexPrefix + "/invertedCompressOffset.idx";
		  RandomAccessFile offsetFile = new RandomAccessFile(offsetFileName, "r");
		  if (id == 0)
			  offset = 0;
		  else {
			  offsetFile.seek(8L * (id - 1));
			  offset = offsetFile.readLong();
		  }
		  
		  offsetFile.seek(8L * id);
		  next_offset = offsetFile.readLong();
		  
		  int size = (int)(next_offset - offset);
		  String indexFileName = _options._indexPrefix + "/invertedCompressMerged.idx";
		  RandomAccessFile indexFile = new RandomAccessFile(indexFileName,"r");
		  indexFile.seek(offset);
		  
		  ArrayList<Byte> byte_list = new ArrayList<Byte>();
		  for (int i=0;i<size;i++) {
			  byte_list.add(indexFile.readByte());
		  }
		  List<Integer> int_list = decodeByte(byte_list);
		  
		  TokenInfo info = new TokenInfo();
		  info.corpusFreq = int_list.get(1);
		  info.docFreq = int_list.get(2);
		  info.postingList = new ArrayList<Posting>();
		  int cur = 3;
		  while (cur<int_list.size()) {
			  int docid = int_list.get(cur);
			  cur++;
			  Posting p = new Posting(docid);
			  int oc_count = int_list.get(cur);
			  cur++;
			  for (int i=0;i<oc_count;i++) {
				  p.oc.add(int_list.get(cur));
				  cur++;
			  }
			  info.postingList.add(p);
		  }
		  
		  offsetFile.close();
		  indexFile.close();
		  return info;
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
		  info.postingList = new ArrayList<Posting>();
		  while (s.hasNext()) {
			  String[] e = s.next().split(",");
			  
			  Posting p = new Posting(Integer.parseInt(e[0]));
			  for (int i = 1;i < e.length; i++) { p.oc.add(Integer.parseInt(e[i])); }
			  info.postingList.add(p);
			  
		  }
		  s.close();
		  return info;
	  }

	  @Override
	  public void loadIndex() throws IOException, ClassNotFoundException {
		 String indexFile = _options._indexPrefix + "/invertedCompressDoc.idx";
		 System.out.println("Loading documents from: " + indexFile);
		 ObjectInputStream reader = new ObjectInputStream(new BufferedInputStream(new FileInputStream(indexFile)));
		 this._documents = (Vector<DocumentIndexed>)reader.readObject();
		 reader.close();
		 
		 System.out.println("loading dictionary");
		 String termIndexFile = _options._indexPrefix + "/invertedCompressTerm.idx";
		 reader = new ObjectInputStream(new BufferedInputStream(new FileInputStream(termIndexFile)));
		 this._dictionary = (Map<String, Integer>)reader.readObject();
		 reader.close();
		 
		 // Compute numDocs and totalTermFrequency b/c Indexer is not serializable.
		 this._numDocs = _documents.size();
		 String totalFreqFileName = _options._indexPrefix + "/invertedCompressFreq.idx";
		 RandomAccessFile freqFile = new RandomAccessFile(totalFreqFileName, "r");
		 this._totalTermFrequency = freqFile.readLong();
		 System.out.println("total corpus freq: " + this._totalTermFrequency);
		 freqFile.close();

		 
		 this.cachedPtrArray = new int[_dictionary.size()];
		 Arrays.fill(cachedPtrArray,0);
		 
	  }

	  @Override
	  public DocumentIndexed getDoc(int docid) {
	    return _documents.get(docid);
	  }

	  /**
	   * In HW2, you should be using {@link DocumentIndexed}.
	   */
	  @Override
	  public DocumentIndexed nextDoc(Query query, int doc_id) {
		  while(true) {
			  Integer candidate = nextDoc_withAllTokens(query._tokens,doc_id);
			  if (candidate == null) {
				  tmap.clear();
				  return null;
			  }
			  boolean containAll = true;
			  for (Vector<String> phrase: query._phrases) {
				  if (!containsPhrase(phrase,candidate)) {
					  containAll = false;
					  break;
				  }
			  }
			  if (containAll)
				  return _documents.get(candidate);
			  else
				  doc_id = candidate;
		  }
	  }
	  
	  private boolean containsPhrase (Vector<String> phrase, int docid) {
		  Integer pos = -1;
		  while (true) {
			  boolean contains = true;
			  List<Integer> positions = new ArrayList<Integer>();

		      for (String term : phrase) {
		        Integer p = nextPos(term, docid, pos);
		        if (p == null) {
		          return false;
		        }
		        positions.add(p);
		      }

		      int p1 = positions.get(0);
		      for (int i = 1; i < positions.size(); i++) {
		        int p2 = positions.get(i);
		        if ((p1 + 1) != p2) {
		          contains = false;
		          break;
		        }
		        p1 = p2;
		      }
		      if (contains) {
		        return true;
		      }
		      pos = Collections.min(positions);
		  }
	  }
	  
	  private Integer nextPos(String token, int docid, int pos) {
		  Integer idx = _dictionary.get(token);
		  if (null == idx)
			  return null;
		  
		  if (!tmap.containsKey(idx)) {
			  try {
				  tmap.put(idx, fetchInfo(idx));
			  }
			  catch(Exception e) {}
		  }
		  ArrayList<Posting> pl = tmap.get(idx).postingList;
		  Posting p = searchPosting(pl,docid);
		  if (null == p)
			  return null;
		  ArrayList<Integer> oc = p.oc;
		  return searchOc(oc,pos);
	  }
	  
	  private Posting searchPosting(ArrayList<Posting> pl, int docid) {
		  for (int i=0;i<pl.size();i++) {
			  if (pl.get(i).docid == docid)
				  return pl.get(i);
			  if (pl.get(i).docid > docid)
				  return null;
		  }
		  return null;
	  }
	  
	  private Integer searchOc(ArrayList<Integer> list, int pos) {
		  for (int i=0;i<list.size();i++) {
			  if (list.get(i)>pos)
				  return list.get(i);
		  }
		  return null;
	  }
	  
	  private Integer nextDoc_withAllTokens(Vector<String> tokens, int docid) {
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
			  return prev;
		  else
			  return nextDoc_withAllTokens(tokens,max-1);
	  } 
	  
	  private int[] cachedPtrArray;
	  
	  private Integer next(String token, int docid) {
		  Integer idx = _dictionary.get(token);
		  if (null == idx)
			  return null;
		  
		  int cachedPtr = cachedPtrArray[idx];
		  ArrayList<Posting> pl;
		  if (!tmap.containsKey(idx)) {
			  try {
				  tmap.put(idx, fetchInfo(idx));
			  }
			  catch(Exception e) {}
		  }
		  pl = tmap.get(idx).postingList;
		  if (pl.get(pl.size()-1).docid <= docid)
			  return null;
		  
		  if (pl.get(0).docid > docid) {
			  cachedPtrArray[idx] = 0;
			  return pl.get(0).docid;
		  }
		  
		  if (cachedPtr > 0 && pl.get(cachedPtr-1).docid > docid)
			  cachedPtr = 0;
		  while (pl.get(cachedPtr).docid <= docid)
			  cachedPtr++;
		  cachedPtrArray[idx] = cachedPtr;
		  return pl.get(cachedPtr).docid;
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
		  ArrayList<Posting> pl = info.postingList;
		  for (int i=0;i<pl.size();i++) {
			  if (pl.get(i).docid == docid)
				  return pl.get(i).oc.size();
			  if (pl.get(i).docid > docid)
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
	  
	  private String stem(String origin, Stemmer stemmer) {
		  String lower = origin.toLowerCase();
	      stemmer.add(lower.toCharArray(), lower.length());
	      stemmer.stem();
	      return stemmer.toString();
	  }
	  
	  private byte[] vByte(int num) {
		  byte[] ret = null;
		  if (num < 128) {
			  ret = new byte[1];
		      ret[0] = (byte) (num + 128);
		      return ret;
		  } else if (num < 16384) {
			  ret = new byte[2];
		      ret[0] = (byte) (num / 128);
		      ret[1] = (byte) (num % 128 + 128);
		  } else if (num < 2097152) {
		      ret = new byte[3];
		      ret[0] = (byte) (num / 16384);
		      byte[] rest = vByte(num % 16384);
		      if (rest.length == 1) {
		    	  ret[1] = 0;
		    	  ret[2] = rest[0];
		      } else {
		    	  ret[1] = rest[0];
		    	  ret[2] = rest[1];
		      }
		  } else if (num < 268435456) {
			  ret = new byte[4];
		      ret[0] = (byte) (num / 2097152);
		      byte[] rest = vByte(num % 2097152);
		      if (rest.length == 1) {
		    	  ret[1] = 0;
		    	  ret[2] = 0;
		    	  ret[3] = rest[0];
		      } else if (rest.length == 2) {
		    	  ret[1] = 0;
		    	  ret[2] = rest[0];
		    	  ret[3] = rest[1];
		      } else if (rest.length == 3) {
		    	  ret[1] = rest[0];
		    	  ret[2] = rest[1];
		    	  ret[3] = rest[2];
		      }
		  }
		  return ret;
	  }

	  private List<Integer> decodeByte(List<Byte> list) {
		  List<Byte> byteList = new ArrayList<Byte>();
		  List<Integer> ret = new ArrayList<Integer>();
		  for (int i = 0; i < list.size(); i++) {
			  if (list.get(i) < 0) {
				  byteList.add(list.get(i));
				  ret.add(convert(byteList));
				  byteList.clear();
			  } else {
				  byteList.add(list.get(i));
			  }
		  }
		  return ret;
	  }

	  private int convert(List<Byte> byteList) {
		  if (byteList.size() == 1) {
			  return (byteList.get(0) + 128);
		  } else if (byteList.size() == 2) {
			  return (byteList.get(0) * 128 + (byteList.get(1) + 128));
		  } else if (byteList.size() == 3) {
			  return (byteList.get(0) * 16384 + byteList.get(1) * 128 + (byteList.get(2) + 128));
		  } else {
			  return (byteList.get(0) * 2097152 + byteList.get(1) * 16384 + byteList.get(2) * 128 + (byteList.get(3) + 128));
		  }
	  }
	  
	  class Posting {
		  public int docid;
		  
		  /* Stored each position of the term occurs
		     the size of the arraylist is the number of occurence */
		  public ArrayList<Integer> oc = new ArrayList<Integer>();
		    
		  public Posting(int docid){
		      this.docid = docid;
		  }
	  }
	  
	  class TokenInfo {
		  public int corpusFreq;
		  public int docFreq;
		  public ArrayList<Posting> postingList;
		  
		  public TokenInfo() {}
		  
		  public TokenInfo(int doc_id) {
			  corpusFreq = 0;
			  docFreq = 0;
			  postingList = new ArrayList<Posting>();
			  postingList.add(new Posting(doc_id));
		  }
	  }
	  
	  // class to store pageRank and numViews
	  class Pair {
		  float pageRank;
		  int numViews;
		  Pair(float p, int n) {
			  pageRank = p;
			  numViews = n;
		  } 
	  }
}
