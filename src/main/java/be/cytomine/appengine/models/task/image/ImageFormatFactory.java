package be.cytomine.appengine.models.task.image;

import java.util.HashMap;
import java.util.Map;

import be.cytomine.appengine.models.task.image.formats.JPEGFormat;
import be.cytomine.appengine.models.task.image.formats.PNGFormat;

public class ImageFormatFactory {
    private static final Map<String, ImageFormat> checkers = new HashMap<>();

    static {
        checkers.put("PNG", new PNGFormat());
        checkers.put("JPEG", new JPEGFormat());
    }

    public static ImageFormat getFormat(String format) {
        return checkers.get(format.toUpperCase());
    }
}
