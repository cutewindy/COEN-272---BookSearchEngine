import java.util.*;

import org.jsoup.Connection.Response;


public class Crawler {

	
	
	// ======== input ========
	public static String category = "Literature & Fiction";
	// refer to user agent sites when site gets blocked:
	//     http://www.useragentstring.com/pages/Mozilla/
	public static final String USER_AGENT = "Mozilla/5.0 (Windows; U; Windows NT 6.1; it; rv:2.0b4) Gecko/20100818";
	
	// amazon
//	public static int bookId = 16;
//	public static final String SITE = "amazon";
//	public static final String SEED = "http://www.amazon.com/s/ref=sr_pg_35?fst=as%3Aoff&rh=n%3A283155%2Cp_n_availability%3A2245266011%2Cp_n_fresh_match%3A1-2%2Cn%3A%211000%2Cn%3A17&page=35&bbn=1000&ie=UTF8&qid=1464669565";
	
	// barnesNoble
//	public static int bookId = 15410;
//	public static final String SITE = "barnesNoble";
//	public static final String SEED = "http://www.barnesandnoble.com/b/textbooks/computers/_/N-8q9Zug4";

	// betterWorld
//	public static int bookId = 20001;
//	public static final String SITE = "betterWorld";
//	public static final String SEED = "http://www.betterworldbooks.com/computer-science-books-H833.aspx?dsNav=N:4294965695-3000833,Nr:AND(NOT(Condition%3aDigital)%2cNOT(Format%3aeBook))&=";

	// textbook
	public static int bookId = 31522;
	public static final String SITE = "textbook";
	public static final String SEED = "http://www.textbooks.com/Search.php?TYP=SBJ&dHTxt=computer&mHTxt=&CSID=AKDWKWDQKUJD2OTCD2CCQMSCB&PART=PRINT&TXT=computer";
	// =======================

	
	
	public static int pageId = 1;
	public static final int MAX_PAGES_TO_SEARCH = 2000;
	public static final int MAX_BOOKS_TO_SEARCH = 1000 + bookId;
	public static List<Request> requestQueue = new ArrayList<Request>();

	/**
	 * run crawler and save data as json
	 */
	public void run() {
//		requestQueue.add(new CategoryRequest(SEED));
		requestQueue.add(new CategoryLeafRequest(SEED));
		while (!this.requestQueue.isEmpty()
				&& this.pageId <= MAX_PAGES_TO_SEARCH
				&& this.bookId <= MAX_BOOKS_TO_SEARCH) {
			try {
				Request request = this.requestQueue.remove(0);

				System.out.printf("Visiting %s page: %s\n", request.type, request.url);
				if (SITE.equals("amazon") && request.url.contains("ebook")) {
					System.out.println("Skip ebook request!");
					continue;
				}
				if (request.isDuplicateUrl()) {
					System.out.println("Found duplicate u vrl!");
					continue;
				}
				if (!request.isAllowed()) {
					System.out.println("Request is not allowed by robots.txt!");
					continue;
				}
				Response response = request.visit();
				System.out.println("PageId: " + this.pageId);
				this.pageId++;

				if (response.statusCode() != 200) {
					System.out.println("Request not successful! status code: " + String.valueOf(response.statusCode()));
					continue;
				}
				if (!response.contentType().contains("text/html")) {
					System.out.println("Retrieved something other than HTML!");
					continue;
				}
				if (request.isDuplicatePage(response)) {
					System.out.println("Found duplicate page!");
					continue;
				}
				request.parse(response);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		Crawler crawler = new Crawler();
		crawler.run();
	}

}
