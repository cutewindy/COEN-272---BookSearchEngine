import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.json.JSONObject;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.Gson;


public class AmazonPageParser implements PageParser {

	private String url;
	private Document htmlDocument;

	public AmazonPageParser(String url, Document htmlDocument) {
		this.url = url;
		this.htmlDocument = htmlDocument;
	}
	public ArrayList<String> parseBookPageLinks() {
		ArrayList<String> bookPageLinks = new ArrayList<>();
		Element resultsCol  = this.htmlDocument.getElementById("resultsCol");
		Elements h2s = resultsCol.select("h2[data-attribute]");
		for (Element e : h2s) {
			String bookPageLink = e.parent().attr("href");
			bookPageLinks.add(bookPageLink);
//			System.out.println(e.parent().attr("title"));
//    		System.out.println(bookPageLink);
		}
//		System.out.println(bookPageLinks.size());

		return bookPageLinks;
	}

	public String parseNextPageLink() {
		String nextPageLink = null;
		Element pagnNextLink = this.htmlDocument.getElementById("pagnNextLink");
		if (pagnNextLink != null) {
			nextPageLink = pagnNextLink.attr("href"); // relative link
			nextPageLink = "http://www.amazon.com" + nextPageLink;
		}
		System.out.println(nextPageLink);
		return nextPageLink;
	}

	public boolean isBookPage() {
		Elements books = this.htmlDocument.select("body[class^=book]");
		if (books.size() > 1) {
			throw new RuntimeException("Found more than one book!\n" + books.toString());
		}
		return (books.size() == 1) ? true : false;
	}


	public Map<String, Object> parseBookPageInfo() {
		Map<String, Object> book = new LinkedHashMap<String, Object>();

	    book.put("title", parseTitle()); // required
		book.put("url", this.url); // required
	    book.put("price", parsePrice()); // required
	    book.put("author", parseAuthor());
	    book.put("ISBN10", parseISBN10());
	    book.put("ISBN13", parseISBN13());
	    book.put("genre", parseCategory());
	    book.put("description", parseDescription());
	    book.put("star", parseStar());
	    book.put("imgUrl", parseImgUrl());

	    return book;
	}

	// required
	private String parseTitle() {
		Element title = this.htmlDocument.getElementById("productTitle");
		return title.text();
	}

	// required
	private Double parsePrice() {
		Element buyNewSection = this.htmlDocument.getElementById("buyNewSection");
		Elements prices = buyNewSection.getElementsByClass("offer-price");
		if (prices.size() > 1) {
			throw new RuntimeException("Found more than one price!\n" + prices.toString());
		}
		StringBuilder price = new StringBuilder(prices.get(0).text());
		if (price.charAt(0) == '$') {
			price.deleteCharAt(0);
		}
		return Double.parseDouble(price.toString());
	}

	private String parseAuthor() {
		Elements authors = this.htmlDocument.getElementsByClass("contributorNameID");
		if (authors.size() == 0) {
			return "N/A";
		}
		if (authors.size() > 1) {
			throw new RuntimeException("Found more than one author!\n" + authors.toString());
		}
		return authors.get(0).text();
	}

	private String parseISBN10() {
		Elements ISBN10s = this.htmlDocument.select("li:contains(ISBN-10:)");
		if (ISBN10s.size() == 0) {
			return "N/A";
		}
		if (ISBN10s.size() > 1) {
			throw new RuntimeException("Found more than one ISBN10!\n" + ISBN10s.toString());
		}
		Pattern p = Pattern.compile("^\\s*ISBN-10:\\s*([0-9-]+)\\s*$");
		Matcher m = p.matcher(ISBN10s.get(0).text());
		if (m.find()) {
			return m.group(1);
		}
		else {
			return "N/A";
		}
	}

	private String parseISBN13() {
		Elements ISBN13s = this.htmlDocument.select("li:contains(ISBN-13:)");
		if (ISBN13s.size() == 0) {
			return "N/A";
		}
		if (ISBN13s.size() > 1) {
			throw new RuntimeException("Found more than one ISBN13!\n" + ISBN13s.toString());
		}
		Pattern p = Pattern.compile("^\\s*ISBN-13:\\s*([0-9-]+)\\s*$");
		Matcher m = p.matcher(ISBN13s.get(0).text());
		if (m.find()) {
			return m.group(1);
		}
		else {
			return "N/A";
		}
	}

	private String parseCategory() {
		return "N/A";
	}

	private String parseDescription() {
		Element bookDescription_feature_div = this.htmlDocument.getElementById("bookDescription_feature_div");
		if (bookDescription_feature_div == null) {
			return "N/A";
		}
		Elements noscripts = bookDescription_feature_div.getElementsByTag("noscript");
		if (noscripts.size() == 0) {
			return "N/A";
		}
		if (noscripts.size() > 1) {
			throw new RuntimeException("Found more than one noscript!\n" + noscripts.toString());
		}
		return noscripts.get(0).text();
	}

