package edu.nyu.cs.cs2580;

import java.io.IOException;

public class Bhattacharyya {
	public static void main(String[] args) {
		BhattacharyyaHelper helper = new BhattacharyyaHelper(args[0], args[1]);
		try {
			helper.generateOutput();
		}
		catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}
}
