package be.cytomine.appengine.models.task.image;

import java.util.List;

public interface ImageFormat {
    boolean checkSignature(byte[] file);

    List<Integer> getDimensions(byte[] file);
}
