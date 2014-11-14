package edu.nyu.cs.cs2580;

public class NumViews {

	int docid;
	String docName;
	int numViews;

	public NumViews(int docid, String docName, int numViews) {
		this.docid = docid;
		this.docName = docName;
		this.numViews = numViews;
	}
	
	public int getDocid() {
		return docid;
	}

	public int getNumViews() {
		return numViews;
	}

	public String getDocName() {
		return docName;
	}

}