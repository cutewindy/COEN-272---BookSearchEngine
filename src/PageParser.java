import java.util.*;

interface PageParser {
    // parse category leaf page
    ArrayList<String> parseBookPageLinks();
    String parseNextPageLink();

    // parse book page
    boolean isBookPage();
    Map<String, Object> parseBookPageInfo();
    void saveBookPageInfo(int bookId, Map<String, Object> bookPageInfo, String filename);
}
