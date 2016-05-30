import org.jsoup.nodes.Document;

public class PageParserFactory {
    private String site;
    private String url;
    private Document htmlDocument;

    public static PageParser getPageParser(String site, String url, Document htmlDocument) {
        if (site.equals("amazon")) {
            return new AmazonPageParser(url, htmlDocument);
        } 
        else if (site.equals("barnesNoble")) {
            return new BarnesNoblePageParser(url, htmlDocument);
        }
        else if (site.equals("betterWorld")) {
            return new BetterWorldPageParser(url, htmlDocument);
        }
        else if (site.equals("textbook")) {
            return new TextbookPageParser(url, htmlDocument);
        }
        else {
            throw new RuntimeException("site not found: " + site);
        }
    }
}
