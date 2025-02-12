package be.cytomine.appengine.models.task.formats;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class PngFormat implements FileFormat {

    public static final byte[] SIGNATURE = {
        (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47,
        (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A
    };

    @Override
    public boolean checkSignature(File file) {
        if (file.length() < SIGNATURE.length) {
            return false;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] fileSignature = new byte[SIGNATURE.length];
            int bytesRead = fis.read(fileSignature);

            return bytesRead == SIGNATURE.length && Arrays.equals(fileSignature, SIGNATURE);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public Dimension getDimensions(File file) {
        byte[] ihdrChunk = new byte[8];

        try (FileInputStream fis = new FileInputStream(file)) {
            if (fis.read(ihdrChunk, 0, 8) != 8) {
                return null;
            }

            String ihdr = new String(ihdrChunk, 4, 4);
            if (!ihdr.equals("IHDR")) {
                return null;
            }

            byte[] widthBytes = new byte[4];
            byte[] heightBytes = new byte[4];
            if (fis.read(widthBytes) != 4 || fis.read(heightBytes) != 4) {
                return null;
            }

            Integer width = ByteBuffer.wrap(widthBytes).getInt();
            Integer height = ByteBuffer.wrap(heightBytes).getInt();

            return new Dimension(width, height);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public boolean validate(File file) {
        return true;
    }
}
