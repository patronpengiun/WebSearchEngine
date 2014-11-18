package edu.nyu.cs.cs2580;

import java.util.HashMap;

/**
 * @CS2580: implement this class for HW2 to incorporate any additional
 * information needed for your favorite ranker.
 */
public class DocumentIndexed extends Document {
  private static final long serialVersionUID = 9184892508124423115L;
  private long _totalWords = 0;
  private HashMap<String, Integer> _wordFreq = new HashMap<String, Integer>();
  
  
  public long get_totalWords() {
	  return _totalWords;
  }

  public void set_totalWords(long _totalWords) {
	  this._totalWords = _totalWords;
  }

  public DocumentIndexed(int docid) {
	  super(docid);
  }
  
  public HashMap<String, Integer> word_freq(){
	  return _wordFreq;
  }

//  public void word_frequency_increase(String word){
//	  if(!_wordFreq.containsKey(word)){
//		  _wordFreq.put(word, 1);
//	  } else{
//		  int tmp = _wordFreq.get(word);
//		  _wordFreq.put(word, ++tmp);
//	  }
//  }
//  
//  public int cur_word_frequence(String word){
//	  if(!_wordFreq.containsKey(word)){
//		  return 0;
//	  } else return _wordFreq.get(word);
//  }
  
  public void word_frequency_increase(String word){
	  if(!_wordFreq.containsKey(word)){
		  _wordFreq.put(word, 1);
	  } else{
		  int tmp = _wordFreq.get(word);
		  _wordFreq.put(word, ++tmp);
	  }
  }
  
  public int cur_word_frequence(String word){
	  if(!_wordFreq.containsKey(word)){
		  return 0;
	  } else return _wordFreq.get(word);
  }
  
  
}
