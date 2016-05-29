import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;


public class Crawler {
	private static final String USER_AGENT = 
			"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1";
	private static final int MAX_PAGES_TO_SEARCH = 1000;
	private static final int MAX_BOOKS_TO_SEARCH = 100;
	private static final String REPO = "repository";
	private static final String REPORT = "report.html";

	private static final String SITE = "amazon";
	private static final String SEED1 = "https://www.scu.edu/";
	private static final String SEED2 = "http://www.tutorialspoint.com/java/io/bufferedreader_read_char.htm";
	private static final String SEED3 = "http://www.amazon.com/s/ref=nb_sb_noss_2?url=search-alias%3Daps&field-keywords=books";

	private List<String> pageQueue = new ArrayList<String>();
	private HashMap<String, ArrayList<ArrayList<Integer>>> visitedPages = new HashMap();
	private List<String> linksPerPage = new ArrayList<String>();
	private List<String> seeds = new ArrayList<String>();
	private int pageId= 1;
	private int bookId= 1;



	private static final String tmpString = "123";

	
	
	/**
	 * run crawler and save data as json
	 */
	public void run() {
//		createDir(REPO);
		createFile(REPORT);
		addSeeds();
		crawler();
	}
	
	
	
	/**
	 * create a new directory to save data, if the directory exited, overwrite it
	 * @param dirName
	 */
//	private void createDir(String dirName) {
//		try {
//			FileWriter dir = new FileWriter(dirName, false);
//			dir.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
	
	
	
