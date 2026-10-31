package com.socialpath.session;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Tells roughly where an IP address is, for the list of active sessions. The
 * lookup is offline, in the DB-IP Lite city database the Docker build
 * downloads, so addresses never leave the server. Without the database (a
 * local run) only private addresses get a name.
 */
@Slf4j
@Component
public class IpLocator {

    /** Shown for addresses inside a local network, which no database knows. */
    public static final String LOCAL_NETWORK = "Local network";

    private final DatabaseReader reader;

    public IpLocator(@Value("${app.geoip.database}") String databasePath) {
        this.reader = open(new File(databasePath));
    }

    /**
     * Where an address is.
     * @param ip IPv4 or IPv6 address as text
     * @return "City, Country", just the country, {@link #LOCAL_NETWORK}, or
     *         null when unknown
     */
    public String locate(String ip) {
        InetAddress address = parse(ip);
        if (address == null) {
            return null;
        }
        if (isPrivate(address)) {
            return LOCAL_NETWORK;
        }
        if (reader == null) {
            return null;
        }
        try {
            return reader.tryCity(address).map(IpLocator::describe).orElse(null);
        } catch (IOException | GeoIp2Exception e) {
            log.warn("IP location lookup failed for {}", ip, e);
            return null;
        }
    }

    @PreDestroy
    void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }

    private static DatabaseReader open(File database) {
        if (!database.isFile()) {
            log.info("No IP location database at {}: session locations will show as unknown", database);
            return null;
        }
        try {
            return new DatabaseReader.Builder(database).build();
        } catch (IOException e) {
            log.warn("Could not open the IP location database {}", database, e);
            return null;
        }
    }

    private static String describe(CityResponse response) {
        StringJoiner place = new StringJoiner(", ");
        Optional.ofNullable(response.city().getName()).ifPresent(place::add);
        Optional.ofNullable(response.country().getName()).ifPresent(place::add);
        return place.length() == 0 ? null : place.toString();
    }

    /**
     * Reads an address. Only literal addresses: a host name would make this do a DNS lookup.
     * @param ip the address as text
     * @return the address, or null when it is not one
     */
    private static InetAddress parse(String ip) {
        if (ip == null || ip.isBlank() || !ip.matches("[0-9a-fA-F.:]+")) {
            return null;
        }
        try {
            return InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private static boolean isPrivate(InetAddress address) {
        if (address.isLoopbackAddress() || address.isSiteLocalAddress()
                || address.isLinkLocalAddress() || address.isAnyLocalAddress()) {
            return true;
        }
        // IPv6 unique local addresses (fc00::/7), the IPv6 counterpart of 10.x / 192.168.x.
        byte[] bytes = address.getAddress();
        return bytes.length == 16 && (bytes[0] & 0xFE) == 0xFC;
    }
}
