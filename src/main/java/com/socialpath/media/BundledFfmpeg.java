package com.socialpath.media;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.Loader;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Runs the ffmpeg and ffprobe programs bundled with the bytedeco ffmpeg
 * library: nothing has to be installed on the machine, and the same build
 * works locally and in Docker. Each run is a separate process with a timeout,
 * so a broken or hostile file can neither hang nor crash the application.
 */
@Slf4j
@Component
public class BundledFfmpeg {

    /** The bundled programs. */
    public enum Tool {
        /** Converts media. */
        FFMPEG(org.bytedeco.ffmpeg.ffmpeg.class),
        /** Reads media properties. */
        FFPROBE(org.bytedeco.ffmpeg.ffprobe.class);

        private final Class<?> loaderClass;

        Tool(Class<?> loaderClass) {
            this.loaderClass = loaderClass;
        }
    }

    /**
     * Outcome of a finished run.
     * @param exitCode the process exit code, 0 on success
     * @param stdout what the program printed to standard output
     * @param stderr what the program printed to standard error
     */
    public record Result(int exitCode, String stdout, String stderr) {
        /**
         * Tells whether the program reported success.
         * @return true for exit code 0
         */
        public boolean succeeded() {
            return exitCode == 0;
        }
    }

    /** A run that went over its time limit and was killed. */
    public static class TimeoutException extends IOException {
        TimeoutException(Tool tool, Duration timeout) {
            super(tool + " did not finish within " + timeout.toSeconds() + " s");
        }
    }

    /** How long a killed process gets to actually exit. */
    private static final Duration KILL_GRACE = Duration.ofSeconds(5);
    private static final int DELETE_ATTEMPTS = 10;
    private static final Duration DELETE_RETRY_PAUSE = Duration.ofMillis(100);

    private final Map<Tool, String> paths = new EnumMap<>(Tool.class);
    private final Map<Tool, Boolean> unavailable = new EnumMap<>(Tool.class);

    /**
     * Extracts a bundled program on first use. A platform without a bundled
     * build is remembered, so the failure is logged only once.
     * @param tool the program
     * @return its executable, or empty when it cannot run on this platform
     */
    public synchronized Optional<String> path(Tool tool) {
        if (!paths.containsKey(tool) && !unavailable.containsKey(tool)) {
            try {
                String path = Loader.load(tool.loaderClass);
                paths.put(tool, path);
                log.info("Using bundled {}: {}", tool.name().toLowerCase(Locale.ROOT), path);
            } catch (Throwable e) {
                unavailable.put(tool, true);
                log.warn("Bundled {} is not available on this platform", tool.name().toLowerCase(Locale.ROOT), e);
            }
        }
        return Optional.ofNullable(paths.get(tool));
    }

    /**
     * Runs a bundled program and waits for it.
     * @param tool the program
     * @param timeout how long it may run before it is killed
     * @param args its arguments
     * @return the exit code and output
     * @throws IllegalStateException when the program cannot run on this platform
     * @throws TimeoutException when it ran out of time
     */
    public Result run(Tool tool, Duration timeout, String... args) throws IOException {
        String executable = path(tool)
                .orElseThrow(() -> new IllegalStateException(tool + " is not available on this platform"));
        List<String> command = new ArrayList<>();
        command.add(executable);
        command.addAll(List.of(args));

        // Output goes to files rather than pipes: a chatty process can never block on a full pipe.
        Path stdout = Files.createTempFile("ffmpeg-", ".out");
        Path stderr = Files.createTempFile("ffmpeg-", ".err");
        try {
            ProcessBuilder builder = new ProcessBuilder(command)
                    .redirectOutput(stdout.toFile())
                    .redirectError(stderr.toFile());
            addBundledLibrariesToPath(builder, executable);
            Process process = builder.start();
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                // Until it has really exited it keeps its output files open (and locked on Windows).
                process.waitFor(KILL_GRACE.toMillis(), TimeUnit.MILLISECONDS);
                throw new TimeoutException(tool, timeout);
            }
            return new Result(process.exitValue(), Files.readString(stdout), Files.readString(stderr));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while running " + tool, e);
        } finally {
            deleteQuietly(stdout);
            deleteQuietly(stderr);
        }
    }

    /**
     * Removes a temporary file; failing to do so must never hide the actual
     * outcome of the run. On Windows a killed process releases its files a
     * moment after it has exited, hence a few short retries; a file still
     * held after that is removed when the application exits.
     * @param file the file to remove
     */
    private static void deleteQuietly(Path file) {
        for (int attempt = 1; attempt <= DELETE_ATTEMPTS; attempt++) {
            try {
                Files.deleteIfExists(file);
                return;
            } catch (IOException e) {
                if (!pause(DELETE_RETRY_PAUSE)) {
                    break;
                }
            }
        }
        file.toFile().deleteOnExit();
        log.debug("Temporary file {} is still in use; it will be removed on exit", file);
    }

    private static boolean pause(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * The bundled shared libraries are extracted next to the executables.
     * Windows looks for libraries in the executable's directory by itself;
     * Linux and macOS have to be told through the library path variable.
     * @param builder the process to configure
     * @param executable the path of the bundled executable
     */
    public static void addBundledLibrariesToPath(ProcessBuilder builder, String executable) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return;
        }
        String variable = os.contains("mac") ? "DYLD_LIBRARY_PATH" : "LD_LIBRARY_PATH";
        String libraryDir = Path.of(executable).getParent().toString();
        String existing = builder.environment().get(variable);
        builder.environment().put(variable,
                existing == null || existing.isEmpty() ? libraryDir : libraryDir + File.pathSeparator + existing);
    }
}
