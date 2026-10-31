package com.socialpath;

import com.socialpath.session.DeviceInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Real User-Agent strings: most browsers also name the ones they are built on
 * (Edge says Chrome and Safari), so the order of the checks matters.
 */
class DeviceInfoTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36|Chrome|Windows",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 Edg/130.0.0.0|Edge|Windows",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15|Safari|macOS",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1|Safari|iOS",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/130.0.0.0 Mobile/15E148 Safari/604.1|Chrome|iOS",
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36|Chrome|Android",
        "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/26.0 Chrome/122.0.0.0 Mobile Safari/537.36|Samsung Internet|Android",
        "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:131.0) Gecko/20100101 Firefox/131.0|Firefox|Linux",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 OPR/115.0.0.0|Opera|Windows",
        "Mozilla/5.0 (X11; CrOS x86_64 14541.0.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36|Chrome|ChromeOS",
    })
    void from_KnownBrowsers(String userAgent, String browser, String os) {
        assertEquals(new DeviceInfo(browser, os), DeviceInfo.from(userAgent));
    }

    @Test
    void from_UnknownOrMissingHeader() {
        DeviceInfo unknown = new DeviceInfo(DeviceInfo.UNKNOWN_BROWSER, DeviceInfo.UNKNOWN_OS);

        assertEquals(unknown, DeviceInfo.from(null));
        assertEquals(unknown, DeviceInfo.from("curl/8.5.0"));
    }
}
