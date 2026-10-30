package com.socialpath.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialpath.exception.InvalidVideoException;
import com.socialpath.media.BundledFfmpeg.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Reads what a video is made of with the bundled ffprobe: length, picture
 * size and codecs. ffprobe only reads the file headers, so this is quick
 * enough to run while an upload is being accepted.
 */
@Component
@RequiredArgsConstructor
public class VideoProbe {

    /** Shown when ffprobe cannot make sense of a file. */
    public static final String UNREADABLE = "The video file is damaged or unreadable.";

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final BundledFfmpeg ffmpeg;
    private final ObjectMapper json = new ObjectMapper();

    /**
     * What a video consists of.
     * @param duration its length
     * @param width picture width as stored, in pixels
     * @param height picture height as stored, in pixels
     * @param videoCodec ffmpeg's name of the video codec, such as "h264"
     * @param audioCodec ffmpeg's name of the audio codec, or null without sound
     */
    public record VideoInfo(Duration duration, int width, int height, String videoCodec, String audioCodec) {
        /**
         * The longer side of the picture; the same whichever way the video is turned.
         * @return the larger of width and height
         */
        public int longerSide() {
            return Math.max(width, height);
        }
    }

    /**
     * Reads a video's properties.
     * @param file the video
     * @return its length, size and codecs
     * @throws InvalidVideoException when the file is not a readable video
     */
    public VideoInfo probe(Path file) throws IOException {
        BundledFfmpeg.Result result = ffmpeg.run(Tool.FFPROBE, TIMEOUT,
                "-v", "error", "-print_format", "json", "-show_format", "-show_streams", file.toString());
        if (!result.succeeded()) {
            throw new InvalidVideoException(UNREADABLE);
        }

        JsonNode root = json.readTree(result.stdout());
        JsonNode video = firstStream(root, "video");
        JsonNode audio = firstStream(root, "audio");
        double seconds = root.path("format").path("duration").asDouble(Double.NaN);
        if (video == null || Double.isNaN(seconds)) {
            throw new InvalidVideoException(UNREADABLE);
        }
        return new VideoInfo(
                Duration.ofMillis(Math.round(seconds * 1000)),
                video.path("width").asInt(),
                video.path("height").asInt(),
                video.path("codec_name").asText(),
                audio == null ? null : audio.path("codec_name").asText());
    }

    private static JsonNode firstStream(JsonNode root, String type) {
        for (JsonNode stream : root.path("streams")) {
            if (type.equals(stream.path("codec_type").asText())) {
                return stream;
            }
        }
        return null;
    }
}
