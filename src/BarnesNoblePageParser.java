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


public class BarnesNoblePageParser implements PageParser {

    private String url;
    private Document htmlDocument;

    public BarnesNoblePageParser(String url, Document htmlDocument) {
        this.url = url;
        this.htmlDocument = htmlDocument;
    }


    @Override
    public ArrayList<String> parseBookPageLinks() {
		ArrayList<String> bookPageLinks = new ArrayList<>();
		Element searchGrid = this.htmlDocument.getElementById("searchGrid");
		Elements ps = searchGrid.getElementsByClass("product-info-title");
		for (Element p : ps) {
			Elements a = p.select("a[href]");
			String bookPageLink = "http://www.barnesandnoble.com" + a.attr("href");
//			System.out.println(bookPageLink);
			bookPageLinks.add(bookPageLink);
		}
//		System.out.println(ps.size());
		return bookPageLinks;
    }

    @Override
    public String parseNextPageLink() {
		String nextPageLink = null;
		Elements pagination = this.htmlDocument.getElementsByClass("pagination");
		Elements span = pagination.get(0).getElementsByAttributeValue("class", "next-page");
		nextPageLink = span.get(0).parent().attr("href");
		System.out.println(nextPageLink);
		return nextPageLink;
    }

    @Override
    public boolean isBookPage() {
        Elements body = this.htmlDocument.getElementsByTag("body");
        return body.attr("class").equals("pdpPage");
    }

    @Override
    public Map<String, Object> parseBookPageInfo() {
        Map<String, Object> book = new LinkedHashMap<String, Object>();

        book.put("title", parseTitle());
        book.put("url", this.url);
        book.put("price", parsePrice());
        book.put("author", parseAuthor());
        book.put("ISBN10", parseISBN10());
        book.put("ISBN13", parseISBN13());
        book.put("genre", parseGenre());
        book.put("description", parseDescription());
        book.put("star", parseStar());
        book.put("imgUrl", parseImgUrl());

        return book;
    }

    // required
    private String parseTitle() {
        Elements titles = this.htmlDocument.getElementsByAttributeValue("itemprop", "name");
        if (titles.size() > 1) {
            throw new RuntimeException("Found more than one title!\n" + titles.toString());
        }
        return titles.get(0).text();
    }

    // required
    private Double parsePrice() {
        Elements prices = this.htmlDocument.getElementsByClass("current-price");
//        if (prices.size() > 1) {
//            throw new RuntimeException("Found more than one price!\n" + prices.toString());
//        }
        StringBuilder price = new StringBuilder(prices.get(0).text());
        if (price.charAt(0) == '$') {
            price.deleteCharAt(0);
        }
        return Double.parseDouble(price.toString());
    }

    private String parseAuthor() {
        Element prodSummary = this.htmlDocument.getElementById("prodSummary");
        if (prodSummary == null) {
            return "N/A";
        }
        Elements authors = prodSummary.getElementsByAttributeValue("itemprop", "author");
        if (authors.size() == 0) {
            return "N/A";
        }
        if (authors.size() > 1) {
            throw new RuntimeException("Found more than one author!\n" + authors.toString());
        }
        return authors.get(0).text();
    }

    private String parseISBN10() {
        // Barnes & Noble Book Store has no info of ISBN10
        return "N/A";
    }

    private String parseISBN13() {
        Element additionalProductInfo = this.htmlDocument.getElementById("additionalProductInfo");
        if (additionalProductInfo == null) {
            return "N/A";
        }
        Elements dls = additionalProductInfo.getElementsByTag("dl");
        if (dls.size() == 0) {
            return "N/A";
        }
        if (dls.size() > 1) {
            throw new RuntimeException("Found more than one dl!\n" + dls.toString());
        }
        Pattern p = Pattern.compile(".*ISBN-13:\\s*([0-9-]+)\\s+");
        Matcher m = p.matcher(dls.get(0).text());

        if (m.find()) {
            return m.group(1);
        }
        else {
            return "N/A";
        }
    }

    private String parseGenre() {
        return "N/A";
    }

    private String parseDescription() {
        Element productInfoOverview = this.htmlDocument.getElementById("productInfoOverview");
        if (productInfoOverview == null) {
            return "N/A";
        }
        return productInfoOverview.text();
    }

    private String parseStar() {
        // Cannot find in the first request page
        return "N/A";
    }

    private String parseImgUrl() {
        Element pdpMainImage = this.htmlDocument.getElementById("pdpMainImage");
        if (pdpMainImage == null) {
            return "N/A";
        }
        StringBuilder imgUrl = new StringBuilder(pdpMainImage.attr("src"));
        if (imgUrl.charAt(0) == '/' && imgUrl.charAt(1) == '/') {
            imgUrl.insert(0, "http:");
        }
        return imgUrl.toString();
    }

    public void saveBookPageInfo(int bookId, Map<String, Object> bookPageInfo, String filename) {
        JSONObject bookIndex = new JSONObject();
        bookIndex.put("index", new JSONObject().put("_id", bookId));

        FileWriter fw = null; // # true: append
        try {
            fw = new FileWriter(filename, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedWriter bw = new BufferedWriter(fw);
        PrintWriter pw = new PrintWriter(bw);

        pw.println(bookIndex.toString());
        pw.println(new Gson().toJson(bookPageInfo));

        try {
            pw.close();
            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public static void main(String[] args) throws IOException {
        // TODO Auto-generated method stub
        System.out.println("DEBUG========");
//        String url = "http://www.barnesandnoble.com/w/harry-potter-and-the-cursed-child-parts-i-ii-j-k-rowling/1123463689?ean=9781338099133";
//		String url = "http://www.barnesandnoble.com/w/me-before-you-jojo-moyes/1110570195?ean=9780143124542";
//        String url = "http://www.barnesandnoble.com/w/the-fires-of-vesuvius-mary-beard/1112257427?ean=9780674045866";
        String url = "http://www.barnesandnoble.com/b/books/computers/apple/_/N-29Z8q8Zxhr";
        String USER_AGENT =
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1";

        System.out.println("Visiting: " + url);
        Connection.Response response = Jsoup.connect(url)
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


        BarnesNoblePageParser barnesNobleBookPage = new BarnesNoblePageParser(url, htmlDocument);

//        System.out.println("\nCheck book page...");
//        boolean isBook = barnesNobleBookPage.isBookPage();
//        System.out.println("is book page: " + isBook);
//
//        System.out.println("\nParse book page...");
//        Map<String, Object> bookPageInfo = barnesNobleBookPage.parseBookPageInfo();
//        System.out.println(new Gson().toJson(bookPageInfo));
//
//        System.out.println("\nSave book info to test.json...");
//        barnesNobleBookPage.saveBookPageInfo(1, bookPageInfo, "test.json");
        
		barnesNobleBookPage.parseBookPageLinks();
		barnesNobleBookPage.parseNextPageLink();

    }

}

