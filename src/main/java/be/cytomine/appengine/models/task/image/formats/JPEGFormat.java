package be.cytomine.appengine.models.task.image.formats;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;

import be.cytomine.appengine.models.task.image.ImageFormat;

public class JPEGFormat implements ImageFormat {

    public static final byte[] SIGNATURE = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };

    @Override
    public boolean checkSignature(byte[] file) {
        if (file.length < SIGNATURE.length) {
            return false;
        }

        return Arrays.equals(Arrays.copyOf(file, SIGNATURE.length), SIGNATURE);
    }

    @Override
    public List<Integer> getDimensions(byte[] file) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(file));
            return Arrays.asList(image.getWidth(), image.getHeight());
        } catch (IOException e) {
            return null;
        }
    }
}
