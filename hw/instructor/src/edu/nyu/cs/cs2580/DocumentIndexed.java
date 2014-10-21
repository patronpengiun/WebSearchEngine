package edu.nyu.cs.cs2580;

import java.util.HashMap;

/**
 * @CS2580: implement this class for HW2 to incorporate any additional
 * information needed for your favorite ranker.
 */
public class DocumentIndexed extends Document {
  private static final long serialVersionUID = 9184892508124423115L;
  private long _totalWords = 0;
  private HashMap<String, Integer> wordFrequency = new HashMap<String, Integer>();

  public long get_totalWords() {
	  return _totalWords;
  }

  public void set_totalWords(long _totalWords) {
	  this._totalWords = _totalWords;
  }

  public HashMap<String, Integer> getWordFrequency() {
	  return wordFrequency;
  }

  public void setWordFrequency(HashMap<String, Integer> wordFrequency) {
	  this.wordFrequency = wordFrequency;
  }

  public DocumentIndexed(int docid) {
	  super(docid);
  }
  
  public void incrementWordFrequency(String word) {
	  if (wordFrequency.containsKey(word)) {
		int i = wordFrequency.get(word);
		i++;
		wordFrequency.put(word, i);
	  } else {
		wordFrequency.put(word, 1);
	  }
  }

  public int getWordFrequencyOf(String word) {
	  if (wordFrequency.containsKey(word))
		return wordFrequency.get(word);
	  else
		return 0;
  }

  public int getOccurance() {
	  return 0;
  }
}
