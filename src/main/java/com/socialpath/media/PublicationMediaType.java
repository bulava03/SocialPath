package com.socialpath.media;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * The media formats a post or comment may carry. The type of an upload is
 * read from its first bytes, never from the file name or the content type the
 * client sends, and the stored file gets the extension of that type: an HTML
 * page renamed to photo.jpg is not a photo.
 *
 * Browsers play only some video formats; the others are accepted and then
 * converted to MP4 (see VideoService).
 */
public enum PublicationMediaType {
    /** JPEG photo. */
    JPEG("jpg", false, true),
    /** PNG picture. */
    PNG("png", false, true),
    /** GIF picture. */
    GIF("gif", false, true),
    /** WebP picture. */
    WEBP("webp", false, true),
    /** MP4 video. */
    MP4("mp4", true, true),
    /** WebM video (Matroska with the "webm" document type). */
    WEBM("webm", true, true),
    /** QuickTime video, as iPhones and many cameras record. */
    MOV("mov", true, false),
    /** AVI video. */
    AVI("avi", true, false),
    /** Matroska video other than WebM. */
    MKV("mkv", true, false),
    /** Windows Media video (ASF container). */
    WMV("wmv", true, false),
    /** 3GPP video from older phones. */
    THREE_GP("3gp", true, false);

    /** Bytes needed to tell the formats apart (the WebM document type sits a few dozen bytes in). */
    public static final int HEADER_LENGTH = 64;

    private static final Set<String> GIF_SIGNATURES = Set.of("GIF87a", "GIF89a");

    /** EBML header element of the document type "webm": id 0x4282, size 4, then the text. */
    private static final byte[] WEBM_DOC_TYPE = {0x42, (byte) 0x82, (byte) 0x84, 'w', 'e', 'b', 'm'};

    /** ISO base media brands of MP4 video (HEIF image brands are left out on purpose). */
    private static final Set<String> MP4_BRANDS = Set.of(
            "isom", "iso2", "iso3", "iso4", "iso5", "iso6", "mp41", "mp42", "avc1", "M4V ", "dash", "mmp4", "MSNV");

    /** Atoms an old QuickTime file may start with when it has no "ftyp" box. */
    private static final Set<String> QUICKTIME_ATOMS = Set.of("moov", "mdat", "wide", "free", "skip", "pnot");

    /** The GUID every ASF (Windows Media) file starts with. */
    private static final int[] ASF_GUID = {
        0x30, 0x26, 0xB2, 0x75, 0x8E, 0x66, 0xCF, 0x11, 0xA6, 0xD9, 0x00, 0xAA, 0x00, 0x62, 0xCE, 0x6C};

    private final String extension;
    private final boolean video;
    private final boolean browserFormat;

    PublicationMediaType(String extension, boolean video, boolean browserFormat) {
        this.extension = extension;
        this.video = video;
        this.browserFormat = browserFormat;
    }

    /**
     * Extension stored files of this type get.
     * @return the extension without a dot
     */
    public String extension() {
        return extension;
    }

    /**
     * Tells videos from photos.
     * @return true for a video format
     */
    public boolean isVideo() {
        return video;
    }

    /**
     * Tells formats browsers can show from those that have to be converted.
     * A browser format may still need converting for its codec or size.
     * @return true when browsers can show this format
     */
    public boolean isBrowserFormat() {
        return browserFormat;
    }

    /**
     * Recognizes a format by the first bytes of a file.
     * @param header the first HEADER_LENGTH bytes, fewer if the file is shorter
     * @return the format, or empty when the file is none of the allowed ones
     */
    public static Optional<PublicationMediaType> detect(byte[] header) {
        Optional<PublicationMediaType> picture = detectPicture(header);
        return picture.isPresent() ? picture : detectVideo(header);
    }

    private static Optional<PublicationMediaType> detectPicture(byte[] header) {
        if (startsWith(header, 0xFF, 0xD8, 0xFF)) {
            return Optional.of(JPEG);
        }
        if (startsWith(header, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
            return Optional.of(PNG);
        }
        if (GIF_SIGNATURES.contains(ascii(header, 0, 6))) {
            return Optional.of(GIF);
        }
        if (ascii(header, 0, 4).equals("RIFF") && ascii(header, 8, 4).equals("WEBP")) {
            return Optional.of(WEBP);
        }
        return Optional.empty();
    }

    private static Optional<PublicationMediaType> detectVideo(byte[] header) {
        String box = ascii(header, 4, 4);
        String brand = ascii(header, 8, 4);
        if (box.equals("ftyp")) {
            return brandOf(brand);
        }
        if (QUICKTIME_ATOMS.contains(box)) {
            return Optional.of(MOV);
        }
        if (ascii(header, 0, 4).equals("RIFF") && brand.equals("AVI ")) {
            return Optional.of(AVI);
        }
        if (startsWith(header, 0x1A, 0x45, 0xDF, 0xA3)) {
            return Optional.of(contains(header, WEBM_DOC_TYPE) ? WEBM : MKV);
        }
        if (startsWith(header, ASF_GUID)) {
            return Optional.of(WMV);
        }
        return Optional.empty();
    }

    /**
     * The video format of an ISO base media file; HEIF images and unknown brands are none.
     * @param brand the major brand from the "ftyp" box
     * @return the format, or empty
     */
    private static Optional<PublicationMediaType> brandOf(String brand) {
        if (MP4_BRANDS.contains(brand)) {
            return Optional.of(MP4);
        }
        if (brand.equals("qt  ")) {
            return Optional.of(MOV);
        }
        if (brand.startsWith("3gp") || brand.startsWith("3g2")) {
            return Optional.of(THREE_GP);
        }
        return Optional.empty();
    }

    /**
     * The format of a stored file, by the extension the server gave it.
     * @param fileId a stored file id such as {uuid}.mp4
     * @return the format, or empty for any other extension
     */
    public static Optional<PublicationMediaType> ofFileId(String fileId) {
        int dot = fileId.lastIndexOf('.');
        if (dot < 0) {
            return Optional.empty();
        }
        String extension = fileId.substring(dot + 1).toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(type -> type.extension.equals(extension)).findFirst();
    }

    private static boolean contains(byte[] data, byte[] part) {
        for (int i = 0; i + part.length <= data.length; i++) {
            if (Arrays.equals(data, i, i + part.length, part, 0, part.length)) {
                return true;
            }
        }
        return false;
    }

    private static boolean startsWith(byte[] data, int... prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if ((data[i] & 0xFF) != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static String ascii(byte[] data, int from, int length) {
        if (data.length < from + length) {
            return "";
        }
        return new String(data, from, length, StandardCharsets.US_ASCII);
    }
}
