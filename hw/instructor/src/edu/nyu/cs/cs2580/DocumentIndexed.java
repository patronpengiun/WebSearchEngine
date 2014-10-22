package edu.nyu.cs.cs2580;

import java.util.HashMap;

/**
 * @CS2580: implement this class for HW2 to incorporate any additional
 * information needed for your favorite ranker.
 */
public class DocumentIndexed extends Document {
  private static final long serialVersionUID = 9184892508124423115L;
  private long _totalWords = 0;

  public long get_totalWords() {
	  return _totalWords;
  }

  public void set_totalWords(long _totalWords) {
	  this._totalWords = _totalWords;
  }

  public DocumentIndexed(int docid) {
	  super(docid);
  }
  
}
