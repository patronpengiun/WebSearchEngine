package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.Vector;
import java.util.HashMap;
import java.util.Scanner;

class Evaluator {

  public static void main(String[] args) throws IOException {
    HashMap < String , HashMap < Integer , Double > > relevance_judgments =
      new HashMap < String , HashMap < Integer , Double > >();
    if (args.length < 1){
      System.out.println("need to provide relevance_judgments");
      return;
    }
    String p = args[0];
    // first read the relevance judgments into the HashMap
    readRelevanceJudgments(p,relevance_judgments);
    // now evaluate the results from stdin
    evaluateStdin(relevance_judgments);
  }

  public static void readRelevanceJudgments(
    String p,HashMap < String , HashMap < Integer , Double > > relevance_judgments){
    try {
      BufferedReader reader = new BufferedReader(new FileReader(p));
      try {
        String line = null;
        while ((line = reader.readLine()) != null){
          // parse the query,did,relevance line
          Scanner s = new Scanner(line).useDelimiter("\t");
          String query = s.next();
          int did = Integer.parseInt(s.next());
          String grade = s.next();
          double rel = 0.0;
          // convert to binary relevance
          if ((grade.equals("Perfect")) ||
            (grade.equals("Excellent")) ||
            (grade.equals("Good"))){
            rel = 1.0;
          }
          if (relevance_judgments.containsKey(query) == false){
            HashMap < Integer , Double > qr = new HashMap < Integer , Double >();
            relevance_judgments.put(query,qr);
          }
          HashMap < Integer , Double > qr = relevance_judgments.get(query);
          qr.put(did,rel);
        }
      } finally {
        reader.close();
      }
    } catch (IOException ioe){
      System.err.println("Oops " + ioe.getMessage());
    }
  }

  public static void evaluateStdin(
    HashMap < String , HashMap < Integer , Double > > relevance_judgments){
    // only consider one query per call    
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
      
      String line = null;
      double RR = 0.0;			// number of current relevent ranks
      double R = -1;			// number of total relevent ranks
      double N = 0.0;			// number of ranks
      
      double p1 = 0.0;			// precision@1
      double p5 = 0.0;			// precision@5
      double p10 = 0.0;			// precision@10
      
      double r1 = 0.0;			// recall@1
      double r5 = 0.0;			// recall@5
      double r10 = 0.0;			// recall@10
      
      
      
      while ((line = reader.readLine()) != null){
        Scanner s = new Scanner(line).useDelimiter("\t");
        String query = s.next();
        
        if (R < 0) {		// check if R is calculated
        	for (double value: relevance_judgments.get(query).values()) {
            	  if (1.0 == value) R++;
              }
        }       
        
        int did = Integer.parseInt(s.next());
      	String title = s.next();
      	double rel = Double.parseDouble(s.next());
      	if (relevance_judgments.containsKey(query) == false){
      	  throw new IOException("query not found");
      	}
      	HashMap < Integer , Double > qr = relevance_judgments.get(query);
      	if (qr.containsKey(did) != false){
      	  RR += qr.get(did);					
      	}
      	++N;
      	
      	if (N == 1) {
      		p1 = RR / N;
      		r1 = RR / R;
      	} else if (N == 5) {
      		p5 = RR / N;
      		r5 = RR / R;
      	} else if (N == 10) {
      		p10 = RR / N;
      		r10 = RR / R;
      	}
      }
      
      // this is the output of the evaluation results
      System.out.println("Precision@1: " + p1 + "\n" + "Precision@5: " + p5 + "\n" + "Precision@10: " + p10);
      System.out.println("Recall@1: " + r1 + "\n" + "Recall@5: " + r5 + "\n" + "Recall@10: " + r10); 
    } catch (Exception e){
      System.err.println("Error:" + e.getMessage());
    }
  }
}
