package com.socialpath;

import com.socialpath.exception.InvalidImageException;
import com.socialpath.media.AvatarImageProcessor;
import com.socialpath.media.AvatarImageProcessor.ProcessedAvatar;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvatarImageProcessorTest {

    private final AvatarImageProcessor processor = new AvatarImageProcessor();

    @Test
    void process_LargeLandscapePhoto_ProducesSquareFullAndThumbnail() throws IOException {
        // Arrange
        byte[] png = encode(solidImage(2000, 1500, BufferedImage.TYPE_INT_RGB, Color.BLUE), "png");

        // Act
        ProcessedAvatar result = processor.process(new ByteArrayResource(png));

        // Assert
        BufferedImage full = decode(result.full());
        BufferedImage thumbnail = decode(result.thumbnail());
        assertEquals(AvatarImageProcessor.FULL_SIZE, full.getWidth());
        assertEquals(AvatarImageProcessor.FULL_SIZE, full.getHeight());
        assertEquals(AvatarImageProcessor.THUMBNAIL_SIZE, thumbnail.getWidth());
        assertEquals(AvatarImageProcessor.THUMBNAIL_SIZE, thumbnail.getHeight());
    }

    @Test
    void process_SmallPhoto_IsCroppedButNotUpscaled() throws IOException {
        // Arrange
        byte[] png = encode(solidImage(300, 200, BufferedImage.TYPE_INT_RGB, Color.BLUE), "png");

        // Act
        ProcessedAvatar result = processor.process(new ByteArrayResource(png));

        // Assert
        BufferedImage full = decode(result.full());
        BufferedImage thumbnail = decode(result.thumbnail());
        assertEquals(200, full.getWidth());
        assertEquals(200, full.getHeight());
        assertEquals(200, thumbnail.getWidth());
        assertEquals(200, thumbnail.getHeight());
    }

    @Test
    void process_TransparentPng_IsFlattenedOntoWhite() throws IOException {
        // Arrange: a fully transparent picture
        byte[] png = encode(new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB), "png");

        // Act
        ProcessedAvatar result = processor.process(new ByteArrayResource(png));

        // Assert: JPEG has no alpha, and the transparent area became white
        BufferedImage full = decode(result.full());
        Color corner = new Color(full.getRGB(0, 0));
        assertTrue(corner.getRed() > 245 && corner.getGreen() > 245 && corner.getBlue() > 245,
                "expected white, got " + corner);
    }

    @Test
    void process_NotAnImage_IsRejected() {
        // Arrange
        byte[] text = "definitely not a picture".getBytes(StandardCharsets.UTF_8);

        // Act & Assert
        assertThrows(InvalidImageException.class, () -> processor.process(new ByteArrayResource(text)));
    }

    @Test
    void process_ResolutionAboveLimit_IsRejectedBeforeDecoding() throws IOException {
        // Arrange: a limit of 10,000 pixels and a 200x100 picture
        AvatarImageProcessor strictProcessor = new AvatarImageProcessor(10_000);
        byte[] png = encode(solidImage(200, 100, BufferedImage.TYPE_INT_RGB, Color.BLUE), "png");

        // Act & Assert
        assertThrows(InvalidImageException.class, () -> strictProcessor.process(new ByteArrayResource(png)));
    }

    private static BufferedImage solidImage(int width, int height, int type, Color color) {
        BufferedImage image = new BufferedImage(width, height, type);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        return image;
    }

    private static byte[] encode(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }

    private static BufferedImage decode(byte[] bytes) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }
}
