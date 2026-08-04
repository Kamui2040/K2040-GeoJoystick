package com.k2040.geojoystick;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
    private static final int MAX_RESPONSE_BYTES = 262_144;
    private static final int MAX_RESPONSE_CHARS = 262_144;
    private static final int MAX_REDIRECTS = 4;
    private static final int TIMEOUT_MS = 5_000;
    private static final long MAX_RESOLUTION_MS = 20_000L;

    private static boolean isSupportedHost(String host) {
        switch (host) {
            case "google.com":
            case "www.google.com":
            case "maps.google.com":
            case "maps.app.goo.gl":
            case "goo.gl":
            case "maps.apple.com":
            case "openstreetmap.org":
            case "www.openstreetmap.org":
                return true;
            default:
                return false;
        }
    }


    private static final Pattern URL_SCHEME_PATTERN = Pattern.compile(
            "https?://",
            Pattern.CASE_INSENSITIVE);
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
    private static final Pattern OSM_QUERY_PATTERN = Pattern.compile(
            "(?:[?&]mlat=)(-?\\d{1,2}(?:\\.\\d+)?)(?:[^#\\s]*?[&]mlon=)(-?\\d{1,3}(?:\\.\\d+)?)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern OSM_FRAGMENT_PATTERN = Pattern.compile(
            "#map=\\d{1,2}(?:\\.\\d+)?/(-?\\d{1,2}(?:\\.\\d+)?)/(-?\\d{1,3}(?:\\.\\d+)?)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PLAIN_PATTERN = Pattern.compile(
            "^\\s*(-?\\d{1,2}(?:\\.\\d+)?)\\s*[,;\\s]\\s*(-?\\d{1,3}(?:\\.\\d+)?)\\s*$");

    private LocationLinkParser() {
    }

    static double[] resolveCoordinates(String sharedText) {
        if (sharedText == null
                || sharedText.trim().isEmpty()
                || sharedText.length() > MAX_SHARED_TEXT_CHARS) {
            return null;
        }

        String inspectedText = decodeBounded(sharedText);
        if (inspectedText == null) {
            return null;
        }
        Matcher urlMatcher = URL_PATTERN.matcher(inspectedText);
        if (!urlMatcher.find()) {
            return URL_SCHEME_PATTERN.matcher(inspectedText).find()
                    ? null
                    : parseCoordinates(inspectedText);
        }

        String urlText = trimTrailingPunctuation(urlMatcher.group());
        if (!isSupportedMapUrl(urlText)) {
            return null;
        }

        double[] direct = parseCoordinates(urlText);
        if (direct != null) {
            return direct;
        }

        try {
            return resolveHttpsUrl(urlText);
        } catch (IOException ignored) {
            return null;
        }
    }

    static boolean isSupportedMapUrl(String text) {
        if (text == null || text.length() > MAX_URL_CHARS) {
            return false;
        }
        try {
            URL url = new URL(text);
            validateSupportedUrlSyntax(url);
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    static boolean isPublicAddressLiteral(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        try {
            return isPublicAddress(InetAddress.getByName(text));
        } catch (IOException exception) {
            return false;
        }
    }

    private static double[] resolveHttpsUrl(String urlText) throws IOException {
        URL current = validatePublicHttpsUrl(new URL(urlText));
        long deadlineNanos = System.nanoTime() + MAX_RESOLUTION_MS * 1_000_000L;

        for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
            int remainingMs = remainingMillis(deadlineNanos);
            if (remainingMs <= 0) {
                return null;
            }

            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) current.openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(Math.min(TIMEOUT_MS, remainingMs));
                connection.setReadTimeout(Math.min(TIMEOUT_MS, remainingMs));
                connection.setUseCaches(false);
                connection.setRequestMethod("GET");
                connection.setRequestProperty(
                        "User-Agent",
                        "GeoJoystick Android (com.k2040.geojoystick; map-link resolver)");
                connection.setRequestProperty(
                        "Accept",
                        "text/html,application/xhtml+xml,text/plain");
                connection.setRequestProperty("Accept-Encoding", "identity");

                int status = connection.getResponseCode();
                URL connectedUrl = validatePublicHttpsUrl(connection.getURL());
                double[] fromCurrentUrl = parseCoordinates(connectedUrl.toString());
                if (fromCurrentUrl != null) {
                    return fromCurrentUrl;
                }

                if (isRedirect(status)) {
                    if (redirects >= MAX_REDIRECTS) {
                        return null;
                    }
                    String location = connection.getHeaderField("Location");
                    if (location == null
                            || location.trim().isEmpty()
                            || location.length() > MAX_URL_CHARS) {
                        return null;
                    }
                    current = validatePublicHttpsUrl(new URL(current, location));
                    continue;
                }

                if (status < 200
                        || status >= 300
                        || !isAllowedContentType(connection.getContentType())) {
                    return null;
                }

                int contentLength = connection.getContentLength();
                if (contentLength > MAX_RESPONSE_BYTES) {
                    return null;
                }

                try (InputStream stream = connection.getInputStream()) {
                    String body = readLimited(stream);
                    return body == null ? null : parseCoordinates(body);
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

        String decoded = decodeBounded(text);
        if (decoded == null) {
            return null;
        }

        double[] result = match(decoded, AT_PATTERN);
        if (result == null) result = match(decoded, DATA_PATTERN);
        if (result == null) result = match(decoded, OSM_QUERY_PATTERN);
        if (result == null) result = match(decoded, OSM_FRAGMENT_PATTERN);
        if (result == null) result = match(decoded, QUERY_PATTERN);
        if (result == null) result = match(decoded, PLAIN_PATTERN);
        return result;
    }

    private static String decodeBounded(String text) {
        String decoded = text;
        for (int i = 0; i < 2; i++) {
            try {
                decoded = URLDecoder.decode(decoded, StandardCharsets.UTF_8.name());
                if (decoded.length() > MAX_RESPONSE_CHARS) {
                    return null;
                }
            } catch (IllegalArgumentException exception) {
                return null;
            } catch (Exception ignored) {
                break;
            }
        }
        return decoded;
    }

    private static URL validatePublicHttpsUrl(URL url) throws IOException {
        validateSupportedUrlSyntax(url);
        String host = normalizeHost(url.getHost());
        InetAddress[] addresses = InetAddress.getAllByName(host);
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

    private static void validateSupportedUrlSyntax(URL url) throws IOException {
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

        String host = normalizeHost(url.getHost());
        if (!isSupportedHost(host)) {
            throw new IOException("Unsupported map host");
        }
        if ("goo.gl".equals(host) && !url.getPath().startsWith("/maps/")) {
            throw new IOException("Unsupported short-link path");
        }
    }

    private static String normalizeHost(String host) throws IOException {
        if (host == null || host.trim().isEmpty()) {
            throw new IOException("Missing host");
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        while (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isEmpty()) {
            throw new IOException("Missing host");
        }
        return normalized;
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

    private static int remainingMillis(long deadlineNanos) {
        long remainingNanos = deadlineNanos - System.nanoTime();
        if (remainingNanos <= 0L) {
            return 0;
        }
        return (int) Math.min(
                Integer.MAX_VALUE,
                Math.max(1L, remainingNanos / 1_000_000L));
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
            return false;
        }
        String normalized = contentType.toLowerCase(Locale.ROOT);
        return normalized.startsWith("text/html")
                || normalized.startsWith("application/xhtml+xml")
                || normalized.startsWith("text/plain");
    }

    private static String readLimited(InputStream stream) throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = stream.read(buffer)) >= 0) {
            if (body.size() + read > MAX_RESPONSE_BYTES) {
                return null;
            }
            body.write(buffer, 0, read);
        }
        return body.toString(StandardCharsets.UTF_8.name());
    }

    private static double[] match(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            try {
                double lat = Double.parseDouble(matcher.group(1));
                double lng = Double.parseDouble(matcher.group(2));
                if (Double.isFinite(lat)
                        && Double.isFinite(lng)
                        && lat >= -90.0
                        && lat <= 90.0
                        && lng >= -180.0
                        && lng <= 180.0) {
                    return new double[]{lat, lng};
                }
            } catch (NumberFormatException ignored) {
                // Continue searching the bounded structured text.
            }
        }
        return null;
    }

    private static String trimTrailingPunctuation(String value) {
        String result = value;
        while (!result.isEmpty()) {
            char last = result.charAt(result.length() - 1);
            if (last == '.' || last == ',' || last == ')' || last == ']' || last == '}') {
                result = result.substring(0, result.length() - 1);
            } else {
                break;
            }
        }
        return result;
    }
}
