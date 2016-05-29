import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.*;

import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.Gson;


public class AmazonBookPage implements BookPage {

	private String url;
	private Document htmlDocument;

	public AmazonBookPage(String url, Document htmlDocument) {
		this.url = url;
		this.htmlDocument = htmlDocument;
	}
	
	public boolean isBookPage() {
		Elements books = this.htmlDocument.select("body[class^=book]");
		if (books.size() > 1) {
			throw new RuntimeException("Found more than one book!\n" + books.toString());
		}
		return (books.size() == 1) ? true : false;
	}
	
	
	public Map<String, String> parseBookPageInfo() {
		Map<String, String> book = new LinkedHashMap<String, String>();

	    book.put("title", parseTitle());
		book.put("url", this.url);
	    book.put("price", parsePrice());
	    book.put("author", parseAuthor());
	    book.put("ISBN10", parseISBN10());
	    book.put("ISBN13", parseISBN13());
	    book.put("category", parseCategory());
	    book.put("description", parseDescription());
	    book.put("review", parseReview());
	    book.put("imgUrl", parseImgUrl());
	    
	    return book;
	}
	
	private String parseTitle() {
		Element title = this.htmlDocument.getElementById("productTitle");
		return title.text();
	}

	private String parsePrice() {
		Element buyNewSection = this.htmlDocument.getElementById("buyNewSection");
		Elements prices = buyNewSection.getElementsByClass("offer-price");
		if (prices.size() > 1) {
			throw new RuntimeException("Found more than one offer-price!\n" + prices.toString());
		}
		return prices.get(0).text();
	}
	
	private String parseAuthor() {
		Elements authors = this.htmlDocument.getElementsByClass("contributorNameID");
		if (authors.size() > 1) {
			throw new RuntimeException("Found more than one author!\n" + authors.toString());
		}
		return authors.get(0).text();
	}
	
	private String parseISBN10() {
		Elements ISBN10s = this.htmlDocument.select("li:contains(ISBN-10:)");
		if (ISBN10s.size() > 1) {
			throw new RuntimeException("Found more than one ISBN10!\n" + ISBN10s.toString());
		}
		Pattern p = Pattern.compile("^\\s*ISBN-10:\\s*([0-9-]+)\\s*$");
		Matcher m = p.matcher(ISBN10s.get(0).text());
		if (m.find()) {
			return m.group(1);
		}
		else {
			return "";
		}
	}

	private String parseISBN13() {
		Elements ISBN13s = this.htmlDocument.select("li:contains(ISBN-13:)");
		if (ISBN13s.size() > 1) {
			throw new RuntimeException("Found more than one ISBN13!\n" + ISBN13s.toString());
		}
		Pattern p = Pattern.compile("^\\s*ISBN-13:\\s*([0-9-]+)\\s*$");
		Matcher m = p.matcher(ISBN13s.get(0).text());
		if (m.find()) {
			return m.group(1);
		}
		else {
			return "";
		}
	}

	private String parseCategory() {
		return "";
	}
	
	private String parseDescription() {
		Element bookDescription_feature_div = this.htmlDocument.getElementById("bookDescription_feature_div");
		Elements noscripts = bookDescription_feature_div.getElementsByTag("noscript");
		if (noscripts.size() > 1) {
			throw new RuntimeException("Found more than one noscript!\n" + noscripts.toString());
		}
		return noscripts.get(0).text();
	}
	
	private String parseImgUrl() {
		Element imgUrl = this.htmlDocument.getElementById("imgBlkFront");
		// TODO: img src is base64 encoded, needs to decode it, currently using data-a-dynamic-image as a wordaround
		// System.out.println(imgUrl);
		// System.out.println(imgUrl.attr("data-a-dynamic-image"));
		JSONObject imgUrls = new JSONObject(imgUrl.attr("data-a-dynamic-image"));
		return imgUrls.keys().next().toString();
	}
	
	private String parseReview() {
		Element productDetailsTables = this.htmlDocument.getElementById("productDetailsTable");
		Elements reviews = productDetailsTables.select("span[class~=s_star_]");
		if (reviews.size() > 1) {
			throw new RuntimeException("Found more than one review!\n" + reviews.toString());
		}
		Pattern p = Pattern.compile("^\\s*([0-9.]+) out of \\d+ stars\\s*$");
		Matcher m = p.matcher(reviews.get(0).text());
		if (m.find()) {
			return m.group(1);
		}
		else {
			return "";
		}
	}
	
	public void saveBookPageInfo(int bookId, Map<String, String> bookPageInfo, String filename) throws IOException {
		// sample output:
		// {"index":{"_id":"<bookId>"}}
		JSONObject bookIndex = new JSONObject();
		bookIndex.put("index", new JSONObject().put("_id", bookId));

		FileWriter fw = new FileWriter(filename, true); // # true: append
		BufferedWriter bw = new BufferedWriter(fw);
		PrintWriter out = new PrintWriter(bw);

        out.println(bookIndex.toString());
        out.println(new Gson().toJson(bookPageInfo));
//        out.println(bookPageInfo.toString());
//        out.println(bookPageInfo.toString(4)); // DEBUG: pretty print json

        out.close();
        bw.close();
        fw.close();
	}

	
	public static void main(String[] args) throws IOException {
		System.out.println("Game on!");

		String url = "http://www.amazon.com/Oh-Places-Youll-Dr-Seuss/dp/0679805273/ref=sr_1_1?s=books&ie=UTF8&qid=1464329058&sr=1-1&keywords=book";
		String USER_AGENT =
				"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1";

		Connection.Response response = Jsoup.connect(url)
											.userAgent(USER_AGENT)
											.timeout(100000)
											.ignoreHttpErrors(true)
											.execute();
		int statusCode = response.statusCode();
		System.out.println("statusCode: " + statusCode);
		Document htmlDocument = response.parse();

		// DEBUG: parse html page
  		// PrintWriter writer= new PrintWriter("book.html");
  		// writer.println(htmlDocument.toString());

		AmazonBookPage amazonBookPage = new AmazonBookPage(url, htmlDocument);
		boolean isBookPage = amazonBookPage.isBookPage();
		System.out.println("isBook:" + isBookPage);
		Map<String, String> bookPageInfo = amazonBookPage.parseBookPageInfo();
		System.out.println(new Gson().toJson(bookPageInfo));
		amazonBookPage.saveBookPageInfo(1, bookPageInfo, "amazon.json");
		System.out.println("Saved to amazon.json");

		System.exit(0);
	}

}
