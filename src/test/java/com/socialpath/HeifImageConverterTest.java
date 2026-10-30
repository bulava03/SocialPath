package com.socialpath;

import com.socialpath.media.AvatarImageProcessor;
import com.socialpath.media.AvatarImageProcessor.ProcessedAvatar;
import com.socialpath.media.BundledFfmpeg;
import com.socialpath.media.BundledFfmpeg.Tool;
import com.socialpath.media.HeifImageConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class HeifImageConverterTest {

    @Test
    void isHeif_IphoneHeicHeader_IsDetected() {
        assertTrue(HeifImageConverter.isHeif(header("heic")));
    }

    @Test
    void isHeif_GenericHeifAndAvifHeaders_AreDetected() {
        assertTrue(HeifImageConverter.isHeif(header("mif1")));
        assertTrue(HeifImageConverter.isHeif(header("avif")));
    }

    @Test
    void isHeif_Mp4VideoHeader_IsNotAnImage() {
        // Same container family, but a video brand.
        assertFalse(HeifImageConverter.isHeif(header("isom")));
    }

    @Test
    void isHeif_JpegAndShortInput_AreNotHeif() {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0, 1};
        assertFalse(HeifImageConverter.isHeif(jpeg));
        assertFalse(HeifImageConverter.isHeif(new byte[]{0, 0, 0}));
    }

    /**
     * End to end through the bundled ffmpeg: an AVIF photo (the same HEIF
     * container iPhones use for HEIC) becomes a square JPEG avatar. The
     * bundled ffmpeg can write AVIF but not HEIC, so AVIF stands in here.
     */
    @Test
    void process_AvifPhoto_IsConvertedIntoAvatar(@TempDir Path dir) throws Exception {
        // Arrange
        BundledFfmpeg ffmpeg = new BundledFfmpeg();
        assumeTrue(ffmpeg.path(Tool.FFMPEG).isPresent(), "bundled ffmpeg is not available on this platform");
        Path avif = dir.resolve("photo.avif");
        BundledFfmpeg.Result generated = ffmpeg.run(Tool.FFMPEG, Duration.ofSeconds(60),
                "-v", "error", "-y", "-f", "lavfi", "-i", "color=c=red:s=300x200",
                "-frames:v", "1", "-c:v", "libaom-av1", "-still-picture", "1", avif.toString());
        // ffmpeg is available here, so failing to run it is a real failure, not a skip.
        assertTrue(generated.succeeded(), () -> "could not generate a test AVIF: " + generated.stderr());
        AvatarImageProcessor processor = new AvatarImageProcessor(new HeifImageConverter(ffmpeg));

        // Act
        ProcessedAvatar result = processor.process(new FileSystemResource(avif));

        // Assert
        BufferedImage full = ImageIO.read(new ByteArrayInputStream(result.full()));
        assertEquals(200, full.getWidth());
        assertEquals(200, full.getHeight());
    }

    private static byte[] header(String brand) {
        byte[] header = new byte[12];
        header[3] = 0x18;
        System.arraycopy("ftyp".getBytes(StandardCharsets.US_ASCII), 0, header, 4, 4);
        System.arraycopy(brand.getBytes(StandardCharsets.US_ASCII), 0, header, 8, 4);
        return header;
    }
}
