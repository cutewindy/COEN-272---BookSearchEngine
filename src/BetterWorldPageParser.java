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


public class BetterWorldPageParser implements PageParser {

    private String url;
    private Document htmlDocument;

    public BetterWorldPageParser(String url, Document htmlDocument) {
        this.url = url;
        this.htmlDocument = htmlDocument;
    }

    @Override
    public ArrayList<String> parseBookPageLinks() {
		ArrayList<String> bookPageLinks = new ArrayList<>();
		Element searchResult = this.htmlDocument.getElementById("MainContentPlaceHolder_content__ctl0_SearchResults");
		Elements names = searchResult.getElementsByClass("name");
		for (Element n : names) {
			Elements a = n.select("a[href]");
			String bookPageLink = "http://www.betterworldbooks.com/" + a.attr("href");
//			System.out.println(bookPageLink);
			bookPageLinks.add(bookPageLink);
		}
//		System.out.println(names.size());
		return bookPageLinks;    	
    }

    @Override
    public String parseNextPageLink() {
        Elements pagination = this.htmlDocument.getElementsByAttributeValue("class", "pagination");
        Elements img = pagination.get(0).getElementsByAttributeValue("src", "../../images/arrow-next.gif");
        Element a = img.get(0).parent();
        System.out.println(a.attr("href"));
        return a.attr("href");
    }

    @Override
    public boolean isBookPage() {
        // TODO Auto-generated method stub
        return true;
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
        Elements infoText = htmlDocument.getElementsByClass("info-text");
        Elements h2 = infoText.get(0).getElementsByTag("h2");
        if (h2.size() > 1) {
            throw new RuntimeException("Found more than one title!\n" + h2.toString());
        }
        return h2.get(0).text();
    }

    // required
    private Double parsePrice() {
        Element newPriceBox = htmlDocument.getElementById("MainContentPlaceHolder_SideBar_RightRailItemPriceDisplay_NewPriceBox");
        Elements strong = newPriceBox.getElementsByTag("strong");
        if (strong.size() > 1) {
            throw new RuntimeException("Found more than one offer-price!\n" + strong.toString());
        }
        StringBuilder price = new StringBuilder(strong.get(0).text());
        if (price.charAt(0) == '$') {
            price.deleteCharAt(0);
        }
        return Double.parseDouble(price.toString());
    }

    private String parseAuthor() {
        Elements infoText = htmlDocument.getElementsByClass("info-text");
        if (infoText.size() == 0) {
            return "N/A";
        }
        Elements a = infoText.get(0).getElementsByTag("a");
        if (a.size() == 0) {
            return "N/A";
        }
//		if (a.size() > 1) {
//			throw new RuntimeException("Found more than one author!\n" + a.toString());
//		}
        return a.get(0).text();
    }

    private String parseISBN10() {
        Element bookDetails = htmlDocument.getElementById("MainContentPlaceHolder_BookInfo_BookDetails_attributes");
        if (bookDetails == null) {
            return "N/A";
        }
        Pattern p = Pattern.compile("\\s*ISBN-10:\\s*([0-9-]+)\\s*");
        Matcher m = p.matcher(bookDetails.text());
        if (m.find()) {
            return m.group(1);
        }
        else {
            return "N/A";
        }
    }

    private String parseISBN13() {
        Element bookDetails = htmlDocument.getElementById("MainContentPlaceHolder_BookInfo_BookDetails_attributes");
        if (bookDetails == null) {
            return "N/A";
        }
        Pattern p = Pattern.compile("\\s*ISBN-13:\\s*([0-9-]+)\\s*");
        Matcher m = p.matcher(bookDetails.text());
        if (m.find()) {
            return m.group(1);
        }
        else {
            return "N/A";
        }
    }

    private String parseGenre() {
        return Crawler.category;
    }

    private String parseDescription() {
        Element description = htmlDocument.getElementById("MainContentPlaceHolder_BookInfo_BookDescription_attributes");
        if (description == null) {
            return "N/A";
        }
        return description.text();
    }

    private String parseStar() {
        // Cannot find average star
        return "N/A";
    }

    private String parseImgUrl() {
        Element expandImage = htmlDocument.getElementById("expand-image");
        if (expandImage == null) {
            return "N/A";
        }
        Elements imgs = expandImage.getElementsByTag("img");
        if (imgs.size() == 0) {
            return "N/A";
        }
        if (imgs.size() > 1) {
            throw new RuntimeException("Found more than one imgUrls!\n" + imgs.toString());
        }
        return imgs.get(0).attr("src");
    }

    @Override
    public void saveBookPageInfo(int bookId, Map<String, Object> bookPageInfo, String filename) {
        // sample output:
        // {"index":{"_id":"<bookId>"}}
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
//        out.println(bookPageInfo.toString());
//        out.println(bookPageInfo.toString(4)); // DEBUG: pretty print json

        try {
            pw.close();
            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public static void main(String[] args) {
        System.out.println("DEBUG========BetterWorldBooks");
//        String url = "http://www.betterworldbooks.com/the-hunger-games-trilogy-id-9780545265355.aspx";
//        String url = "http://www.betterworldbooks.com/wwe-encyclopedia-second-edition-id-9780756691592.aspx";
        String url = "http://www.betterworldbooks.com/computer-science-books-H833.aspx?dsNav=N:4294965695-3000833,Nr:AND(NOT(Condition%3aDigital)%2cNOT(Format%3aeBook))&=";
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


            BetterWorldPageParser betterWorldBooks = new BetterWorldPageParser(url, htmlDocument);

//            System.out.println("\nCheck book page...");
//            boolean isBook = betterWorldBooks.isBookPage();
//            System.out.println("is book page: " + isBook);
//
//            System.out.println("\nParse book page...");
//            Map<String, Object> bookPageInfo = betterWorldBooks.parseBookPageInfo();
//            System.out.println(new Gson().toJson(bookPageInfo));
//
//            System.out.println("\nSave book info to test.json...");
//            betterWorldBooks.saveBookPageInfo(1, bookPageInfo, "test.json");
            
            betterWorldBooks.parseBookPageLinks();
            betterWorldBooks.parseNextPageLink();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}

