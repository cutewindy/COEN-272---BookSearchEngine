import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.Gson;


public class TextbookPageParser implements PageParser{

	private String url;
	private Document htmlDocument;

	public TextbookPageParser(String url, Document htmlDocument) {
		this.url = url;
		this.htmlDocument = htmlDocument;
	}


	@Override
	public ArrayList<String> parseBookPageLinks() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String parseNextPageLink() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isBookPage() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Map<String, Object> parseBookPageInfo() {
		// TODO Auto-generated method stub
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
		Elements prodDsktpWrapper = this.htmlDocument.getElementsByClass("prodDsktpWrapper");
		Elements name = prodDsktpWrapper.get(0).getElementsByAttributeValue("itemprop", "name");
		Elements bookEdition = prodDsktpWrapper.get(0).getElementsByAttributeValue("itemprop", "bookEdition");
		StringBuilder title = new StringBuilder();
		title.append(name.get(0).text()).append(" - ").append(bookEdition.get(0).text());
		return title.toString();
	}

	// required
	private Double parsePrice() {
		Elements prodCol2Info = this.htmlDocument.getElementsByClass("prodCol2Info");
		Elements prodTypeWrapper = prodCol2Info.get(0).getElementsByClass("prodTypeWrapper");
		Elements prodPriceTop = prodTypeWrapper.get(1).getElementsByClass("prodPriceTop");
		Elements prices = prodPriceTop.get(0).getElementsByAttributeValue("itemprop", "price");
		StringBuilder price = new StringBuilder(prices.get(0).text());
		if (price.charAt(0) == '$') {
			price.deleteCharAt(0);
		}
		return Double.parseDouble(price.toString());
	}
	
	private String parseAuthor() {
		Elements prodDsktpWrapper = this.htmlDocument.getElementsByClass("prodDsktpWrapper");
		if (prodDsktpWrapper.size() == 0) {
			return "N/A";
		}
		Elements name = prodDsktpWrapper.get(0).getElementsByAttributeValue("itemprop", "name");
		if (name.size() == 0) {
			return "N/A";
		}
		return name.get(1).text();
	}
	
	private String parseISBN10() {
		Elements prodCol1Img = this.htmlDocument.getElementsByClass("prodCol1Img");
		if (prodCol1Img.size() == 0) {
			return "N/A";
		}
		Pattern p = Pattern.compile("\\s*ISBN10:\\s*([0-9-]+)\\s*");
		Matcher m = p.matcher(prodCol1Img.text());
		if (m.find()) {
			return m.group(1);
		}
		else {
			return "N/A";
		}
	}
	
	private String parseISBN13() {
		Element bkEAN = this.htmlDocument.getElementById("bkEAN");
		if (bkEAN == null) {
			return "N/A";
		}
		return bkEAN.text();
	}
	
	private String parseCategory() {
		return "N/A";
	}
	
	private String parseDescription() {
		Element prodInfo1 = this.htmlDocument.getElementById("prodInfo1");
		if (prodInfo1 == null) {
			return "N/A";
		}
		return prodInfo1.text();
	}
	
	private String parseStar() {
		// there is no average review stars
		return "N/A";
	}
	
	private String parseImgUrl() {
		Elements prodCol1Img = this.htmlDocument.getElementsByClass("prodCol1Img");
		if (prodCol1Img.size() == 0) {
			return "N/A";
		}
		Elements img = prodCol1Img.get(0).getElementsByTag("img");
		if (img.size() == 0) {
			return "N/A";
		}
		return img.get(0).attr("src");
	}
	
	@Override
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
		
		System.out.println("DEBUG========TextbookPage");
//	    String url = "http://www.textbooks.com/Multivariable-Calculus-8th-Edition/9781305266643/Lopez-robert.php?CSID=AKDWKWDQKUJD2OTCD2CCQMSCB";
//	    String url = "http://www.textbooks.com/Human-Anatomy-and-Physiology-10th-Edition/9780321927040/Elaine-N-Marieb.php?CSID=AKDWKWDQKUJD2OTCD2CCQMSCB";
	    String url = "http://www.textbooks.com/Human-Anatomy-and-Physiology---Text-9th-Edition/9780321743268/Elaine-N-Marieb.php?CSID=AKDWKWDQKUJD2OTCD2CCQMSCB";
		String USER_AGENT =
	            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1";
	
	    System.out.println("Visiting: " + url);
	    Connection.Response response;
	    try {
	        response = Jsoup.connect(url)
	                .userAgent(USER_AGENT)
	                .timeout(100000)
	                .ignoreHttpErrors(true)
	                .execute();
	
	
	        int statusCode = response.statusCode();
	        System.out.println("\nstatusCode: " + statusCode);
	        Document htmlDocument = response.parse();
	        // DEBUG: parse html page
	        PrintWriter writer= new PrintWriter("book.html");
	        writer.println(htmlDocument.toString());
	        writer.close();
	
	        TextbookPageParser textbook = new TextbookPageParser(url, htmlDocument);
	
	        System.out.println("\nCheck book page...");
	        boolean isBook = textbook.isBookPage();
	        System.out.println("is book page: " + isBook);
	
	        System.out.println("\nParse book page...");
	        Map<String, Object> bookPageInfo = textbook.parseBookPageInfo();
	        System.out.println(new Gson().toJson(bookPageInfo));
	
	        System.out.println("\nSave book info to textbook.json...");
	        textbook.saveBookPageInfo(1, bookPageInfo, "textbook.json");
	    }
	    catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	    }
	}
}
