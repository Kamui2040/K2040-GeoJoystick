package com.k2040.geojoystick;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class LocationLinkParser {
    private static final int MAX_SHARED_TEXT_CHARS = 8_192;
    private static final int MAX_URL_CHARS = 4_096;
    private static final int MAX_RESPONSE_CHARS = 262_144;
    private static final int MAX_REDIRECTS = 5;
    private static final int TIMEOUT_MS = 8_000;

    private static final Pattern URL_PATTERN = Pattern.compile(
            "https://[^\\s<>\"']+",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern AT_PATTERN = Pattern.compile(
            "@(-?\\d{1,2}(?:\\.\\d+)?),(-?\\d{1,3}(?:\\.\\d+)?)");
    private static final Pattern DATA_PATTERN = Pattern.compile(
            "!3d(-?\\d{1,2}(?:\\.\\d+)?)!4d(-?\\d{1,3}(?:\\.\\d+)?)");
    private static final Pattern QUERY_PATTERN = Pattern.compile(
            "(?:[?&](?:q|query|ll|destination)=)(-?\\d{1,2}(?:\\.\\d+)?)[,\\s]+(-?\\d{1,3}(?:\\.\\d+)?)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PLAIN_PATTERN = Pattern.compile(
            "(?<!\\d)(-?\\d{1,2}(?:\\.\\d+)?)[,\\s]+(-?\\d{1,3}(?:\\.\\d+)?)"
                    + "(?!\\d)");

    private LocationLinkParser() {
    }

    static double[] resolveCoordinates(String sharedText) {
        if (sharedText == null || sharedText.trim().isEmpty()
                || sharedText.length() > MAX_SHARED_TEXT_CHARS) {
            return null;
        }

        double[] direct = parseCoordinates(sharedText);
        if (direct != null) {
            return direct;
        }

        Matcher urlMatcher = URL_PATTERN.matcher(sharedText);
        if (!urlMatcher.find()) {
            return null;
        }

        String urlText = trimTrailingPunctuation(urlMatcher.group());
        if (urlText.length() > MAX_URL_CHARS) {
            return null;
        }

        try {
            return resolveHttpsUrl(urlText);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static double[] resolveHttpsUrl(String urlText) throws IOException {
        URL current = validatePublicHttpsUrl(new URL(urlText));

        for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) current.openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(TIMEOUT_MS);
                connection.setReadTimeout(TIMEOUT_MS);
                connection.setUseCaches(false);
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "GeoJoystick Android utility");
                connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,text/plain");
                connection.setRequestProperty("Accept-Encoding", "identity");

                int status = connection.getResponseCode();
                double[] fromCurrentUrl = parseCoordinates(connection.getURL().toString());
                if (fromCurrentUrl != null) {
                    return fromCurrentUrl;
                }

                if (isRedirect(status)) {
                    if (redirects >= MAX_REDIRECTS) {
                        return null;
                    }
                    String location = connection.getHeaderField("Location");
                    if (location == null || location.trim().isEmpty()
                            || location.length() > MAX_URL_CHARS) {
                        return null;
                    }
                    current = validatePublicHttpsUrl(new URL(current, location));
                    continue;
                }

                if (status < 200 || status >= 400
                        || !isAllowedContentType(connection.getContentType())) {
                    return null;
                }

                int contentLength = connection.getContentLength();
                if (contentLength > MAX_RESPONSE_CHARS) {
                    return null;
                }

                try (InputStream stream = connection.getInputStream()) {
                    return parseCoordinates(readLimited(stream));
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        return null;
    }

    static double[] parseCoordinates(String text) {
        if (text == null || text.length() > MAX_RESPONSE_CHARS) {
            return null;
        }

        String decoded = text;
        for (int i = 0; i < 2; i++) {
            try {
                decoded = URLDecoder.decode(decoded, StandardCharsets.UTF_8.name());
                if (decoded.length() > MAX_RESPONSE_CHARS) {
                    return null;
                }
            } catch (Exception ignored) {
                break;
            }
        }

        double[] result = match(decoded, DATA_PATTERN);
        if (result == null) result = match(decoded, AT_PATTERN);
        if (result == null) result = match(decoded, QUERY_PATTERN);
        if (result == null) result = match(decoded, PLAIN_PATTERN);
        return result;
    }

    private static URL validatePublicHttpsUrl(URL url) throws IOException {
        if (url == null
                || !"https".equalsIgnoreCase(url.getProtocol())
                || url.getUserInfo() != null
                || url.toString().length() > MAX_URL_CHARS) {
            throw new IOException("Unsupported URL");
        }

        int port = url.getPort();
        if (port != -1 && port != 443) {
            throw new IOException("Unsupported port");
        }

        String host = url.getHost();
        if (host == null || host.trim().isEmpty()) {
            throw new IOException("Missing host");
        }

        String normalizedHost = host.toLowerCase(Locale.ROOT);
        while (normalizedHost.endsWith(".")) {
            normalizedHost = normalizedHost.substring(0, normalizedHost.length() - 1);
        }
        if (normalizedHost.trim().isEmpty()
                || "localhost".equals(normalizedHost)
                || normalizedHost.endsWith(".localhost")
                || normalizedHost.endsWith(".local")) {
            throw new IOException("Private host");
        }

        InetAddress[] addresses = InetAddress.getAllByName(normalizedHost);
        if (addresses.length == 0) {
            throw new IOException("Unresolved host");
        }
        for (InetAddress address : addresses) {
            if (!isPublicAddress(address)) {
                throw new IOException("Non-public destination");
            }
        }
        return url;
    }

    private static boolean isPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return false;
        }

        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address && bytes.length == 4) {
            int a = bytes[0] & 0xff;
            int b = bytes[1] & 0xff;
            int c = bytes[2] & 0xff;

            if (a == 0 || a == 10 || a == 127 || a >= 224) return false;
            if (a == 100 && b >= 64 && b <= 127) return false;
            if (a == 169 && b == 254) return false;
            if (a == 172 && b >= 16 && b <= 31) return false;
            if (a == 192 && b == 168) return false;
            if (a == 192 && b == 0 && (c == 0 || c == 2)) return false;
            if (a == 198 && (b == 18 || b == 19)) return false;
            if (a == 198 && b == 51 && c == 100) return false;
            if (a == 203 && b == 0 && c == 113) return false;
            return true;
        }

        if (address instanceof Inet6Address && bytes.length == 16) {
            int first = bytes[0] & 0xff;
            int second = bytes[1] & 0xff;
            if ((first & 0xfe) == 0xfc) return false;
            if (first == 0x20 && second == 0x01
                    && (bytes[2] & 0xff) == 0x0d
                    && (bytes[3] & 0xff) == 0xb8) {
                return false;
            }
        }
        return true;
    }

    private static boolean isRedirect(int status) {
        return status == HttpURLConnection.HTTP_MOVED_PERM
                || status == HttpURLConnection.HTTP_MOVED_TEMP
                || status == HttpURLConnection.HTTP_SEE_OTHER
                || status == 307
                || status == 308;
    }

    private static boolean isAllowedContentType(String contentType) {
        if (contentType == null || contentType.trim().isEmpty()) {
            return true;
        }
        String normalized = contentType.toLowerCase(Locale.ROOT);
        return normalized.startsWith("text/html")
                || normalized.startsWith("application/xhtml+xml")
                || normalized.startsWith("text/plain");
    }

    private static String readLimited(InputStream stream) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) >= 0) {
                int remaining = MAX_RESPONSE_CHARS - body.length();
                if (remaining <= 0) {
                    return null;
                }
                body.append(buffer, 0, Math.min(read, remaining));
                if (read > remaining) {
                    return null;
                }
            }
        }
        return body.toString();
    }

    private static double[] match(String text, Pattern pattern) {
        if (text == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            try {
                double lat = Double.parseDouble(matcher.group(1));
                double lng = Double.parseDouble(matcher.group(2));
                if (Double.isFinite(lat)
                        && Double.isFinite(lng)
                        && lat >= -90.0 && lat <= 90.0
                        && lng >= -180.0 && lng <= 180.0) {
                    return new double[]{lat, lng};
                }
            } catch (NumberFormatException ignored) {
                // Continue searching the bounded text for another coordinate pair.
            }
        }
        return null;
    }

    private static String trimTrailingPunctuation(String value) {
        return value.replaceAll("[)\\]}>.,;]+$", "");
    }
}
