import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

interface BookPage {
    boolean isBookPage();
    Map<String, String> parseBookPageInfo();
    void saveBookPageInfo(int bookId, Map<String, String> bookPageInfo, String filename) throws IOException;
}
