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

  // Stores all Document in memory.
  private Vector<DocumentIndexed> _documents = new Vector<DocumentIndexed>();
  
  //Each element in the array is the term frequency of the terms that appears in a particular document
  private ArrayList<HashMap<Integer,Integer>> _termFrequencyMapArray = new ArrayList<HashMap<Integer,Integer>>();

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
      String indexFile = _options._indexPrefix + "/corpus.idx";
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
      
      
	  String text = "";										// the text of parsed document
      try {
    	  org.jsoup.nodes.Document document = Jsoup.parse(file, null);
    	  doc.setTitle(document.title());
		  _documents.add(doc);
		  text = document.body().text();
		  document = null;		    	  
      } catch (IOException e) {
		  System.err.println(e.getMessage());
	  }
	  
	  Set<Integer> uniq_set = new HashSet<Integer>();	// the uniq term set for this document
	  Scanner[] sr = new Scanner[2];
	  sr[0] = new Scanner(text).useDelimiter("\\s+");
	  sr[1] = new Scanner(doc.getTitle()).useDelimiter("\\s+");
	
	  for (Scanner scanner: sr) {
		  while(scanner.hasNext()) {
			  int offset = 1;								// offset of the token
			  String token = stem(scanner.next(),stemmer);	// stem each term
			  if (!token.equals("")) {
				  int idx;									// integer representation of the term
				  
				  if (_dictionary.containsKey(token)) {		// if the term appears in corpus
					  idx = _dictionary.get(token);
					  if (uniq_set.contains(token)) {						  
						  _postingList.get(idx).get(docid - 1).oc.add(offset);						  
						  _termFrequencyMap.put(idx, _termFrequencyMap.get(idx)+1);						  
					  } else {		// if the this term first appears in this document
						  uniq_set.add(idx);
						  
						  Posting posting = new Posting(docid);
						  posting.oc.add(offset);						  
						  					  
						  _postingList.get(idx).get(docid - 1).oc.add(offset);
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
	 String indexFile = _options._indexPrefix + "/corpus.idx";
	 System.out.println("Load index from: " + indexFile);

	 ObjectInputStream reader = new ObjectInputStream(new FileInputStream(indexFile));
	 IndexerInvertedOccurrence loaded = (IndexerInvertedOccurrence)reader.readObject();
	 
	 
	 
  }

  @Override
  public DocumentIndexed getDoc(int docid) {
    return null;
  }

  /**
   * In HW2, you should be using {@link DocumentIndexed}.
   */
  @Override
  public DocumentIndexed nextDoc(Query query, int docid) {
    return null;
  }

  @Override
  public int corpusDocFrequencyByTerm(String term) {
    return 0;
  }

  @Override
  public int corpusTermFrequency(String term) {
    return 0;
  }

  @Override
  public int documentTermFrequency(String term, String url) {
    SearchEngine.Check(false, "Not implemented!");
    return 0;
  }
  
  private String stem(String origin, Stemmer stemmer) {
	  String lower = origin.toLowerCase();
      stemmer.add(lower.toCharArray(), lower.length());
      stemmer.stem();
      return stemmer.toString();
  }
}
