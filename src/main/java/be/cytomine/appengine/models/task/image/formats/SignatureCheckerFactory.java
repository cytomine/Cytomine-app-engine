package be.cytomine.appengine.models.task.image.formats;

import java.util.HashMap;
import java.util.Map;

public class SignatureCheckerFactory {
    private static final Map<String, SignatureChecker> checkers = new HashMap<>();

    static {
        checkers.put("PNG", new PNGChecker());
        checkers.put("JPEG", new JPEGChecker());
    }

    public static SignatureChecker getChecker(String format) {
        return checkers.get(format.toUpperCase());
    }
}
