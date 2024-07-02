package be.cytomine.appengine.models.task.image.formats;

import java.util.Arrays;

public class JPEGChecker extends SignatureChecker {

    public static final byte[] SIGNATURE = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };

    @Override
    public boolean checkSignature(byte[] file) {
        if (file.length < SIGNATURE.length) {
            return false;
        }

        return Arrays.equals(Arrays.copyOf(file, SIGNATURE.length), SIGNATURE);
    }
}
