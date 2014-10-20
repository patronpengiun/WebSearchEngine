package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

import org.jsoup.Jsoup;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedOccurrence extends Indexer implements Serializable {

  private static final long serialVersionUID = 1077111905740085031L;

  // Maps each term to their integer representation
  private Map<String, Integer> _dictionary = new HashMap<String, Integer>();

  // All unique terms appeared in corpus. Offsets are integer representations.
  private Vector<String> _terms = new Vector<String>();

  // Term document frequency, key is the integer representation of the term and
  // value is the number of documents the term appears in.
  private Map<Integer, Integer> _termDocFrequency =
      new HashMap<Integer, Integer>();

  // Term frequency, key is the integer representation of the term and value is
  // the number of times the term appears in the corpus.
  private Map<Integer, Integer> _termCorpusFrequency =
      new HashMap<Integer, Integer>();
  
  // Url to docid used in documentTermFrequency, key is the url of the document,
  // value is the document id
  private Map<String, Integer> _urlToDoc = new HashMap<String, Integer>(); 

  // Stores all Document in memory.
  private Vector<DocumentIndexed> _documents = new Vector<DocumentIndexed>();
  
  // Each element in the array is the term frequency of 
  // the terms that appears in a particular document
  private ArrayList<HashMap<Integer,Integer>> _termFrequencyMapArray = 
		  new ArrayList<HashMap<Integer,Integer>>();

  private class Posting implements Serializable {
	  public int docid;
	  
	  /* Stored each position of the term occurs
	     the size of the arraylist is the number of occurence */
	  public ArrayList<Integer> oc = new ArrayList<Integer>();
	    
	  public Posting(int docid){
	      this.docid = docid;
	  }
  }
  
  // The index list of each term and posting, key is the integer representation
  // of each term, value is posting of that term
  private Map<Integer, ArrayList<Posting>> _postingList = 
		  new HashMap<Integer, ArrayList<Posting>>();
  
  // Provided for serialization
  public IndexerInvertedOccurrence() { }

  public IndexerInvertedOccurrence(Options options) {
    super(options);
    System.out.println("Using Indexer: " + this.getClass().getSimpleName());
  }

  @Override
  public void constructIndex() throws IOException {	  
	  try {
          String corpus = _options._corpusPrefix + "/";
          System.out.println("Construct index from: " + corpus);

          File corpusFiles = new File(corpus);
          
          // the parser is used to parse html into plain text
          Stemmer stemmer = new Stemmer();
          
          for (final File file : corpusFiles.listFiles())
          {
              System.out.println(file.getName());
                  processDocument(file, stemmer); 
          }
      } catch (Exception e) {
          e.printStackTrace();
      }

      System.out.println(
              "Indexed " + Integer.toString(_numDocs) + " docs with "
              + Long.toString(_totalTermFrequency) + " terms.");

      // serialize the index(posting list)
      String indexFile = _options._indexPrefix + "/invertedOccurance.idx";
      System.out.println("Store index to: " + indexFile);
      ObjectOutputStream writer = 
    		  new ObjectOutputStream(new FileOutputStream(indexFile));
      writer.writeObject(this); //write the entire class into the file
      writer.close();	  
  }

  private void processDocument(File file, Stemmer stemmer) {
		  
	  int docid = _documents.size();
      DocumentIndexed doc = new DocumentIndexed(docid);
      doc.setUrl(Integer.toString(docid));
      _documents.add(doc);
      _numDocs++;
      
      _termFrequencyMapArray.add(new HashMap<Integer,Integer>());
	  HashMap<Integer,Integer> _termFrequencyMap = _termFrequencyMapArray.get(_termFrequencyMapArray.size() - 1);
      
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
	  sr[0] = new Scanner(text).useDelimiter("\\s+");
	  sr[1] = new Scanner(doc.getTitle()).useDelimiter("\\s+");
	
	  for (Scanner scanner: sr) {
		  while(scanner.hasNext()) {
			  // offset of the token
			  int offset = 1;
			  
			  // stem each term
			  String token = stem(scanner.next(),stemmer);
			  
			  if (!token.equals("")) {
				  // integer representation of the term
				  int idx;
				  
				  // if the term appears in corpus
				  if (_dictionary.containsKey(token)) {
					  idx = _dictionary.get(token);
					  if (uniq_set.contains(token)) {						  
						  _postingList.get(idx).get(docid).oc.add(offset);						  
						  _termFrequencyMap.put(idx, _termFrequencyMap.get(idx)+1);						  
					  } else {// if the this term first appears in this document
						  uniq_set.add(idx);
						  
						  Posting posting = new Posting(docid);
						  posting.oc.add(offset);						  
						  					  
						  _postingList.get(idx).add(posting);
						  
						  _termFrequencyMap.put(idx,1);
						  _termDocFrequency.put(idx,_termDocFrequency.get(idx)+1);
					  }	
				  } else {
					idx = _dictionary.size();
					_dictionary.put(token, idx);
					uniq_set.add(idx);
					
					Posting posting = new Posting(docid);
					posting.oc.add(offset);
					
					// the list of positions of term occurs in this document
				    ArrayList<Posting> postings = new ArrayList<Posting>();
					postings.add(posting);					
					_postingList.put(idx, postings);
					
					_termFrequencyMap.put(idx,1);
					_termDocFrequency.put(idx,1);
					_termCorpusFrequency.put(idx, 1);
				  }
			  }
			  
			  ++offset;
		  }
		  scanner.close();
	  }
  }

@Override
  public void loadIndex() throws IOException, ClassNotFoundException {
	 String indexFile = _options._indexPrefix + "/invertedOccurance.idx";
	 System.out.println("Loading index from: " + indexFile);
	 ObjectInputStream reader = new ObjectInputStream(new FileInputStream(indexFile));
	 IndexerInvertedOccurrence loaded = (IndexerInvertedOccurrence)reader.readObject();	
	 this._documents = loaded._documents;
	 
	 // Compute numDocs and totalTermFrequency b/c Indexer is not serializable.
	 this._numDocs = _documents.size();
	 for (Integer freq : loaded._termCorpusFrequency.values()) {
		 this._totalTermFrequency += freq;
	 }

	 this._postingList = loaded._postingList;
	 this._termCorpusFrequency = loaded._termCorpusFrequency;
	 this._urlToDoc = loaded._urlToDoc;
	 reader.close();

	 //printIndex();

	 System.out.println(Integer.toString(_numDocs) + " documents loaded " +
			 "with " + Long.toString(_totalTermFrequency) + " terms!");
	 
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
	  boolean flag = false;		// mark whether we should continue getting next document
	  Integer docid = doc_id;

	  while((docid = next_doc_terms(query._tokens,docid)) != null){
		  flag = false;

		  // if this document have the complete phrases we are querying
		  for(Vector<String> phrase : query._phrases){
			  if(nextPhrase(phrase,docid,-1) == null){
				  flag = true;
				  break;
			  }
		  }
		  if(flag){
			  continue;
		  }else{
			  // found the document and return it
			  DocumentIndexed result = new DocumentIndexed(docid);
			  return result;
		  }
	  }
	  // if we reaches here, we have not find any matched document
	  return null;
  }
  
  private Integer next_doc_terms(Vector<String> tokens, int docid) {
	  if(tokens.size() <= 0){
	      if(docid <= 0){
	        return 1;
	      }else if(docid >= _numDocs){
	        return null;
	      }else{
	        return docid + 1;
	      }
	    }
	    int did = next_doc_term(tokens.get(0), docid); 
	    boolean returnable = true;
	    int max_docid = did;
	    int i = 1;
	    Integer tempDid;
	    for(;i < tokens.size(); i++){
	      tempDid = next_doc_term(tokens.get(i), docid);
	      //one of the term will never find next
	      if(tempDid == null){
	        return null;
	      }
	      if(tempDid > max_docid){
	        max_docid = tempDid;
	      } 
	      if(tempDid != did){
	        returnable = false;
	      }
	    }    
	    if(returnable){
	      return did;
	    }else{
	      return next_doc_terms(tokens, max_docid - 1);
	    }
  }

  private Integer next_doc_term(String term, int docid) {
	  if(_postingList.containsKey(term)){
		  ArrayList<Posting> postings = _postingList.get(term);
		  int largest = postings.get(postings.size() - 1).docid;
		  if(largest < docid){
			  return null;
		  }
		  if(postings.get(0).docid > docid){
			  return (Integer)postings.get(0).docid;
		  }
		  return binarySearchDoc(postings, 0, postings.size() - 1, docid);
	  }
	  return null;
  }

  private Integer binarySearchDoc(ArrayList<Posting> postings, int low, int high, int docid) {
	  int mid;
	    while((high-low)>1){
	      mid = (low+high)/2;
	      if(postings.get(mid).docid <= docid){
	        low = mid;
	      }else{
	        high = mid;
	      }
	    }
	    return (Integer)postings.get(high).docid;
  }

  private Integer nextPhrase(Vector<String> phrase, int docid, int pos) {
	  int did = next_doc_terms(phrase, docid - 1);
	    if(docid != did){
	      return null;
	    }
	    int position = next_pos_term(phrase.get(0), docid, pos); 
	    boolean returnable = true;
	    int largestPos = position;
	    int i = 1;
	    Integer tempPos;
	    for(;i<phrase.size();i++){
	      tempPos = next_pos_term(phrase.get(i), docid, pos);
	      
	      if(tempPos == null){
	        return null;
	      }
	      if(tempPos>largestPos){
	        largestPos = tempPos;
	      } 
	      if(tempPos!=position+1){
	        returnable = false;
	      }else{
	        position = tempPos;
	      }
	    }    
	    if(returnable){
	      return position;
	    }else{
	      return nextPhrase(phrase, docid, largestPos);
	    }
	    
  }

  // the next occurrence of the term in docid after pos
  private Integer next_pos_term(String term, int docid, int pos) {
	  if(_postingList.containsKey(term)){
	      ArrayList<Posting> postings = _postingList.get(term);
	      Posting posting = binarySearchPosting(postings, 0, postings.size() - 1, docid);
	      if(posting == null){
	        return null; 
	      }
	      Integer max = posting.oc.get(posting.oc.size() -1);
	      if(max < pos){
	        return null;
	      }
	      if(posting.oc.get(0) > pos){
	        return (Integer)posting.oc.get(0);
	      }
	      return binarySearchPositions(posting.oc,0,posting.oc.size(),pos);
	    }
	    return null;
  }

  private Integer binarySearchPositions(ArrayList<Integer> oc, int low, int high, int pos) {
	  int mid;
	    while((high - low) > 1){
	      mid = (low + high) / 2;
	      if(oc.get(mid) <= pos){
	        low = mid;
	      }else{
	        high = mid;
	      }
	    }
	    return (Integer)oc.get(high);
  }

private Posting binarySearchPosting(ArrayList<Posting> postings, int low, int high, int docid) {
	  int mid;
	    while((high - low) > 1){
	      mid = (low + high) / 2;
	      if(postings.get(mid).docid <= docid){
	        low = mid;
	      }else{
	        high = mid;
	      }
	    }
	    if(postings.get(high).docid == docid){
	      return postings.get(high);
	    }else{
	      return null;
	    }
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
}
