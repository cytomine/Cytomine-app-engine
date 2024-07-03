package be.cytomine.appengine.models.task.image.formats;

public interface SignatureChecker {
    boolean checkSignature(byte[] file);
}
