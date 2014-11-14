package edu.nyu.cs.cs2580;

public class PageRank {

	int docid;
	double pageRank;

	public PageRank (int docid, double pageRank) {
		this.docid = docid;
		this.pageRank = pageRank;
	}

	public int getDocid() {
		return docid;
	}

	public double getPageRank() {
		return pageRank;
	}

}