package edu.nyu.cs.cs2580;

import edu.nyu.cs.cs2580.SearchEngine.Options;

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


public class CorpusAnalyzerNumViews extends CorpusAnalyzer {
	public CorpusAnalyzerNumViews(Options options) {
		super(options);
	}
	
	@Override
	public void prepare() {
		return;
	}
	
	@Override
	public void compute() throws IOException {
		HashMap<String,Integer> numViews = new HashMap<String,Integer>();
		File corpusDir = new File(_options._corpusPrefix);
		File[] docs = corpusDir.listFiles();
		
		// add all the documents to the hash map
		for (File doc: docs) {
			if (isValidDocument(doc)) {
				numViews.put(doc.getName(),0);
			}
		}
		
		String logFile = _options._logPrefix + "/20140601-160000.log";
		BufferedReader reader = new BufferedReader(new FileReader(logFile));
		String line = null;
		while ((line = reader.readLine()) != null) {
			Scanner s = new Scanner(line);
			s.next(); // skip the first token
			String url = java.net.URLDecoder.decode(s.next(),"UTF-8");
			int num = Integer.parseInt(s.next());
			if (numViews.containsKey(url)) {
				numViews.put(url, num);
			}
			s.close();
		}
		reader.close();
		
		String numViewsFile = _options._indexPrefix + "/numViews.idx";
		ObjectOutputStream writer =
				new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(numViewsFile)));
		writer.writeObject(numViews);
		writer.close();
	}
	
	@Override
	public Object load() throws IOException{
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
