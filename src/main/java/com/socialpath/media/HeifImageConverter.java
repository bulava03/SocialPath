package com.socialpath.media;

import com.socialpath.exception.InvalidImageException;
import com.socialpath.media.BundledFfmpeg.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamSource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Semaphore;

/**
 * Converts HEIF-family photos (HEIC from iPhones, AVIF) into PNG so the rest
 * of the avatar pipeline can read them: Java's ImageIO cannot decode them.
 *
 * The conversion runs the bundled ffmpeg (see {@link BundledFfmpeg}), and only
 * a few conversions run at once. If ffmpeg cannot run on this platform, such
 * uploads are rejected with a clear message.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HeifImageConverter {

    /** ISO base media file brands of HEIF still images (HEIC, HEIF, AVIF). */
    private static final Set<String> HEIF_BRANDS = Set.of(
            "heic", "heix", "heim", "heis", "hevc", "hevx", "mif1", "msf1", "avif", "avis");
    private static final int HEADER_LENGTH = 12;
    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private static final int MAX_PARALLEL_CONVERSIONS = 2;

    private final BundledFfmpeg ffmpeg;
    private final Semaphore conversions = new Semaphore(MAX_PARALLEL_CONVERSIONS);

    /**
     * Tells whether a file is a HEIF-family image, by its first bytes: an
     * "ftyp" box whose major brand is one of the HEIF still image brands.
     * @param header the first bytes of the file
     * @return true for HEIC, HEIF and AVIF images
     */
    public static boolean isHeif(byte[] header) {
        if (header.length < HEADER_LENGTH) {
            return false;
        }
        String boxType = new String(header, 4, 4, StandardCharsets.US_ASCII);
        String brand = new String(header, 8, 4, StandardCharsets.US_ASCII);
        return "ftyp".equals(boxType) && HEIF_BRANDS.contains(brand);
    }

    /**
     * Tells whether a picture is a HEIF-family image.
     * @param source the picture
     * @return true for HEIC, HEIF and AVIF images
     */
    public boolean isHeif(InputStreamSource source) throws IOException {
        try (InputStream in = source.getInputStream()) {
            return isHeif(in.readNBytes(HEADER_LENGTH));
        }
    }

    /**
     * Converts a HEIF-family image into a temporary PNG file. The image is
     * rotated as the photo's metadata says. The caller deletes the file.
     * @param source the HEIC/HEIF/AVIF picture
     * @return the temporary PNG file
     * @throws InvalidImageException when the image cannot be converted
     */
    public Path convertToPng(InputStreamSource source) throws IOException {
        if (ffmpeg.path(Tool.FFMPEG).isEmpty()) {
            throw new InvalidImageException("HEIC and AVIF images are not supported on this server.");
        }
        Path input = Files.createTempFile("avatar-heif-", ".bin");
        Path output = Files.createTempFile("avatar-heif-", ".png");
        boolean converted = false;
        try {
            try (InputStream in = source.getInputStream()) {
                Files.copy(in, input, StandardCopyOption.REPLACE_EXISTING);
            }
            BundledFfmpeg.Result result = convert(input, output);
            if (!result.succeeded() || Files.size(output) == 0) {
                log.warn("ffmpeg could not convert a HEIF image: {}", result.stderr().trim());
                throw new InvalidImageException("The image file is damaged or unreadable.");
            }
            converted = true;
            return output;
        } finally {
            Files.deleteIfExists(input);
            if (!converted) {
                Files.deleteIfExists(output);
            }
        }
    }

    private BundledFfmpeg.Result convert(Path input, Path output) throws IOException {
        try {
            conversions.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting to convert an image", e);
        }
        try {
            return ffmpeg.run(Tool.FFMPEG, TIMEOUT,
                    "-v", "error", "-y", "-i", input.toString(), "-frames:v", "1", output.toString());
        } catch (BundledFfmpeg.TimeoutException e) {
            throw new InvalidImageException("The image took too long to process.", e);
        } finally {
            conversions.release();
        }
    }
}
