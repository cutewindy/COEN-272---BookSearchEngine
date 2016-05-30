import java.util.*;

import org.jsoup.Connection.Response;


public class Crawler {
	public static final int MAX_PAGES_TO_SEARCH = 1000;
	public static final int MAX_BOOKS_TO_SEARCH = 100;
	public static int pageId = 1;
	public static int bookId = 1;

	public static final String SITE = "amazon";
//	public static final String SEED = "http://www.amazon.com/s/ref=nb_sb_noss?url=search-alias%3Daps&field-keywords=books";
	public static final String SEED = "http://www.amazon.com/s/ref=sr_nr_n_0?fst=as%3Aoff&rh=n%3A283155%2Cp_n_availability%3A2245266011%2Cp_n_fresh_match%3A1-2%2Cn%3A%211000%2Cn%3A1%2Cn%3A173508%2Cn%3A266162%2Cn%3A3564986011&bbn=266162&ie=UTF8&qid=1464546269&rnid=266162";

	public static List<Request> requestQueue = new ArrayList<Request>();

	/**
	 * run crawler and save data as json
	 */
	public void run() {
//		requestQueue.add(new CategoryRequest(SEED));
		requestQueue.add(new CategoryLeafRequest(SEED));
		while (!this.requestQueue.isEmpty()
				&& this.pageId < MAX_PAGES_TO_SEARCH
				&& this.bookId < MAX_BOOKS_TO_SEARCH) {
			try {
				Request request = this.requestQueue.remove(0);

				System.out.printf("Visiting %s page: %s", request.type, request.url);
				if (request.isDuplicateUrl()) {
					System.out.println("Found duplicate url!");
					continue;
				}
				if (!request.isAllowed()) {
					System.out.println("Request is not allowed by robots.txt!");
					continue;
				}
				Response response = request.visit();
				this.pageId++;
				System.out.println("PageId: " + this.pageId);

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
