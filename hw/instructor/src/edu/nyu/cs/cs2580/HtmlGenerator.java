package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Vector;

public class HtmlGenerator {
	private static int session_count = 0;
	
	public static String generateFromScoredDocuments(Vector<ScoredDocument> documents, String query) {
		int session_id = session_count++;
		
		StringBuilder builder = new StringBuilder();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader("content/header"));
			String line = null;
			while ((line = br.readLine()) != null) {
				builder.append(line);
			}
			br.close();
		}
		catch (Exception e){
			System.out.println(e.getMessage());
		}

		
		builder.append("<div class='main-content'><table>");
		
		for (ScoredDocument d: documents) {
			builder.append("<tr><td><a href='http://www.google.com' target='_blank' onclick='log(this)'");
			builder.append(" id='");
			builder.append(d._did);
			builder.append("'>");
			builder.append(d._title);
			builder.append("</a></td></tr>");
			LogService.log(Integer.toString(d._did), Integer.toString(session_id), "render", query);
		}
		
		builder.append("</table></div>");
		builder.append("<input id='session' class='hidden' value='");
		builder.append(session_id);
		builder.append("'/>");
		builder.append("<input id='query' class='hidden' value='");
		builder.append(query);
		builder.append("'/>");
		builder.append("</body></html>");
		
		return builder.toString();
	}
}
