package edu.nyu.cs.cs2580;

import java.util.Comparator;
import java.util.Map;

public class PagerankComparator implements Comparator<String> {

    Map<String, Float> base;
    public PagerankComparator(Map<String, Float> pagerankMap) {
        this.base = pagerankMap;
    }

    // Note: this comparator imposes orderings that are inconsistent with equals.    
    public int compare(String a, String b) {
        if (base.get(a) >= base.get(b)) {
            return -1;
        } else {
            return 1;
        } // returning 0 would merge keys
    }
}