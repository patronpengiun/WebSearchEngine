package edu.nyu.cs.cs2580;

import java.util.*;

public class PrefixTree {
	private Node root;
	
	public PrefixTree() {
		root = new Node('*');
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
	
	public List<String> searchPrefix(String prefix, int limit) {
		
	}
	
	class Node {
		char val;
		Integer weight;
		Map<Character,Node> children;
		
		Node(char val) {
			this.val = val;
			weight = null;
			children = new HashMap<Character,Node>();
		}
	}
}
