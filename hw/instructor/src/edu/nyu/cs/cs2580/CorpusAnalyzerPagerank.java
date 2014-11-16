package edu.nyu.cs.cs2580;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Scanner;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW3.
 */
public class CorpusAnalyzerPagerank extends CorpusAnalyzer {
  public CorpusAnalyzerPagerank(Options options) {
    super(options);
  }
  
  // hash map to store all the pages we have in the corpus and their page rank score
  private HashMap<String,Float> links = new HashMap<String,Float>();

  /**
   * This function processes the corpus as specified inside {@link _options}
   * and extracts the "internal" graph structure from the pages inside the
   * corpus. Internal means we only store links between two pages that are both
   * inside the corpus.
   * 
   * Note that you will not be implementing a real crawler. Instead, the corpus
   * you are processing can be simply read from the disk. All you need to do is
   * reading the files one by one, parsing them, extracting the links for them,
   * and computing the graph composed of all and only links that connect two
   * pages that are both in the corpus.
   * 
   * Note that you will need to design the data structure for storing the
   * resulting graph, which will be used by the {@link compute} function. Since
   * the graph may be large, it may be necessary to store partial graphs to
   * disk before producing the final graph.
   *
   * @throws IOException
   */
  @Override
  public void prepare() throws IOException {
    System.out.println("Preparing " + this.getClass().getName());
    
    File corpusDir = new File(_options._corpusPrefix);
	File[] docs = corpusDir.listFiles();
	
	// add all the internal links(filenames) to the hash map
	for (File doc: docs) {
		if (isValidDocument(doc)) {
			links.put(convertToUTF8(doc.getName()),1.0f);
		}
	}
	
	// the file in which we store the graph as adjacent list
	String graphFile = _options._indexPrefix + "/graphFile";
	BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(graphFile), "UTF-8"));
	
	for (File doc: docs) {
		prepareSingleDoc(doc,writer);
	}
	writer.close();
  }
  
  /**
   * This function computes the PageRank based on the internal graph generated
   * by the {@link prepare} function, and stores the PageRank to be used for
   * ranking.
   * 
   * Note that you will have to store the computed PageRank with each document
   * the same way you do the indexing for HW2. I.e., the PageRank information
   * becomes part of the index and can be used for ranking in serve mode. Thus,
   * you should store the whatever is needed inside the same directory as
   * specified by _indexPrefix inside {@link _options}.
   *
   * @throws IOException
   */
  @Override
  public void compute() throws IOException {
    System.out.println("Computing using " + this.getClass().getName());
    
    int steps = 1;
    float lambda = 0.1f;
    
    // iterative computation
    for (int i=0;i<steps;i++) {
    	HashMap<String,Float> tempMap = new HashMap<String,Float>();
        for (String link: links.keySet()) {
        	tempMap.put(link, lambda / links.size());
        }
    	
    	String graphFile = _options._indexPrefix + "/graphFile";
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(graphFile),"UTF8"));
		String line = null;
		while ((line = reader.readLine()) != null) {
			Scanner s = new Scanner(line).useDelimiter("\t");
			String source = s.next();
			int count = Integer.parseInt(reader.readLine());
			while (s.hasNext()) {
				String target = s.next();
				tempMap.put(target,tempMap.get(target) + links.get(source) * (1-lambda) / count);
			}
			s.close();
		}
		reader.close();
		links = tempMap;
    }
    
    // dump result into disk
    String pageRankFile = _options._indexPrefix + "/pageRank.idx";
	ObjectOutputStream writer =
			new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(pageRankFile)));
	writer.writeObject(links);
	writer.close();
  }

  /**
   * During indexing mode, this function loads the PageRank values computed
   * during mining mode to be used by the indexer.
   *
   * @throws IOException
   */
  @Override
  public Object load() throws IOException {
    System.out.println("Loading using " + this.getClass().getName());
    
    // load the scores from disk
    String pageRankFile = _options._indexPrefix + "/pageRank.idx";
	ObjectInputStream reader =
			new ObjectInputStream(new BufferedInputStream(new FileInputStream(pageRankFile)));
	Object ret = null;
	try {
		ret = reader.readObject();
	}
	catch (Exception e) {}
	reader.close();
	return ret;
  }
  
  private void prepareSingleDoc(File doc, BufferedWriter writer) throws IOException{
	  if (!isValidDocument(doc))
		  return;
	  
	  HeuristicLinkExtractor extractor = new HeuristicLinkExtractor(doc);
	  writer.write(convertToUTF8(extractor.getLinkSource()) + "\t");
	  String link = null;
	  int adj_count = 0;
	  while ((link = convertToUTF8(extractor.getNextInCorpusLinkTarget())) != null) {
		  if (links.containsKey(link)) {
			  writer.write(link + "\t");
			  adj_count++;
		  }
	  }
	  writer.newLine();
	  writer.write(Integer.toString(adj_count));
	  writer.newLine();
  } 
}
