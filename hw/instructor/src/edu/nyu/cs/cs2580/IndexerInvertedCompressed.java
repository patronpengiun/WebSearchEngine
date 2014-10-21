package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;
import java.util.*;

import org.jsoup.Jsoup;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedCompressed extends Indexer implements Serializable {
	
	  private static final long serialVersionUID = 1077111905740085031L;

	  // Maps each term to their integer representation
	  private Map<String, Integer> _dictionary = new HashMap<String, Integer>();

	  // All unique terms appeared in corpus. Offsets are integer representations.
	  private Vector<String> _terms = new Vector<String>();
	  
	  // Url to docid used in documentTermFrequency, key is the url of the document,
	  // value is the document id
	  private Map<String, Integer> _urlToDoc = new HashMap<String, Integer>(); 

	  // Stores all Document in memory.
	  protected Vector<DocumentIndexed> _documents = new Vector<DocumentIndexed>();
	  
	  // Each element in the array is the term frequency of 
	  // the terms that appears in a particular document
	  private ArrayList<HashMap<Integer,Integer>> _termFrequencyMapArray = 
			  new ArrayList<HashMap<Integer,Integer>>();
	  
	  //All the information about a token
	  private HashMap<Integer,TokenInfo> tmap = new HashMap<Integer,TokenInfo>();
	  
	  
	  // Provided for serialization
	  
  public IndexerInvertedCompressed(Options options) {
    super(options);
    System.out.println("Using Indexer: " + this.getClass().getSimpleName());
  }

  @Override
  public void constructIndex() throws IOException {
 int num_pieces = 0;
	  
	  try {
          String corpus = _options._corpusPrefix;
          System.out.println("Construct index from: " + corpus);

          File corpusFiles = new File(corpus);
          File[] listFiles = corpusFiles.listFiles();
          int numOfDoc = listFiles.length;
          int count = 0, temp = 0;
          
          // the parser is used to parse html into plain text
          Stemmer stemmer = new Stemmer();
          
          for (File file : listFiles) {
        	  count++;
        	  temp++;
        	  System.out.println(listFiles.length + " / " + count);       	  
              System.out.println(file.getName());
              
              if (file.isFile()) {
            	  processDocument(file, stemmer); 
    		  }
    		  if (temp > numOfDoc / 10) {
    			  temp = 0;
    			  num_pieces++;
    			  dump(num_pieces);
    		  }
          }		               
      } catch (Exception e) {
          e.printStackTrace();
      }
	  
	  num_pieces++;
	  dump(num_pieces);
	  
	  System.out.println("merging pieces");
	  merge(num_pieces);
     
	  System.out.println("store documents idx");
	  String documentIndexFile = _options._indexPrefix + "/invertedCompressDoc.idx";
	  ObjectOutputStream writer =
		  new ObjectOutputStream(new FileOutputStream(documentIndexFile));
	  writer.writeObject(_documents);
	  writer.close();
		  
	  System.out.println("store dictionary idx");
	  String termIndexFile = _options._indexPrefix + "/invertedCompressTerm.idx";
	  writer = new ObjectOutputStream(new FileOutputStream(termIndexFile));
	  writer.writeObject(_dictionary);
	  writer.close();
	   }
  
  
  private void processDocument(File file, Stemmer stemmer) {		  
	  int doc_id = _documents.size();
      DocumentIndexed doc = new DocumentIndexed(doc_id);
      doc.setUrl(Integer.toString(doc_id));
         
	  // the text of parsed document
	  String text = "";	
      try {
    	  org.jsoup.nodes.Document document = Jsoup.parse(file, null);
    	  doc.setTitle(document.title());
		  _documents.add(doc);
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
				  // integer representation of the term
				  int idx;				  
				  // if the term appears in corpus
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
				  if (plist.get(plist.size()-1).docid == doc_id){
					  int size = plist.get(plist.size()-1).oc.size();
					  if(size == 0) {
						  plist.get(plist.size()-1).oc.add(offset);
					  	}
					  else{
						  int add = plist.get(plist.size()-1).oc.get(size - 1);
						  plist.get(plist.size()-1).oc.add(offset - add);
					  	}
					  }
				  else{
					  Posting posting = new Posting(doc_id);
				   	  posting.oc.add(offset);
					  plist.add(new Posting(doc_id));	
				  }
			  }
			  
			  ++offset;
		  }
		  scanner.close();
	  }
	  for (int tid: uniq_set) {
		  tmap.get(tid).docFreq += 1;
	  }
  }
  

  private void dump(int n) throws IOException {
	  String indexFile = _options._indexPrefix + "/invertedCompressed_" + Integer.toString(n) + ".idx";
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

  private void merge (int num) throws IOException{
	  BufferedReader[] readers= new BufferedReader[num];
	  for (int i = 1;i <= num;i++) {
		  String partialFile = _options._indexPrefix + "/invertedOccurance_" + Integer.toString(i) + ".idx";
		  BufferedReader reader = new BufferedReader(new FileReader(partialFile));
		  readers[i-1] = reader;
	  }
	  
	  String mergedFile = _options._indexPrefix + "/invertedOccuranceMerged.idx";
	  BufferedWriter writer = new BufferedWriter(new FileWriter(mergedFile));
	  
	  String offsetFileName = _options._indexPrefix + "/invertedOccuranceOffset.idx";
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
  }
  
  private long writeToIndex(int termId,ArrayList<TokenInfo> list,BufferedWriter writer) throws IOException{
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
	  String temp = termId + " " + info.corpusFreq + " " + info.docFreq;
	  writer.write(temp);
	  result += temp.length();
	  for (Posting posting: info.postingList) {
		  temp = " " + posting.docid + "," + posting.oc;
		  result += temp.length();
		  writer.write(temp);
	  }
	  result++;
	  writer.newLine();
	  return result;
  }
  
  private TokenInfo fetchInfo(int id) throws FileNotFoundException, IOException{
  	  long offset;
  	  String offsetFileName = _options._indexPrefix + "/invertedOccuranceOffset.idx";
  	  RandomAccessFile offsetFile = new RandomAccessFile(offsetFileName, "r");
  	  if (id == 0)
  		  offset = 0;
  	  else {
  		  offsetFile.seek(8L * (id - 1));
  		  offset = offsetFile.readLong();
  	  }
  	  
  	  String indexFileName = _options._indexPrefix + "/invertedOccuranceMerged.idx";
  	  RandomAccessFile indexFile = new RandomAccessFile(indexFileName,"r");
  	  indexFile.seek(offset);
  	  String line = indexFile.readLine();
  	  offsetFile.close();
  	  indexFile.close();
  	  return getInfo(line);
     }
 
  private int getId(String line) {
	  Scanner s = new Scanner(line);
	  return Integer.parseInt(s.next());
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
		  info.postingList.add(new Posting(Integer.parseInt(e[0].trim())));
	  }
	  return info;
  }

  @Override
  public void loadIndex() throws IOException, ClassNotFoundException {
	  	 String indexFile = _options._indexPrefix + "/invertedCompressed.idx";
		 System.out.println("Loading index from: " + indexFile);
		 ObjectInputStream reader = new ObjectInputStream(new FileInputStream(indexFile));
		 IndexerInvertedCompressed loaded = (IndexerInvertedCompressed)reader.readObject();	
		 this._documents = loaded._documents;
		 
		 System.out.println("loading dictionary");
		 String termIndexFile = _options._indexPrefix + "/invertedCompressedTerm.idx";
		 reader = new ObjectInputStream(new FileInputStream(termIndexFile));
		 this._dictionary = (Map<String, Integer>)reader.readObject();
		 reader.close();
		 
		 // Compute numDocs and totalTermFrequency b/c Indexer is not serializable.
		 this._numDocs = _documents.size();
		  
		 this.cachedPtrArray = new int[_dictionary.size()];
		 Arrays.fill(cachedPtrArray,0);
	  
  }
  
  private int[] cachedPtrArray;

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
		  System.out.println(query._tokens);
		  Integer candidate = nextDoc_withAllTokens(query._tokens,doc_id);
		  System.out.println(candidate);
		  if (candidate == null)
			  return null;
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
		  return tmap.get(idx).corpusFreq;
  }

  @Override
  public int corpusTermFrequency(String term) {
	  Integer idx = _dictionary.get(term);
	  if (null == idx)
		  return 0;
	  else
		  return tmap.get(idx).docFreq;
  }

  @Override
  public int documentTermFrequency(String term, String url) {
	  if(_urlToDoc.containsKey(url)){
	      int did = _urlToDoc.get(url);
	      QueryPhrase query = new QueryPhrase(term);
	      DocumentIndexed doc = (DocumentIndexed)nextDoc(query,did);
	      if(doc != null){
//	        return doc.getOccurance();	// need to discuss
	    	return 0;
	      }else{
	        return 0;
	      }
	    }else{
	      return 0;
	    }
  }
  
  //get the term frequency of a term in the document with the docid
  public int documentTermFrequency(String term, Integer docid) {
	  Integer idx = _dictionary.get(term);
	  if (null == idx)
		  return 0;
	  else {
		  //if (docid < _termFrequencyMapArray.size())
			  //return _termFrequencyMapArray.get(docid).get(idx);
	  }
	  return 0;
  }
  
  private String stem(String origin, Stemmer stemmer) {
	  String lower = origin.toLowerCase();
      stemmer.add(lower.toCharArray(), lower.length());
      stemmer.stem();
      return stemmer.toString();
  }

	 public String EliasGamaEncode(int k){
		  int d = (int) (Math.log(k)/Math.log(2));
		  int r = (int) (k - Math.pow(2, d));
		  System.out.println(d+"  "+r);
		  String dstr = getD(d) + "0";
		  if(dstr.equals("0")) return dstr;
		  String rstr = getR(r,d);
		  dstr += rstr;
		  return dstr;
	  }
	  
	  public int EliasGamaDecode(String code){
		  
		  int d = code.indexOf("0");
		  int r = 0;
		  if(d < code.length()){
			  StringBuilder rstr = new StringBuilder(code.substring(d + 1));
			  r = convertStringtoInt(rstr.toString());
		  } 
		  return (int)(Math.pow(2, d) + r);
		 	  
	  }
	  
	  public String EliasCitaEncode(int k){
		  int d = (int) (Math.log(k)/Math.log(2));
		  int r = (int) (k - Math.pow(2, d));
		  int dd = (int) (Math.log(d+1)/Math.log(2));
		  int dr = (int) (d - Math.pow(2, dd) + 1);
		  if(dr < 0) dr = 0;
		  String ddstr = getD(dd) + "0";
		  if(ddstr.equals("0")) return ddstr;
		  String rstr = getR(dr,dd);
		  String drstr = getR(r,d);
		  ddstr += rstr + drstr;
		  return ddstr;
	  }
	  
	  public int EliasCitaDecode(String code){
		  
		  int dd = code.indexOf("0");
		  int dr = 0;
		  int d = 0;
		  int r = 0;
		  if(dd < code.length()){
			  StringBuilder drstr = new StringBuilder(code.substring(dd + 1, dd + dd + 1));
			  dr = convertStringtoInt(drstr.toString());
			  d = (int)(Math.pow(2, dd) + dr - 1);
			  StringBuilder rstr = new StringBuilder(code.substring(dd + dd + 1));
			  r = convertStringtoInt(rstr.toString());
		  } 
		  return (int)(Math.pow(2, d) + r);
		 	  
	  }	  
	  
	  
	  public int convertStringtoInt(String str){
		  int i = str.length();
		  int r = 0;
		  for(int j = i - 1; j >= 0 ; j --){
			  r += Integer.parseInt(String.valueOf(str.charAt(j))) * Math.pow(2, (i - j - 1));
			}
		  return r;
	  }
	  
	  public int findD(String dstr){
		  if(dstr == null) return -1;
		  int d = dstr.length() - 1;
		  return d;
	  }

	  public String getD(int d)
	  {
		  StringBuilder str = new StringBuilder();
		  
		  for (; d>0 ; d-- ){  
			  str.append(1);
			  }
		  return str.toString();
	  }
	  
	  public String getR(int r, int d)
	  {
		  StringBuilder str = new StringBuilder();
		  for (int k = d - 1; k>= 0 ; k-- ){  
			  str.append((int) ((r >> k) & 0x1));
			  }
		  return str.toString();
	  }
   
	  
	  class Posting {
		  public int docid;
		  public int count;
		  /* Stored each position of the term occurs
		     the size of the arraylist is the number of occurence */
		  public ArrayList<Integer> oc = new ArrayList<Integer>();
		    
		  public Posting(int docid){
		      this.docid = docid;
		  }
		  
		  public void getCount(){
			  this.count = oc.size();
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
}
