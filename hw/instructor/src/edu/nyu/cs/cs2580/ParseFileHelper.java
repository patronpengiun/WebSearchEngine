package edu.nyu.cs.cs2580;

import java.io.File;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class ParseFileHelper {
	
	Document doc = null;
	String docTitle;
	String docBody;
	
	public String getDocTitle() {
		return docTitle;
	}

	public String getDocBody() {
		return docBody;
	}

	public Document getDoc() {
		return doc;
	}

	public String parseHtmlText(File file) throws IOException {
		System.out.println("absolutepath: " +file.getAbsolutePath());
		this.doc = Jsoup.parse(file,null);
		
		docTitle = doc.title().toLowerCase();
		docBody = doc.body().text().toLowerCase();
		return docTitle + " " + docBody;
	}
}
