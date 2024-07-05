package be.cytomine.appengine.models.task.image.formats;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import be.cytomine.appengine.models.task.image.ImageFormat;

import org.apache.commons.imaging.bytesource.ByteSource;
import org.apache.commons.imaging.formats.tiff.TiffImageParser;

public class TIFFFormat implements ImageFormat {

    public static final byte[] LE_SIGNATURE = {
            (byte) 0x49, (byte) 0x49, (byte) 0x2A, (byte) 0x00
    };

    public static final byte[] BE_SIGNATURE = {
            (byte) 0x4D, (byte) 0x4D, (byte) 0x00, (byte) 0x2A
    };

    @Override
    public boolean checkSignature(byte[] file) {
        if (file.length < LE_SIGNATURE.length && file.length < BE_SIGNATURE.length) {
            return false;
        }

        // Check little-endian signature
        if (Arrays.equals(Arrays.copyOf(file, LE_SIGNATURE.length), LE_SIGNATURE)) {
            return true;
        }

        // Check big-endian signature
        if (Arrays.equals(Arrays.copyOf(file, BE_SIGNATURE.length), BE_SIGNATURE)) {
            return true;
        }

        return false;
    }

    @Override
    public List<Integer> getDimensions(byte[] file) {
        TiffImageParser parser = new TiffImageParser();
        try {
            BufferedImage image = parser.getBufferedImage(ByteSource.array(file), null);
            return List.of(image.getWidth(), image.getHeight());
        } catch (IOException e) {
            return List.of();
        }
    }
}
