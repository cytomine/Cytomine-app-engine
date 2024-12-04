package be.cytomine.appengine.models.task.formats;

import java.awt.Dimension;
import java.util.Arrays;

public class DicomFormat implements FileFormat {

    public static final byte[] SIGNATURE = { (byte) 0x44, (byte) 0x49, (byte) 0x43, (byte) 0x4D };

    @Override
    public boolean checkSignature(byte[] file) {
        if (file.length < SIGNATURE.length) {
            return false;
        }

        return Arrays.equals(Arrays.copyOf(file, SIGNATURE.length), SIGNATURE);
    }

    @Override
    public boolean validate(byte[] file) {
        return true;
    }

    @Override
    public Dimension getDimensions(byte[] file) {
        return null;
    }
}
