package be.cytomine.appengine.models.task.image.formats;

import java.util.Arrays;

public class PNGChecker extends SignatureChecker {

    public static final byte[] SIGNATURE = {
            (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47,
            (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A
    };

    @Override
    public boolean checkSignature(byte[] file) {
        if (file.length < SIGNATURE.length) {
            return false;
        }

        return Arrays.equals(Arrays.copyOf(file, SIGNATURE.length), SIGNATURE);
    }
}
