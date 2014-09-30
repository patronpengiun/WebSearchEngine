package edu.nyu.cs.cs2580;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Vector;
import java.util.Scanner;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

// @CS2580: This is a simple implementation that you will be changing
// in homework 2.  For this homework, don't worry about how this is done.
class Document {
  public int _docid;

  private static HashMap < String , Integer > _dictionary = new HashMap < String , Integer >();
  private static Vector < String > _rdictionary = new Vector < String >();
  private static HashMap < Integer , Integer > _df = new HashMap < Integer , Integer >();
  private static HashMap < Integer , Integer > _tf = new HashMap < Integer , Integer >();
  private static int _total_tf = 0;
  
  private Vector < Integer > _body;
  private Vector < Integer > _title;
  private String _titleString;
  private int _numviews;
  private HashMap<Integer,Integer> document_tf_map = new HashMap<Integer,Integer>(); 
  private HashMap<Integer,LinkedList<Integer>> term_index_map = new HashMap<Integer,LinkedList<Integer>>();
  private int _wordCount = 0;
  
  public static int documentFrequency(String s){
    return _dictionary.containsKey(s) ? _df.get(_dictionary.get(s)) : 0;
  }

  public static int termFrequency(String s){
    return _dictionary.containsKey(s) ? _tf.get(_dictionary.get(s)) : 0;
  }

  public static int termFrequency(){
    return _total_tf;
  }
  
  public Document(int did, String content){
    Scanner s = new Scanner(content).useDelimiter("\t");

    _titleString = s.next();
    _title = new Vector < Integer >();
    _body = new Vector < Integer >();

    readTermVector(_titleString, _title);
    readTermVector(s.next(), _body);
    
    _wordCount = _body.size();
    
    HashSet < Integer > unique_terms = new HashSet < Integer >();
    for (int i = 0; i < _title.size(); ++i){
      int idx = _title.get(i);
      unique_terms.add(idx);
      int old_tf = _tf.get(idx);
      _tf.put(idx, old_tf + 1);
      _total_tf++;
      
      if (document_tf_map.containsKey(idx)) {
    	  document_tf_map.put(idx,document_tf_map.get(idx)+1);
      } else {
    	  document_tf_map.put(idx,1);
      }
    }
    for (int i = 0; i < _body.size(); ++i){
      int idx = _body.get(i);
      unique_terms.add(idx);
      int old_tf = _tf.get(idx);
      _tf.put(idx, old_tf + 1);
      _total_tf++;
      
      if (document_tf_map.containsKey(idx)) {
    	  document_tf_map.put(idx,document_tf_map.get(idx)+1);
      } else {
    	  document_tf_map.put(idx,1);
      }
      
      if (term_index_map.containsKey(idx)) {
    	  term_index_map.get(idx).add(i);
      } else {
    	  LinkedList<Integer> list = new LinkedList<Integer>();
    	  list.add(i);
    	  term_index_map.put(idx,list);
      }
    }
    for (Integer idx : unique_terms){
      if (_df.containsKey(idx)){
        int old_df = _df.get(idx);
        _df.put(idx,old_df + 1);
      }
    }
    _numviews = Integer.parseInt(s.next());
    _docid = did;
  }
  
  public String get_title_string(){
    return _titleString;
  }

  public int get_numviews(){
    return _numviews;
  }

  public Vector < String > get_title_vector(){
    return getTermVector(_title);
  }

  public Vector < String > get_body_vector(){
    return getTermVector(_body);
  }

  private Vector < String > getTermVector(Vector < Integer > tv){
    Vector < String > retval = new Vector < String >();
    for (int idx : tv){
      retval.add(_rdictionary.get(idx));
    }
    return retval;
  }

  private void readTermVector(String raw,Vector < Integer > tv){
    Scanner s = new Scanner(raw);
    while (s.hasNext()){
      String term = s.next();
      int idx = -1;
      if (_dictionary.containsKey(term)){
        idx = _dictionary.get(term);
      } else {
        idx = _rdictionary.size();
        _rdictionary.add(term);
        _dictionary.put(term, idx);
        _tf.put(idx,0);
        _df.put(idx,0);
      }
      tv.add(idx);
    }
    return;
  }
    
