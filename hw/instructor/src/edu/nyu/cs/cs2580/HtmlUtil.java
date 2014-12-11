package edu.nyu.cs.cs2580;

import java.net.URLEncoder;

public class HtmlUtil {
	public static String generateSpellCorrection(String corrected) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div class='correction'><span>Did you mean: </span><a href='/search?query=");
		sb.append(URLEncoder.encode(corrected).replace("+", "%20"));
		sb.append("&ranker=comprehensive&format=html'><em>" + corrected + "</em></a></div>");
		sb.append("<hr color='#e5e5e5' size='1' noshade>");
		return sb.toString();
	}
}