	private String parseImgUrl() {
		Element imgUrl = this.htmlDocument.getElementById("imgBlkFront");
		if (imgUrl == null) {
			return "N/A";
		}
		// TODO: img src is base64 encoded, needs to decode it, currently using data-a-dynamic-image as a wordaround
		// System.out.println(imgUrl);
		// System.out.println(imgUrl.attr("data-a-dynamic-image"));
		JSONObject imgUrls = new JSONObject(imgUrl.attr("data-a-dynamic-image"));
		return imgUrls.keys().next().toString();
	}

	private String parseStar() {
		Element productDetailsTables = this.htmlDocument.getElementById("productDetailsTable");
		if (productDetailsTables == null) {
			return "N/A";
		}
		Elements reviews = productDetailsTables.select("span[class~=s_star_]");
		if (reviews.size() == 0) {
			return "N/A";
		}
		if (reviews.size() > 1) {
			throw new RuntimeException("Found more than one review!\n" + reviews.toString());
		}
		Pattern p = Pattern.compile("^\\s*([0-9.]+) out of \\d+ stars\\s*$");
		Matcher m = p.matcher(reviews.get(0).text());
		if (m.find()) {
			return m.group(1);
		}
		else {
			return "N/A";
		}
	}

	public void saveBookPageInfo(int bookId, Map<String, Object> bookPageInfo, String filename) {
		// sample output:
		// {"index":{"_id":"<bookId>"}}
		JSONObject bookIndex = new JSONObject();
		bookIndex.put("index", new JSONObject().put("_id", bookId));

		FileWriter fw = null;
		try {
			fw = new FileWriter(filename, true); // # true: append
		} catch (IOException e) {
			e.printStackTrace();
		}
		BufferedWriter bw = new BufferedWriter(fw);
		PrintWriter out = new PrintWriter(bw);

        out.println(bookIndex.toString());
        out.println(new Gson().toJson(bookPageInfo));
//        out.println(bookPageInfo.toString());
//        out.println(bookPageInfo.toString(4)); // DEBUG: pretty print json

		try {
			out.close();
			bw.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public static void main(String[] args) {
		System.out.println("Game on!");

		// book page
//		String url = "http://www.amazon.com/Oh-Places-Youll-Dr-Seuss/dp/0679805273/ref=sr_1_1?s=books&ie=UTF8&qid=1464329058&sr=1-1&keywords=book";
		// category leaf page
//		String url = "http://www.amazon.com/s/ref=sr_nr_n_0?fst=as%3Aoff&rh=n%3A283155%2Cp_n_availability%3A2245266011%2Cp_n_fresh_match%3A1-2%2Cn%3A%211000%2Cn%3A1%2Cn%3A173508%2Cn%3A266162%2Cn%3A3564986011&bbn=266162&ie=UTF8&qid=1464546269&rnid=266162";
//		String url = "http://www.amazon.com/s/ref=sr_pg_2?fst=as%3Aoff&rh=n%3A283155%2Cp_n_availability%3A2245266011%2Cp_n_fresh_match%3A1-2%2Cn%3A%211000%2Cn%3A1%2Cn%3A173508%2Cn%3A266162%2Cn%3A3564986011&page=2&bbn=266162&ie=UTF8&qid=1464586805";
		String url = "http://www.amazon.com/Philadelphia-Architecture-Guide-City-Fourth/dp/1589881109/ref=sr_1_19/187-3700663-0446832?s=books&ie=UTF8&qid=1464594276&sr=1-19&refinements=p_n_availability%3A2245266011%2Cp_n_fresh_match%3A1-2";
		String USER_AGENT =
				"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1";

		Response response = null;
		Document htmlDocument = null;
		try {
			response = Jsoup.connect(url)
                            .userAgent(USER_AGENT)
                            .timeout(100000)
                            .ignoreHttpErrors(true)
                            .execute();
			htmlDocument = response.parse();
		} catch (IOException e) {
			e.printStackTrace();
		}
		int statusCode = response.statusCode();
		System.out.println("statusCode: " + statusCode);

		// DEBUG: parse html page
		try {
			PrintWriter writer = new PrintWriter("book.html");
			writer.println(htmlDocument.toString());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		AmazonPageParser amazonPageParser = new AmazonPageParser(url, htmlDocument);
		// parse book page
		 boolean isBookPage = amazonPageParser.isBookPage();
		 System.out.println("isBook:" + isBookPage);
		 Map<String, Object> bookPageInfo = amazonPageParser.parseBookPageInfo();
		 System.out.println(new Gson().toJson(bookPageInfo));
		 amazonPageParser.saveBookPageInfo(1, bookPageInfo, "amazon.json");
		 System.out.println("Saved to amazon.json");

		// parse category leaf page
//		amazonPageParser.parseBookPageLinks();
//		amazonPageParser.parseNextPageLink();



		System.exit(0);
	}

}
