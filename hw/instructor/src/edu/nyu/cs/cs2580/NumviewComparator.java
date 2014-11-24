package edu.nyu.cs.cs2580;

import java.util.Comparator;
import java.util.Map;

class NumviewComparator implements Comparator<String> {

    Map<String, Integer> base;
    public NumviewComparator(Map<String, Integer> numviewMap) {
        this.base = numviewMap;
    }

    // Note: this comparator imposes orderings that are inconsistent with equals.    
    public int compare(String a, String b) {
        if (base.get(a) > base.get(b)) {
            return -1;
        } else if (base.get(a) < base.get(b)) {
            return 1;
        } else {
        	if (a.compareTo(b) < 0)
        		return -1;
        	else 
        		return 1;
        }// returning 0 would merge keys
    }
}