import java.util.*;

interface PageParser {
    // parse category leaf page
    ArrayList<String> parseBookPageLinks();
    String parseNextPageLink();

    // parse book page
    boolean isBookPage();
    Map<String, String> parseBookPageInfo();
    void saveBookPageInfo(int bookId, Map<String, String> bookPageInfo, String filename);
}