	/**
	 * create a new file, which can overwrite the old file
	 * @param String fileName
	 */
	private void createFile(String fileName) {
		try {
			FileWriter fileWriter = new FileWriter(fileName, false); // false-overwrite; true-append
			fileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	
	/**
	 * Before crawl web sites, add seeds to pageQueue.
	 */
	private void addSeeds() {
//		seeds.add(SEED1);
//		seeds.add(SEED2);
		seeds.add(SEED3);
		for (int i = 0; i < seeds.size(); i++) {
			pageQueue.add(seeds.get(i));
		}
	}
	
	
	
	/**
	 * crawl all the pages in the pageQueue
	 * @param url
	 */
	private void crawler() {
		while (!this.pageQueue.isEmpty()
				&& this.pageId <= MAX_PAGES_TO_SEARCH
				&& this.bookId <= MAX_BOOKS_TO_SEARCH) {
			String currentUrl = this.pageQueue.remove(0);
			try {
				System.out.println("\nPageId: " + this.pageId);
				System.out.println("Crawling page: " + currentUrl);
				crawlPage(currentUrl);
				pageId++;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	
	
	/**
	 * crawl the specified web page by the given url
	 * @param url
	 */
	private void crawlPage(String url) {
		if (!isAllowed(url)) {
			return;
		}
		try {
			Connection.Response response = Jsoup.connect(url)
					.userAgent(USER_AGENT)
					.timeout(100000)
					.ignoreHttpErrors(true)
					.execute();
			int statusCode = response.statusCode();
			int numOfLinks = -1;
			int numOfImages = -1;
			if (statusCode == 200) {
				if (!response.contentType().contains("text/html")) {
					System.out.println("**Failure**\nRetrieved something other than HTML");
					return;
				}
				Document htmlDocument = response.parse();

				String title = htmlDocument.title();
				System.out.println("Title: " + title);
				
				Elements linksOnPage = htmlDocument.select("a[href]");
				numOfLinks = linksOnPage.size();
				for (Element link: linksOnPage) {
					String crawledUrl = link.absUrl("href");
					if (!crawledUrl.isEmpty()) {
						this.pageQueue.add(crawledUrl);
					}
				}
				
				Elements imagesOnPage = htmlDocument.select("img");
				numOfImages = imagesOnPage.size();
				
				int bodySize = response.bodyAsBytes().length;
				
				ArrayList<Integer> pageInfo = new ArrayList<Integer>(Arrays.asList(numOfLinks, numOfImages, bodySize));		
				if (!isDuplicate(title, pageInfo)) { // if not duplicate, save pageInfo into visitedPages
					if (!visitedPages.containsKey(title)) {
						visitedPages.put(title, new ArrayList<ArrayList<Integer>>());
					}
					visitedPages.get(title).add(pageInfo);
					report(pageId, title, url, statusCode, numOfLinks, numOfImages);
					
					// TODO save as json
					BookPage bookPage = BookPageFactory.getBookPage(SITE, url, htmlDocument);
					boolean isBookPage = bookPage.isBookPage();
					System.out.println("isBook:" + isBookPage);
					if (isBookPage) {
						Map<String, String> bookPageInfo = bookPage.parseBookPageInfo();
						System.out.println(bookPageInfo.toString());
						bookPage.saveBookPageInfo(this.bookId++, bookPageInfo, SITE.toLowerCase() + ".json");
						System.out.println("Saved to amazon.json");
					}
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.out.println(ioe.getMessage());
			System.out.println(ioe.getLocalizedMessage());
		}
	}
	
	
	
	/**
	 * Save all the pageInfo into report. The report has duplicate or bad page.
	 * @param int pageId
	 * @param String title
	 * @param String url
	 * @param int statusCode
	 * @param int numOfLinks
	 * @param int numOfImages
	 */
	private void report(
			int pageId,
			String title,
			String url,
			int statusCode,
			int numOfLinks,
			int numOfImages) {
		try {
			FileWriter fileWriter = new FileWriter(REPORT, true); // false-overwriter;true-append
			StringBuilder info = new StringBuilder();
			info.append(String.format("<a href=\"%s\">%s</a>", url, title)).append(", ")
				.append(String.valueOf(pageId)).append(", ")
				.append(String.valueOf(statusCode)).append(", ")
				.append(String.valueOf(numOfLinks)).append(", ")
				.append(String.valueOf(numOfImages));
			fileWriter.write(info.toString() + "<br>\n");
			fileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	
	
	/**
	 * Check duplicate
	 * @param String title
	 * @param int numOfLinks
	 * @param int numOfImages
	 * @param int bodySize
	 * @return
	 */
	private boolean isDuplicate(String title, ArrayList<Integer> pageInfo) {
		if (visitedPages.containsKey(title)) {
			for (int i = 0; i < visitedPages.get(title).size(); i++) {
				if (visitedPages.get(title).get(i).get(0).intValue() == pageInfo.get(0).intValue() &&
					visitedPages.get(title).get(i).get(1).intValue() == pageInfo.get(1).intValue() && 
					visitedPages.get(title).get(i).get(2).intValue() == pageInfo.get(2).intValue()) {
					return true;
				}
			}
		}
		return false;
	}
	
	
	
	/**
	 * use robots.txt to check out whether is allowed to crawl that server 
	 * @param String url
	 * @return
	 */
	public boolean isAllowed(String url) {
		try {
			URL URL = new URL(url);
			String domain = URL.getHost();
			String robotsUrl = URL.getProtocol() + "://" + domain + "/robots.txt";
			Connection.Response response = Jsoup.connect(robotsUrl)
												.userAgent(USER_AGENT)
												.timeout(100000)
												.ignoreHttpErrors(true)
												.execute();
			Document robotDocument = response.parse();
			SimpleRobotRulesParser parser = new SimpleRobotRulesParser();
			BaseRobotRules rules = parser.parseContent(
					domain, robotDocument.toString().getBytes("UTF-8"),
					"text/plain", USER_AGENT);
			return rules.isAllowed(url);
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
			return false;
		}
	}
	
	
	

	
	

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Crawler crawler = new Crawler();
		crawler.run();
//		System.out.println("Test...................");
//		System.out.println("before report =="+ new Date().toString());
//		crawler.report(1, "dd", "rrr", 200, 7, 89);
//		System.out.println("after report"+ new Date().toString());
	}

}
