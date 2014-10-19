package edu.nyu.cs.cs2580;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedDoconly extends Indexer implements Serializable{
	
  private static final long serialVersionUID = 165377250909L;
  
  // Stores all document in memory
  transient private Vector<edu.nyu.cs.cs2580.Document> _documents = new Vector<edu.nyu.cs.cs2580.Document>();
	  
  // Maps each term to their integer representation
  transient private Map<String, Integer> _dictionary = new HashMap<String, Integer>();
	  
  // Term document frequency, key is the integer representation of the term and
  // value is the number of documents the term appears in.
  private HashMap<Integer,Integer> _termDocFrequency = new HashMap<Integer,Integer>();
	  
  // Term frequency, key is the integer representation of the term and value is
  // the number of times the term appears in the corpus.
  private HashMap<Integer,Integer> _termCorpusFrequency = new HashMap<Integer,Integer>();
  
  // key is the document id
  // value is the term frequency of the terms that appears in that document
  private HashMap<Integer,HashMap<Integer,Integer>> _termFrequencyMapArray = new HashMap<Integer,HashMap<Integer,Integer>>();
  
  // The postings lists, key is term Id, value is posting list
  private HashMap<Integer,ArrayList<Integer>> _postingLists = new HashMap<Integer,ArrayList<Integer>>();
  
  public IndexerInvertedDoconly(Options options) {
    super(options);
    System.out.println("Using Indexer: " + this.getClass().getSimpleName());
  }

  @Override
  public void constructIndex() throws IOException {
	  int num_pieces = 0;
	  
	  Stemmer stemmer = new Stemmer();
	  File corpusDir = new File(_options._corpusPrefix);
	  File[] docs = corpusDir.listFiles();
	  int numOfDoc = docs.length;
	  int count=0,temp=0;
	  for (File file: docs) {
		  count++;
		  temp++;
		  System.out.println(docs.length + " / " + count);
		  if (file.isFile()) {
			  constructSingleDocument(file.getAbsolutePath(),stemmer);
		  }
		  if (temp > numOfDoc / 10) {
			  temp = 0;
			  num_pieces++;
			  dump(num_pieces);
		  }
	  }
	  num_pieces++;
	  dump(num_pieces);
	  
	  String documentIndexFile = _options._indexPrefix + "/invertedOnlyDoc.idx";
	  ObjectOutputStream writer =
		  new ObjectOutputStream(new FileOutputStream(documentIndexFile));
	  writer.writeObject(_documents);
	  writer.close();
		  
	  String termIndexFile = _options._indexPrefix + "/invertedOnlyTerm.idx";
	  writer = new ObjectOutputStream(new FileOutputStream(termIndexFile));
	  // store the number of pieces that has been dumped to disk
	  _dictionary.put("",num_pieces);
	  writer.writeObject(_dictionary);
	  writer.close();
  }

  @Override
  public void loadIndex() throws IOException, ClassNotFoundException {
	  System.out.println("loading indexedDocVector");
	  String documentIndexFile = _options._indexPrefix + "/invertedOnlyDoc.idx";
	  ObjectInputStream reader =
		  new ObjectInputStream(new FileInputStream(documentIndexFile));
	  this._documents = (Vector<edu.nyu.cs.cs2580.Document>)reader.readObject();
	  reader.close();
	  
	  System.out.println("loading dictionary");
	  String termIndexFile = _options._indexPrefix + "/invertedOnlyTerm.idx";
	  reader = new ObjectInputStream(new FileInputStream(termIndexFile));
	  this._dictionary = (Map<String, Integer>)reader.readObject();
	  reader.close();
	  
	  int num_pieces = _dictionary.get("");
	  _dictionary.remove("");
	  
	  for (int i=1;i<=num_pieces;i++) {
		  System.out.println("loading piece" + Integer.toString(i));
		  String filePath = _options._indexPrefix + "/invertedDocOnly_" + Integer.toString(num_pieces) + ".idx";
		  reader = new ObjectInputStream(new FileInputStream(filePath));
		  IndexerInvertedDoconly load = (IndexerInvertedDoconly)reader.readObject();
		  merge(load);
		  reader.close();
		  load = null;
		  System.gc();
	  }
	  
	  // Compute numDocs and totalTermFrequency b/c Indexer is not serializable.
	  this._numDocs = _documents.size();
	  for (Integer freq : _termCorpusFrequency.values()) {
		  this._totalTermFrequency += freq;
	  }
	  
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
	  ArrayList<Integer> pl = _postingLists.get(idx);
	  if (pl.get(pl.size()-1) <= docid)
		  return null;
	  
	  if (pl.get(0) > docid) {
		  cachedPtrArray[idx] = 0;
		  return pl.get(0);
	  }
	  
	  if (cachedPtr > 0 && pl.get(cachedPtr-1) > docid)
		  cachedPtr = 0;
	  while (pl.get(cachedPtr) <= docid)
		  cachedPtr++;
	  cachedPtrArray[idx] = cachedPtr;
	  return pl.get(cachedPtr);
  }

  @Override
  public int corpusDocFrequencyByTerm(String term) {
	  Integer idx = _dictionary.get(term);
	  if (null == idx)
		  return 0;
	  else
		  return _termDocFrequency.get(idx);
  }

  @Override
  public int corpusTermFrequency(String term) {
	  Integer idx = _dictionary.get(term);
	  if (null == idx)
		  return 0;
	  else
		  return _termCorpusFrequency.get(idx);
  }

  @Override
  public int documentTermFrequency(String term, String url) {
    SearchEngine.Check(false, "Not implemented!");
    return 0;
  }
  
  // get the term frequency of a term in the document with the docid
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
  
  private void constructSingleDocument(String path, Stemmer stemmer) {
	  int doc_id = _documents.size();
	  DocumentIndexed doc = new DocumentIndexed(doc_id);
	  doc.setUrl(Integer.toString(doc_id));
	  _termFrequencyMapArray.put(doc_id,new HashMap<Integer,Integer>());
	  HashMap<Integer,Integer> _termFrequencyMap = _termFrequencyMapArray.get(doc_id);
	  
	  try {
		  org.jsoup.nodes.Document document = Jsoup.parse(new File(path), null);
		  doc.setTitle(document.title());
		  _documents.add(doc);
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
					  int idx; // integer representation of the term
					  if (_dictionary.containsKey(token)) {
						  idx = _dictionary.get(token);
						  if (uniq_set.contains(idx)) {
							  _termFrequencyMap.put(idx, _termFrequencyMap.get(idx)+1);
						  } else {
							  uniq_set.add(idx);
							  if (_postingLists.get(idx) == null) {
								  ArrayList<Integer> newPostingList = new ArrayList<Integer>();
							  	  newPostingList.add(doc_id);
							  	  _postingLists.put(idx, newPostingList);
							  }
							  else
								  _postingLists.get(idx).add(doc_id);
							  _termFrequencyMap.put(idx,1);
							  if (_termDocFrequency.get(idx) == null) 
								  _termDocFrequency.put(idx, 1);
							  else 
								  _termDocFrequency.put(idx,_termDocFrequency.get(idx)+1);
						  }
						  if (_termCorpusFrequency.get(idx) == null)
							  _termCorpusFrequency.put(idx, 1);
						  else 
							  _termCorpusFrequency.put(idx, _termCorpusFrequency.get(idx)+1);
					  }
					  else {
						  idx = _dictionary.size();
						  _dictionary.put(token, idx);
						  uniq_set.add(idx);
						  ArrayList<Integer> newPostingList = new ArrayList<Integer>();
						  newPostingList.add(doc_id);
						  _postingLists.put(idx,newPostingList);
						  _termFrequencyMap.put(idx,1);
						  _termDocFrequency.put(idx,1);
						  _termCorpusFrequency.put(idx, 1);
					  }
				  }
			  }
			  scanner.close();
		  }
	  }
	  catch (IOException e) {
		  System.err.println(e.getMessage());
	  }
  }
  
  private void dump(int n) throws IOException {
	  String indexFile = _options._indexPrefix + "/invertedDocOnly_" + Integer.toString(n) + ".idx";
	  System.out.println("Store index piece " + Integer.toString(n) + " to: " + indexFile);
	  ObjectOutputStream writer =
	      new ObjectOutputStream(new FileOutputStream(indexFile));
	  writer.writeObject(this);
	  writer.close();
	  _termDocFrequency.clear();
	  _termCorpusFrequency.clear();
	  _termFrequencyMapArray.clear();
	  _postingLists.clear();
	  System.gc();
  }
  
  private void merge (IndexerInvertedDoconly piece) {
	  for (Map.Entry<Integer,Integer> e: piece._termDocFrequency.entrySet()) {
		  int termId = e.getKey();
		  int count = e.getValue();
		  if (this._termDocFrequency.get(termId) == null) 
			  this._termDocFrequency.put(termId, count);
		  else 
			  this._termDocFrequency.put(termId,_termDocFrequency.get(termId)+count);
	  }
	  
	  for (Map.Entry<Integer,Integer> e: piece._termCorpusFrequency.entrySet()) {
		  int termId = e.getKey();
		  int count = e.getValue();
		  if (this._termCorpusFrequency.get(termId) == null) 
			  this._termCorpusFrequency.put(termId, count);
		  else 
			  this._termCorpusFrequency.put(termId,_termCorpusFrequency.get(termId)+count);
	  }
	  
	  for (Map.Entry<Integer, HashMap<Integer,Integer>> e: piece._termFrequencyMapArray.entrySet()) {
		  this._termFrequencyMapArray.put(e.getKey(), e.getValue());
	  }
	  
	  for (Map.Entry<Integer, ArrayList<Integer>> e: piece._postingLists.entrySet()) {
		  int termId = e.getKey();
		  ArrayList<Integer> list = e.getValue();
		  if (this._postingLists.get(termId) == null)
			  this._postingLists.put(termId, list);
		  else
			  this._postingLists.get(termId).addAll(list);
	  }
  }
  
  private String stem(String origin, Stemmer stemmer) {
	  String lower = origin.toLowerCase();
      stemmer.add(lower.toCharArray(), lower.length());
      stemmer.stem();
      return stemmer.toString();
  }
}
