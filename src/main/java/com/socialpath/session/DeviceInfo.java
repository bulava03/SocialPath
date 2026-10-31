package com.socialpath.session;

import java.util.List;

/**
 * The browser and operating system named by a User-Agent header, for the list
 * of active sessions. Only the common ones are told apart; that is enough to
 * recognise one's own devices, and a full parser library would be overkill.
 * @param browser e.g. "Chrome", "Firefox"
 * @param os e.g. "Windows", "Android"
 */
public record DeviceInfo(String browser, String os) {

    /** Shown when the browser is not recognised. */
    public static final String UNKNOWN_BROWSER = "Unknown browser";
    /** Shown when the operating system is not recognised. */
    public static final String UNKNOWN_OS = "unknown system";

    /** Checked in order: Edge and Opera also say "Chrome", Chrome also says "Safari". */
    private static final List<String[]> BROWSERS = List.of(
            new String[] {"Edg", "Edge"},
            new String[] {"OPR/", "Opera"},
            new String[] {"SamsungBrowser/", "Samsung Internet"},
            new String[] {"Firefox/", "Firefox"},
            new String[] {"FxiOS/", "Firefox"},
            new String[] {"Chrome/", "Chrome"},
            new String[] {"CriOS/", "Chrome"},
            new String[] {"Safari/", "Safari"});

    /** Checked in order: Android also says "Linux", iOS also says "Mac OS X". */
    private static final List<String[]> SYSTEMS = List.of(
            new String[] {"Android", "Android"},
            new String[] {"iPhone", "iOS"},
            new String[] {"iPad", "iPadOS"},
            new String[] {"Windows", "Windows"},
            new String[] {"Mac OS X", "macOS"},
            new String[] {"CrOS", "ChromeOS"},
            new String[] {"Linux", "Linux"});

    /**
     * Reads a User-Agent header.
     * @param userAgent the header, may be null
     * @return the browser and system, with "unknown" names for what is not recognised
     */
    public static DeviceInfo from(String userAgent) {
        if (userAgent == null) {
            return new DeviceInfo(UNKNOWN_BROWSER, UNKNOWN_OS);
        }
        return new DeviceInfo(find(BROWSERS, userAgent, UNKNOWN_BROWSER), find(SYSTEMS, userAgent, UNKNOWN_OS));
    }

    private static String find(List<String[]> known, String userAgent, String fallback) {
        for (String[] entry : known) {
            if (userAgent.contains(entry[0])) {
                return entry[1];
            }
        }
        return fallback;
    }
}
