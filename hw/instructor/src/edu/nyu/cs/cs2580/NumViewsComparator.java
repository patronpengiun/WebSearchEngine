package edu.nyu.cs.cs2580;

import java.util.Comparator;

public class NumViewsComparator implements Comparator<NumViews> {

	public int compare(NumViews arg0, NumViews arg1) {
		if (arg0.getNumViews() < arg1.getNumViews())
			return 1;
		else if (arg0.getNumViews() > arg1.getNumViews())
			return -1;
		else
			return 0;
	}
}