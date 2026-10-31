package com.socialpath;

import com.socialpath.session.IpLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Without the database file, as in a local run: private addresses are still
 * named, everything else is unknown, and nothing is ever looked up online.
 */
class IpLocatorTest {

    private final IpLocator locator = new IpLocator("no-such-dir/no-such-database.mmdb");

    @ParameterizedTest
    @ValueSource(strings = {"127.0.0.1", "10.1.2.3", "172.18.0.1", "192.168.1.20", "::1", "fd12:3456::1", "fe80::1"})
    void locate_PrivateAddress_IsTheLocalNetwork(String ip) {
        assertEquals(IpLocator.LOCAL_NETWORK, locator.locate(ip));
    }

    @Test
    void locate_PublicAddressWithoutDatabase_IsUnknown() {
        assertNull(locator.locate("203.0.113.7"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not an ip", "example.com"})
    void locate_NotAnAddress_IsUnknownWithoutADnsLookup(String ip) {
        assertNull(locator.locate(ip));
    }

    @Test
    void locate_Null_IsUnknown() {
        assertNull(locator.locate(null));
    }
}
