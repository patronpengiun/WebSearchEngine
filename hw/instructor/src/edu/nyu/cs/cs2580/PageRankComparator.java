package edu.nyu.cs.cs2580;

import java.util.Comparator;

public class PageRankComparator implements Comparator<PageRank> {

	public int compare(PageRank arg0, PageRank arg1) {
		if (arg0.getPageRank() < arg1.pageRank)
			return 1;
		else if (arg0.getPageRank() > arg1.getPageRank())
			return -1;
		else
			return 0;
	}

}