package be.cytomine.appengine.models.task.image;

import java.util.HashMap;
import java.util.Map;

import be.cytomine.appengine.models.task.image.formats.JPEGFormat;
import be.cytomine.appengine.models.task.image.formats.PNGFormat;

public class ImageFormatFactory {
    private static final Map<String, ImageFormat> formats = new HashMap<>();

    static {
        formats.put("PNG", new PNGFormat());
        formats.put("JPEG", new JPEGFormat());
    }

    public static ImageFormat getFormat(String format) {
        return formats.get(format.toUpperCase());
    }
}
