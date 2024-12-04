package be.cytomine.appengine.models.task.image;

import java.util.HashMap;
import java.util.Map;

import be.cytomine.appengine.models.task.formats.FileFormat;
import be.cytomine.appengine.models.task.formats.GenericFormat;
import be.cytomine.appengine.models.task.formats.JPEGFormat;
import be.cytomine.appengine.models.task.formats.PNGFormat;
import be.cytomine.appengine.models.task.formats.TIFFFormat;

public class ImageFormatFactory {
    private static final Map<String, FileFormat> formats = new HashMap<>();

    static {
        formats.put("JPEG", new JPEGFormat());
        formats.put("PNG", new PNGFormat());
        formats.put("TIFF", new TIFFFormat());
    }

    public static FileFormat getFormat(String format) {
        return formats.get(format.toUpperCase());
    }

    public static FileFormat getGenericFormat() {
        return new GenericFormat();
    }
}
