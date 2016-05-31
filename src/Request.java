import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public abstract class Request {

    public String url;
    public String type;
    private static final String USER_AGENT = Crawler.USER_AGENT;
    private Set<String> visitedUrls = new HashSet<String>();
    private Map<String, ArrayList<ArrayList<Integer>>> visitedPages = new HashMap();

    Request(String url, String type) {
        this.url = url;
        this.type = type;
    }

    abstract void parse(Response response);

    public Response visit() {
        Response response = null;
        try {
            response = Jsoup.connect(url)
                            .userAgent(USER_AGENT)
                            .timeout(100000)
                            .ignoreHttpErrors(true)
                            .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    /**
     * use robots.txt to check out whether is allowed to crawl that server
     */
    public boolean isAllowed() {
        try {
            URL URL = new URL(url);
            String domain = URL.getHost();
            String robotsUrl = URL.getProtocol() + "://" + domain + "/robots.txt";
            Response response = Jsoup.connect(robotsUrl)
                                     .userAgent(USER_AGENT)
                                     .timeout(100000)
                                     .ignoreHttpErrors(true)
                                     .execute();
            Document robotDocument = response.parse();
            SimpleRobotRulesParser parser = new SimpleRobotRulesParser();
            BaseRobotRules rules = parser.parseContent(
                domain, robotDocument.toString().getBytes("UTF-8"),
                "text/plain", USER_AGENT
            );
            return rules.isAllowed(url);
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
            return false;
        }
    }

    public boolean isDuplicateUrl() {
        if (visitedUrls.contains(url)) {
            return true;
        } else {
            visitedUrls.add(url);
            return false;
        }
    }

    public boolean isDuplicatePage(Response response) {
        Document htmlDocument = null;
        try {
            htmlDocument = response.parse();
        } catch (IOException e) {
            e.printStackTrace();
            return true;
        }

        String title = htmlDocument.title();
        int numOfLinks = htmlDocument.select("a[href]").size();
        int numOfImages = htmlDocument.select("img").size();
        int bodySize = response.bodyAsBytes().length;
        ArrayList<Integer> pageInfo = new ArrayList<Integer>(Arrays.asList(numOfLinks, numOfImages, bodySize));

        if (visitedPages.containsKey(title)) {
            for (int i = 0; i < visitedPages.get(title).size(); i++) {
                if (visitedPages.get(title).get(i).get(0).intValue() == pageInfo.get(0).intValue() &&
                    visitedPages.get(title).get(i).get(1).intValue() == pageInfo.get(1).intValue() &&
                    visitedPages.get(title).get(i).get(2).intValue() == pageInfo.get(2).intValue()) {
                    return true;
                }
            }
        }
        else {
            visitedPages.put(title, new ArrayList<ArrayList<Integer>>());
        }
        visitedPages.get(title).add(pageInfo);
        return false;
    }
}

class CategoryRequest extends Request {

    CategoryRequest(String url) {
        super(url, "CategoryRequest");
    }

    // find sub category via 'category menu' and add CategoryRequest in Crawler.requestQueue
    // find category leafs via 'category menu' and add CategoryLeafRequest in Crawler.requestQueue
    @Override
    public void parse(Response response) {

    }
}

class CategoryLeafRequest extends Request {

    CategoryLeafRequest(String url) {
        super(url, "CategoryLeafRequest");
    }

    // find book pages on current page via 'book page link' and add BookPageRequest in Crawler.requestQueue
    // find category leafs via 'next page' and add CategoryLeafRequest in Crawler.requestQueue
    @Override
    public void parse(Response response) {
        Document htmlDocument = null;
        try {
            htmlDocument = response.parse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        PageParser pageParser = PageParserFactory.getPageParser(Crawler.SITE, url, htmlDocument);

        // find book pages on current page via 'book page link' and add BookPageRequest in Crawler.requestQueue
        ArrayList<String> bookPageLinks = pageParser.parseBookPageLinks();
        for (String bookPageLink : bookPageLinks) {
            Crawler.requestQueue.add(new BookPageRequest(bookPageLink));
        }
        System.out.println("\tAdded " + bookPageLinks.size() + " BookPageRequest in Crawler.requestQueue!");

        // find category leafs via 'next page' and add CategoryLeafRequest in Crawler.requestQueue
        String nextPageLink = pageParser.parseNextPageLink();
        if (nextPageLink != null) {
            Crawler.requestQueue.add(new CategoryLeafRequest(nextPageLink));
        }
        System.out.println("\tAdded 1 CategoryLeafRequest in Crawler.requestQueue!");
    }
}

class BookPageRequest extends Request {

    BookPageRequest(String url) {
        super(url, "BookPageRequest");
    }

    // call BookPage.parseBookPageInfo and BookPage.saveBookPageInfo
    @Override
    public void parse(Response response) {
        Document htmlDocument = null;
        try {
            htmlDocument = response.parse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        PageParser pageParser = PageParserFactory.getPageParser(Crawler.SITE, url, htmlDocument);
        boolean isBookPage = pageParser.isBookPage();
        System.out.println("\tisBook:" + isBookPage);
        if (isBookPage) {
            Map<String, Object> pageParserInfo = pageParser.parseBookPageInfo();
            pageParser.saveBookPageInfo(Crawler.bookId, pageParserInfo, Crawler.SITE + ".json");
            System.out.printf("\tSaved to %s.json => bookId: %s, title: %s\n", Crawler.SITE, Crawler.bookId, pageParserInfo.get("title"));
            Crawler.bookId++;
        }
    }
}
