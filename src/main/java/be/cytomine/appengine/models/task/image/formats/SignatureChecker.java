package be.cytomine.appengine.models.task.image.formats;

public abstract class SignatureChecker {
    public abstract boolean checkSignature(byte[] file);
}