  public HashMap<Integer,Double> getNormSpaceVector(int doc_count) {
	  HashMap<Integer,Double> result = new HashMap<Integer,Double>();
	  
	  for (int idx: _body) {
		  result.put(idx, (Math.log(document_tf_map.get(idx)) / Math.log(2) + 1) * (Math.log(1.0 * doc_count / _df.get(idx)) / Math.log(2)));
	  }
	  
	  VectorHelper.normalize(result);
	  return result;
  }
  
  public double getPhraseScore(Vector<String> qv) {
	  double score = 0;
	  if (1 == qv.size()) {
		  score = getUnigramCount(qv.get(0));
	  } else {
		  for (int i=0;i<qv.size()-1;i++) {
			  score += getBigramCount(qv.get(i),qv.get(i+1));
		  }
	  }
	  return score;
  }
  
  private double getUnigramCount(String target) {
	  Integer idx = _dictionary.get(target);
	  if (idx != null) {
		  Integer count = document_tf_map.get(idx);
		  if (null == count)
			  return 0;
		  else
			  return count * 1.0;
	  }
	  else 
		  return 0;
  }
  
  private double getBigramCount(String first, String second) {
	  Integer idx_first = _dictionary.get(first);
	  Integer idx_second = _dictionary.get(second);
	  
	  if (null == idx_first || null == idx_second || null == document_tf_map.get(idx_first) || null == document_tf_map.get(idx_second))
		  return 0;
	  
	  LinkedList<Integer> list_first = term_index_map.get(idx_first);
	  LinkedList<Integer> list_second = term_index_map.get(idx_second);
	  
	  if (null == list_first || null == list_second)
		  return 0;
	  
	  double count = 0;
	  int index_first = -1;
	  int index_second = 0;
	  Iterator<Integer> iter_first = list_first.iterator();
	  Iterator<Integer> iter_second = list_second.iterator();
	  
	  while (iter_first.hasNext() && iter_second.hasNext()) {
		  if (index_first >= index_second) {
			  index_second = iter_second.next();
		  } else {
			  index_first = iter_first.next();
		  }
		  
		  if (1 == index_second - index_first) {
			  count = count + 1;
		  }
	  }
	  
	  while (iter_first.hasNext()) {
		  if (index_first >= index_second)
			  break;
		  index_first = iter_first.next();
		  if (1 == index_second - index_first) {
			  count = count + 1;
		  }
	  }
	  
	  while (iter_second.hasNext()) {
		  if (index_second - index_first > 1)
			  break;
		  index_second = iter_second.next();
		  if (1 == index_second - index_first) {
			  count = count + 1;
		  }
	  }
	  
	  return count;
  }

  public double getQueryLikelihood(Vector<String> query, double lambda) {
	  double result = 0;
	  for (String q: query) {
		  Integer seen_factor = document_tf_map.get(_dictionary.get(q));
		  double prob = (1-lambda) * (seen_factor == null ? 0 : seen_factor) / _wordCount + lambda * termFrequency(q) / _total_tf;
		  result += Math.log(prob) / Math.log(2);
	  }
	  return result;
  }
  
  public static HashMap<Integer,Double> getQueryNormSpaceVector(Vector<String> qv, int doc_count) {
	  HashMap<Integer,Double> result = new HashMap<Integer,Double>();
	  
	  HashMap<String,Integer> tf_map = new HashMap<String,Integer>();
	  for (String s: qv) {
		  if (tf_map.containsKey(s)) {
			  tf_map.put(s, tf_map.get(s) + 1);
		  } else {
			  tf_map.put(s, 1);
		  }
	  }
	  
	  for (String s : tf_map.keySet()) {
		  Integer idx = _dictionary.get(s);
		  if (idx != null) {
			  result.put(idx, (Math.log(tf_map.get(s)) / Math.log(2) + 1) * (Math.log(1.0 * doc_count / _df.get(idx)) / Math.log(2)));
		  }
	  }
	  
	  VectorHelper.normalize(result);
	  return result;
  }
}
