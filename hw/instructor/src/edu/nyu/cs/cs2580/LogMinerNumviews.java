package edu.nyu.cs.cs2580;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Scanner;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW3.
 */
public class LogMinerNumviews extends LogMiner {

  public LogMinerNumviews(Options options) {
    super(options);
  }

  /**
   * This function processes the logs within the log directory as specified by
   * the {@link _options}. The logs are obtained from Wikipedia dumps and have
   * the following format per line: [language]<space>[article]<space>[#views].
   * Those view information are to be extracted for documents in our corpus and
   * stored somewhere to be used during indexing.
   *
   * Note that the log contains view information for all articles in Wikipedia
   * and it is necessary to locate the information about articles within our
   * corpus.
   *
   * @throws IOException
   */
  @Override
  public void compute() throws IOException {
    System.out.println("Computing using " + this.getClass().getName());
    
    HashMap<String,Integer> numViews = new HashMap<String,Integer>();
	File corpusDir = new File(_options._corpusPrefix);
	File[] docs = corpusDir.listFiles();
	
	// add all the (filenames) to the hash map
	for (File doc: docs) {
		if (CorpusAnalyzer.isValidDocument(doc)) {
			numViews.put(CorpusAnalyzer.convertToUTF8(doc.getName()),0);
		}
	}
	
	String logFile = _options._logPrefix + "/20140601-160000.log";
	BufferedReader reader = new BufferedReader(new FileReader(logFile));
	String line = null;
	while ((line = reader.readLine()) != null) {
		Scanner s = new Scanner(line);
		s.next(); // skip the first token
		try {
			String url = java.net.URLDecoder.decode(s.next(),"UTF-8");
			int num = Integer.parseInt(s.next());
			if (numViews.containsKey(url)) {
				numViews.put(url, num);
			}
		}
		catch (Exception e) {}
		s.close();
	}
	reader.close();
	
	String numViewsFile = _options._indexPrefix + "/numViews.idx";
	ObjectOutputStream writer =
			new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(numViewsFile)));
	writer.writeObject(numViews);
	writer.close();
  }

  /**
   * During indexing mode, this function loads the NumViews values computed
   * during mining mode to be used by the indexer.
   * 
   * @throws IOException
   */
  @Override
  public Object load() throws IOException {
    System.out.println("Loading using " + this.getClass().getName());
    
    String numViewsFile = _options._indexPrefix + "/numViews.idx";
	ObjectInputStream reader =
			new ObjectInputStream(new BufferedInputStream(new FileInputStream(numViewsFile)));
	Object ret = null;
	try {
		ret = reader.readObject();
	}
	catch (Exception e) {}
	reader.close();
	return ret;
  }
}
