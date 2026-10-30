package com.socialpath;

import com.socialpath.media.PublicationMediaRules;
import com.socialpath.media.PublicationMediaType;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Uploads are judged by their content, not by the name or the content type
 * the client sends: those are trivial to forge.
 */
class PublicationMediaRulesTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0, 1};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D};
    private static final byte[] GIF = ascii("GIF89a\0\0\0\0\0\0");
    private static final byte[] WEBP = ascii("RIFF\0\0\0\0WEBPVP8 ");
    private static final byte[] MP4 = ascii("\0\0\0\u0018ftypisom\0\0\0\0");
    private static final byte[] HTML = ascii("<script>alert(1)</script>");
    /** The start of a real WebM file: EBML header with the document type "webm". */
    private static final byte[] WEBM = {
            0x1A, 0x45, (byte) 0xDF, (byte) 0xA3, (byte) 0x9F, 0x42, (byte) 0x86, (byte) 0x81, 0x01, 0x42, (byte) 0xF7, (byte) 0x81,
            0x01, 0x42, (byte) 0xF2, (byte) 0x81, 0x04, 0x42, (byte) 0xF3, (byte) 0x81, 0x08, 0x42, (byte) 0x82, (byte) 0x84, 'w', 'e', 'b', 'm'};

    private static byte[] ascii(String text) {
        return text.getBytes(StandardCharsets.ISO_8859_1);
    }

    /** A file whose name and declared type claim a photo, whatever its bytes are. */
    private static MultipartFile upload(byte[] content) {
        return new MockMultipartFile("media", "photo.jpg", "image/jpeg", content);
    }

    /** A file of the given size whose content starts like the given format. */
    private static MultipartFile upload(byte[] header, long size) {
        byte[] content = Arrays.copyOf(header, (int) size);
        return upload(content);
    }

    @Test
    void detect_RecognizesEveryAllowedFormatByItsBytes() {
        assertEquals(PublicationMediaType.JPEG, PublicationMediaType.detect(JPEG).orElseThrow());
        assertEquals(PublicationMediaType.PNG, PublicationMediaType.detect(PNG).orElseThrow());
        assertEquals(PublicationMediaType.GIF, PublicationMediaType.detect(GIF).orElseThrow());
        assertEquals(PublicationMediaType.WEBP, PublicationMediaType.detect(WEBP).orElseThrow());
        assertEquals(PublicationMediaType.MP4, PublicationMediaType.detect(MP4).orElseThrow());
        assertEquals(PublicationMediaType.WEBM, PublicationMediaType.detect(WEBM).orElseThrow());
    }

    @Test
    void detect_PlainMatroskaIsNotWebm() {
        // Same container, but browsers only promise to play the WebM flavour.
        byte[] matroska = Arrays.copyOf(WEBM, WEBM.length + 4);
        byte[] docType = {(byte) 0x88, 'm', 'a', 't', 'r', 'o', 's', 'k', 'a'};
        System.arraycopy(docType, 0, matroska, WEBM.length - 5, docType.length);

        assertTrue(PublicationMediaType.detect(matroska).isEmpty());
    }

    @Test
    void detect_HeicPhotoIsNotMistakenForMp4Video() {
        // Same container family as MP4, but an image brand.
        assertTrue(PublicationMediaType.detect(ascii("\0\0\0\u0018ftypheic\0\0\0\0")).isEmpty());
    }

    @Test
    void check_HtmlDisguisedAsAPhoto_IsRefused() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> PublicationMediaRules.check(List.of(upload(HTML))));

        assertEquals(PublicationMediaRules.NOT_ALLOWED, error.getMessage());
    }

    @Test
    void check_EmptyOrMissing_IsFine() throws IOException {
        assertTrue(PublicationMediaRules.check(null).isEmpty());
        assertTrue(PublicationMediaRules.check(List.of()).isEmpty());
    }

    @Test
    void check_ReturnsTheTypeOfEachFileInOrder() throws IOException {
        List<PublicationMediaType> types = PublicationMediaRules.check(List.of(upload(PNG), upload(JPEG)));

        assertEquals(List.of(PublicationMediaType.PNG, PublicationMediaType.JPEG), types);
    }

    @Test
    void check_SixPhotos_IsRefused() {
        List<MultipartFile> six = Collections.nCopies(6, upload(JPEG));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> PublicationMediaRules.check(six));
        assertEquals(PublicationMediaRules.TOO_MANY, error.getMessage());
    }

    @Test
    void check_VideoWithAPhoto_IsRefused() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> PublicationMediaRules.check(List.of(upload(MP4), upload(JPEG))));

        assertEquals(PublicationMediaRules.VIDEO_NOT_ALONE, error.getMessage());
    }

    @Test
    void check_PhotoOverTheLimit_IsRefused() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> PublicationMediaRules.check(List.of(upload(JPEG, PublicationMediaRules.MAX_PHOTO_BYTES + 1))));

        assertEquals(PublicationMediaRules.PHOTO_TOO_LARGE, error.getMessage());
    }

    @Test
    void check_LargeVideoWithinItsLimit_IsFine() throws IOException {
        // Bigger than a photo may be, but videos have their own limit.
        long size = PublicationMediaRules.MAX_PHOTO_BYTES + 1;

        assertEquals(List.of(PublicationMediaType.MP4), PublicationMediaRules.check(List.of(upload(MP4, size))));
    }
}
