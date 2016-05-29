import org.jsoup.nodes.Document;

public class BookPageFactory {
    private String site;
    private String url;
    private Document htmlDocument;

    public static BookPage getBookPage(String site, String url, Document htmlDocument) {
        if (site.equals("amazon")) {
            return new AmazonBookPage(url, htmlDocument);
        } else {
            throw new RuntimeException("site not found: " + site);
        }
    }
}
