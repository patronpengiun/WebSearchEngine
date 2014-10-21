package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedCompressed extends Indexer implements Serializable {
  public IndexerInvertedCompressed(Options options) {
    super(options);
    System.out.println("Using Indexer: " + this.getClass().getSimpleName());
  }

  @Override
  public void constructIndex() throws IOException {
  }

  @Override
  public void loadIndex() throws IOException, ClassNotFoundException {
  }

  @Override
  public Document getDoc(int docid) {
    return null;
  }

  /**
   * In HW2, you should be using {@link DocumentIndexed}
   */
  @Override
  public Document nextDoc(Query query, int docid) {
    return null;
  }

  @Override
  public int corpusDocFrequencyByTerm(String term) {
    return 0;
  }

  @Override
  public int corpusTermFrequency(String term) {
    return 0;
  }

  /**
   * @CS2580: Implement this for bonus points.
   */
  @Override
  public int documentTermFrequency(String term, String url) {
    return 0;
  }
	 public String EliasGamaEncode(int k){
		  int d = (int) (Math.log(k)/Math.log(2));
		  int r = (int) (k - Math.pow(2, d));
		  System.out.println(d+"  "+r);
		  String dstr = getD(d) + "0";
		  if(dstr.equals("0")) return dstr;
		  String rstr = getR(r,d);
		  dstr += rstr;
		  return dstr;
	  }
	  
	  public int EliasGamaDecode(String code){
		  
		  int d = code.indexOf("0");
		  int r = 0;
		  if(d < code.length()){
			  StringBuilder rstr = new StringBuilder(code.substring(d + 1));
			  r = convertStringtoInt(rstr.toString());
		  } 
		  return (int)(Math.pow(2, d) + r);
		 	  
	  }
	  
	  public String EliasCitaEncode(int k){
		  int d = (int) (Math.log(k)/Math.log(2));
		  int r = (int) (k - Math.pow(2, d));
		  int dd = (int) (Math.log(d+1)/Math.log(2));
		  int dr = (int) (d - Math.pow(2, dd) + 1);
		  if(dr < 0) dr = 0;
		  String ddstr = getD(dd) + "0";
		  if(ddstr.equals("0")) return ddstr;
		  String rstr = getR(dr,dd);
		  String drstr = getR(r,d);
		  ddstr += rstr + drstr;
		  return ddstr;
	  }
	  
	  public int EliasCitaDecode(String code){
		  
		  int dd = code.indexOf("0");
		  int dr = 0;
		  int d = 0;
		  int r = 0;
		  if(dd < code.length()){
			  StringBuilder drstr = new StringBuilder(code.substring(dd + 1, dd + dd + 1));
			  dr = convertStringtoInt(drstr.toString());
			  d = (int)(Math.pow(2, dd) + dr - 1);
			  StringBuilder rstr = new StringBuilder(code.substring(dd + dd + 1));
			  r = convertStringtoInt(rstr.toString());
		  } 
		  return (int)(Math.pow(2, d) + r);
		 	  
	  }	  
	  
	  
	  public int convertStringtoInt(String str){
		  int i = str.length();
		  int r = 0;
		  for(int j = i - 1; j >= 0 ; j --){
			  r += Integer.parseInt(String.valueOf(str.charAt(j))) * Math.pow(2, (i - j - 1));
			}
		  return r;
	  }
	  
	  
	  
	  
	  public int findD(String dstr){
		  if(dstr == null) return -1;
		  int d = dstr.length() - 1;
		  return d;
	  }

	  public String getD(int d)
	  {
		  StringBuilder str = new StringBuilder();
		  
		  for (; d>0 ; d-- ){  
			  str.append(1);
			  }
		  return str.toString();
	  }

	  
	  public String getR(int r, int d)
	  {
		  StringBuilder str = new StringBuilder();
		  for (int k = d - 1; k>= 0 ; k-- ){  
			  str.append((int) ((r >> k) & 0x1));
			  }
		  return str.toString();
	  }
  
  
}
