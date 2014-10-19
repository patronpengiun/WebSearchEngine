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
          ParseFileHelper parser = new ParseFileHelper();
          for (final File file : corpusFiles.listFiles())
          {
              System.out.println(file.getName());
              String text = parser.parseHtmlText(file);
              int docid = _documents.size();
              DocumentIndexed document = new DocumentIndexed(docid);
              document.setTitle(parser.getDocTitle());
              document.setUrl(url);
              _documents.add(document);
              
              processDocument(text); 
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

  private void processDocument(String text) {
	
	Set<Integer> uniq_set = new HashSet<Integer>();	// the uniq term set for this document
	Scanner sc = new Scanner(text);
	
	while(sc.hasNext()) {
		int offset = 0;								// offset of the token
		String token = sc.next();
		if (_dictionary.containsKey(token)) {
			
		} else {
			int idx = _dictionary.size();
			_dictionary.put(token, idx);
			Posting posting = new Posting(_documents.size());
			posting.oc.add(offset);
			ArrayList<Posting> postings = new ArrayList<Posting>();
			postings.add(posting);
			_postingList.put(idx, postings);
		}
		++offset;
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
}
