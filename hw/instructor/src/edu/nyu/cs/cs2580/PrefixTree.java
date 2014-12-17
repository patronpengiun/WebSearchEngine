package edu.nyu.cs.cs2580;

import java.io.Serializable;
import java.util.*;

public class PrefixTree implements Serializable{
	private static final long serialVersionUID = -8568926562981497095L;
	private Node root;
	private HashSet<String> stopWords;
	
	public PrefixTree() {
		root = new Node('*');
		String[] rawStopWords = new String[] {
				"i", "a", "about", "an", "are", "as", "at", "be", "by",  
				"for", "from", "how", "in", "is", "it", "of", "on", "or", 
				"that", "the", "this", "to", "was", "what", "when", "where",
				"who", "will", "with", "and", "[edit]", "^"
			};
		stopWords = new HashSet<String>(Arrays.asList(rawStopWords));
	}
	
	public void add(String word, int weight) {
		addHelper(word,weight,0,root);
	}
	
	private void addHelper(String word, int weight, int index, Node node) {
		if (index == word.length()) {
			node.weight = weight;
			return;
		} 
		
		if (!node.children.containsKey(word.charAt(index))) {
			node.children.put(word.charAt(index),new Node(word.charAt(index)));
		}
		addHelper(word,weight,index+1,node.children.get(word.charAt(index)));
	}
	
	public void update(String word) {
		updateHelper(word,0,root);
	}
	
	private void updateHelper(String word, int index, Node node) {
		if (index == word.length()) {
			if (node.weight == null)
				node.weight = 10;
			else
				node.weight += 10;
			return;
		}
		
		if (!node.children.containsKey(word.charAt(index))) {
			node.children.put(word.charAt(index),new Node(word.charAt(index)));
		}
		updateHelper(word,index+1,node.children.get(word.charAt(index)));
	}
	
	public List<String> searchPrefix(String prefix, int limit) {
		LinkedList<String> result = new LinkedList<String>();
		PriorityQueue<WeightedString> q = new PriorityQueue<WeightedString>();
		StringBuilder sb = new StringBuilder();
		
		if (prefix.length() == 0)
			return result;
		
		Node worker = root;
		for (int i=0;i<prefix.length();i++) {
			worker = worker.children.get(prefix.charAt(i));
			if (worker == null)
				return result;
		}
		
		sb.append(prefix);
		searchHelper(limit,q,worker,sb);
		while (!q.isEmpty()) {
			result.addFirst(q.poll().str);
		}
		return result;
	}
	
	private void searchHelper(int limit, PriorityQueue<WeightedString> q, Node node, StringBuilder sb) {
		if (node.weight != null) {
			String temp = sb.toString();
			if (isValidLookup(temp))
				q.add(new WeightedString(temp,node.weight));
			if (q.size() > limit)
				q.poll();
		}
		for (Node n: node.children.values()) {
			sb.append(n.val);
			searchHelper(limit,q,n,sb);
			sb.setLength(sb.length() - 1);
		}
	}
	
	private boolean isValidLookup(String str) {
		String[] temp = str.split(" ");
		
		if (str.contains("\"") || str.contains("^") || str.contains("-") || str.contains(")") || str.contains("("))
			return false;
		
		if (temp.length == 1) {
			return !stopWords.contains(temp[0]);
		} else {
			return !stopWords.contains(temp[temp.length-1]);
		}
	}
	
	class Node implements Serializable{
		private static final long serialVersionUID = -9142672891694305781L;
		char val;
		Integer weight;
		Map<Character,Node> children;
		
		Node(char val) {
			this.val = val;
			weight = null;
			children = new HashMap<Character,Node>();
		}
	}
	
	class WeightedString implements Comparable<WeightedString>, Serializable {
		private static final long serialVersionUID = 2183469023372990006L;
		String str;
		int weight;
		
		WeightedString(String str, int weight) {
			this.str = str;
			this.weight = weight;
		}
		
		@Override
		public int compareTo(WeightedString o) {
			return this.weight - o.weight;
		}
	}
}
