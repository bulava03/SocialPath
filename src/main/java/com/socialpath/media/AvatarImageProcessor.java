package com.socialpath.media;

import com.socialpath.exception.InvalidImageException;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.core.io.InputStreamSource;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;

/**
 * Turns an uploaded picture into the two avatar versions the site serves: a
 * full one for the profile/group page and a thumbnail for lists, search,
 * comments and the header. Both are center-cropped squares, re-encoded as
 * JPEG, with any transparency flattened onto white so no part of the page
 * shows through. Smaller pictures are never upscaled.
 */
@Component
public class AvatarImageProcessor {

    /** Side of the full version, in pixels: the profile/group page avatar. */
    public static final int FULL_SIZE = 1024;
    /** Side of the thumbnail, in pixels: lists, search, comments, header. */
    public static final int THUMBNAIL_SIZE = 256;

    /** About 50 megapixels: any real camera photo fits, a decompression bomb does not. */
    private static final long DEFAULT_MAX_PIXELS = 50_000_000L;
    private static final float JPEG_QUALITY = 0.85f;

    private final long maxPixels;

    public AvatarImageProcessor() {
        this(DEFAULT_MAX_PIXELS);
    }

    public AvatarImageProcessor(long maxPixels) {
        this.maxPixels = maxPixels;
        // Picks up image format plugins (WebP) bundled inside the application jar.
        ImageIO.scanForPlugins();
    }

    /**
     * The two encoded versions of an avatar.
     * @param full the full version as JPEG bytes
     * @param thumbnail the thumbnail as JPEG bytes
     */
    public record ProcessedAvatar(byte[] full, byte[] thumbnail) {
    }

    /**
     * Processes an uploaded or stored picture. The source is read twice: once
     * to check the dimensions without decoding the pixels, once to decode.
     * @param source the picture (an upload or a stored file)
     * @return the full version and the thumbnail as JPEG bytes
     * @throws InvalidImageException when the picture cannot be used
     */
    public ProcessedAvatar process(InputStreamSource source) throws IOException {
        int shortSide = checkDimensions(source);
        int fullSide = Math.min(FULL_SIZE, shortSide);

        BufferedImage cropped;
        try (InputStream in = source.getInputStream()) {
            // Thumbnailator also applies the EXIF orientation of phone photos.
            cropped = Thumbnails.of(in)
                    .size(fullSide, fullSide)
                    .crop(Positions.CENTER)
                    .asBufferedImage();
        } catch (IOException | IllegalArgumentException e) {
            throw new InvalidImageException("The image file is damaged or unreadable.", e);
        }

        BufferedImage full = onWhiteBackground(cropped);
        int thumbnailSide = Math.min(THUMBNAIL_SIZE, fullSide);
        return new ProcessedAvatar(
                toJpeg(full, fullSide),
                toJpeg(full, thumbnailSide));
    }

    /**
     * Reads only the image header and rejects unsupported formats and
     * pictures too large to decode safely.
     * @param source the picture
     * @return the shorter side of the picture, in pixels
     */
    private int checkDimensions(InputStreamSource source) throws IOException {
        try (InputStream in = source.getInputStream();
             ImageInputStream imageStream = ImageIO.createImageInputStream(in)) {
            Iterator<ImageReader> readers = imageStream == null
                    ? Collections.emptyIterator()
                    : ImageIO.getImageReaders(imageStream);
            if (!readers.hasNext()) {
                throw new InvalidImageException("Unsupported image format. Please use JPEG, PNG, GIF or WebP.");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageStream, true, true);
                long width = reader.getWidth(0);
                long height = reader.getHeight(0);
                if (width * height > maxPixels) {
                    throw new InvalidImageException("The image resolution is too large.");
                }
                return (int) Math.min(width, height);
            } catch (IOException e) {
                throw new InvalidImageException("The image file is damaged or unreadable.", e);
            } finally {
                reader.dispose();
            }
        }
    }

    private BufferedImage onWhiteBackground(BufferedImage image) {
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return rgb;
    }

    private byte[] toJpeg(BufferedImage image, int side) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Thumbnails.of(image)
                .size(side, side)
                .outputFormat("jpg")
                .outputQuality(JPEG_QUALITY)
                .toOutputStream(out);
        return out.toByteArray();
    }
}
