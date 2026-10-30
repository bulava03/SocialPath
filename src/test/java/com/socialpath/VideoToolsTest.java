package com.socialpath;

import com.socialpath.exception.InvalidVideoException;
import com.socialpath.media.BundledFfmpeg;
import com.socialpath.media.BundledFfmpeg.Tool;
import com.socialpath.media.VideoProbe;
import com.socialpath.media.VideoProbe.VideoInfo;
import com.socialpath.media.VideoTranscoder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Probing and converting real videos with the bundled ffmpeg. The test videos
 * are generated here from ffmpeg's built-in test pattern, so nothing
 * third-party is needed.
 */
class VideoToolsTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(120);

    private static BundledFfmpeg ffmpeg;
    private static VideoProbe probe;
    private static VideoTranscoder transcoder;

    @TempDir
    Path dir;

    @BeforeAll
    static void tools() {
        ffmpeg = new BundledFfmpeg();
        assumeTrue(ffmpeg.path(Tool.FFMPEG).isPresent() && ffmpeg.path(Tool.FFPROBE).isPresent(),
                "bundled ffmpeg is not available on this platform");
        probe = new VideoProbe(ffmpeg);
        transcoder = new VideoTranscoder(ffmpeg);
    }

    /** One second of test pattern; with sound when asked; any container by file name. */
    private Path generate(String name, String size, boolean sound) throws IOException {
        Path file = dir.resolve(name);
        List<String> args = new ArrayList<>(List.of("-v", "error", "-y",
                "-f", "lavfi", "-i", "testsrc=size=" + size + ":rate=10:duration=1"));
        if (sound) {
            args.addAll(List.of("-f", "lavfi", "-i", "sine=frequency=440:duration=1", "-c:a", "pcm_s16le"));
        }
        args.addAll(List.of("-c:v", "mpeg4", file.toString()));
        run(args.toArray(String[]::new));
        return file;
    }

    private void run(String... args) throws IOException {
        BundledFfmpeg.Result result = ffmpeg.run(Tool.FFMPEG, TIMEOUT, args);
        assertTrue(result.succeeded(), () -> "could not generate a test video: " + result.stderr());
    }

    @Test
    void probe_ReadsLengthSizeAndCodecs() throws IOException {
        Path mov = generate("camera.mov", "1920x1080", true);

        VideoInfo info = probe.probe(mov);

        assertEquals(1920, info.width());
        assertEquals(1080, info.height());
        assertEquals("mpeg4", info.videoCodec());
        assertEquals("pcm_s16le", info.audioCodec());
        assertTrue(Math.abs(info.duration().toMillis() - 1000) < 200, info.duration().toString());
    }

    @Test
    void probe_SomethingThatIsNotAVideo_IsRejected() throws IOException {
        Path text = Files.writeString(dir.resolve("notes.mov"), "not a video at all");

        InvalidVideoException error = assertThrows(InvalidVideoException.class, () -> probe.probe(text));
        assertEquals(VideoProbe.UNREADABLE, error.getMessage());
    }

    @Test
    void toMp4_BigVideo_BecomesH264AndAacWithinTheSizeLimit() throws IOException {
        Path mov = generate("camera.mov", "1920x1080", true);
        Path mp4 = dir.resolve("out.mp4");

        transcoder.toMp4(mov, mp4, TIMEOUT);

        VideoInfo out = probe.probe(mp4);
        assertEquals("h264", out.videoCodec());
        assertEquals("aac", out.audioCodec());
        assertEquals(1280, out.width());
        assertEquals(720, out.height());
    }

    @Test
    void toMp4_SmallSilentVideo_IsNotEnlarged_AndStaysSilent() throws IOException {
        Path avi = generate("old.avi", "320x240", false);
        Path mp4 = dir.resolve("out.mp4");

        transcoder.toMp4(avi, mp4, TIMEOUT);

        VideoInfo out = probe.probe(mp4);
        assertEquals(320, out.width());
        assertEquals(240, out.height());
        assertNull(out.audioCodec());
    }

    @Test
    void toMp4_PortraitPhoneVideo_IsTurnedUpright() throws IOException {
        // Phones store portrait videos sideways with a rotation mark; copy the
        // stream to add the mark without turning the picture.
        Path sideways = generate("sideways.mov", "640x360", false);
        Path marked = dir.resolve("portrait.mov");
        run("-v", "error", "-y", "-display_rotation:v", "90", "-i", sideways.toString(), "-c", "copy", marked.toString());
        Path mp4 = dir.resolve("out.mp4");

        transcoder.toMp4(marked, mp4, TIMEOUT);

        VideoInfo out = probe.probe(mp4);
        assertEquals(360, out.width());
        assertEquals(640, out.height());
    }

    @Test
    void toMp4_UnreadableSource_FailsAndLeavesNothing() throws IOException {
        Path broken = Files.writeString(dir.resolve("broken.mov"), "not a video at all");
        Path mp4 = dir.resolve("out.mp4");

        assertThrows(IOException.class, () -> transcoder.toMp4(broken, mp4, TIMEOUT));
        assertFalse(Files.exists(mp4));
    }

    @Test
    void run_OverTheTimeLimit_IsKilled() {
        // A long encode with almost no time allowed.
        assertThrows(BundledFfmpeg.TimeoutException.class, () -> ffmpeg.run(Tool.FFMPEG, Duration.ofMillis(300),
                "-v", "error", "-y", "-f", "lavfi", "-i", "testsrc=size=1920x1080:rate=30:duration=600",
                "-c:v", "mpeg4", dir.resolve("long.mp4").toString()));
    }
}
