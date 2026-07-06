package com.k2040.geojoystick;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class LocationLinkParser {
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");
    private static final Pattern AT_PATTERN = Pattern.compile("@(-?\\d{1,2}(?:\\.\\d+)?),(-?\\d{1,3}(?:\\.\\d+)?)");
    private static final Pattern DATA_PATTERN = Pattern.compile("!3d(-?\\d{1,2}(?:\\.\\d+)?)!4d(-?\\d{1,3}(?:\\.\\d+)?)");
    private static final Pattern QUERY_PATTERN = Pattern.compile("(?:[?&](?:q|query|ll|destination)=)(-?\\d{1,2}(?:\\.\\d+)?)[,\\s]+(-?\\d{1,3}(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLAIN_PATTERN = Pattern.compile("(?<!\\d)(-?\\d{1,2}(?:\\.\\d+)?)[,\\s]+(-?\\d{1,3}(?:\\.\\d+)?)(?!\\d)");

    private LocationLinkParser() {
    }

    static double[] resolveCoordinates(String sharedText) {
        if (sharedText == null || sharedText.trim().isEmpty()) {
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
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(urlText).openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(10_000);
            connection.setRequestProperty("User-Agent", "GeoJoystick/0.1.0 Android utility");
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml");

            int status = connection.getResponseCode();
            String finalUrl = connection.getURL().toString();
            double[] fromFinalUrl = parseCoordinates(finalUrl);
            if (fromFinalUrl != null) {
                return fromFinalUrl;
            }

            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            if (stream == null) {
                return null;
            }

            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                char[] buffer = new char[4096];
                int read;
                while ((read = reader.read(buffer)) >= 0 && body.length() < 262_144) {
                    body.append(buffer, 0, read);
                }
            }
            return parseCoordinates(body.toString());
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    static double[] parseCoordinates(String text) {
        if (text == null) {
            return null;
        }

        String decoded = text;
        for (int i = 0; i < 2; i++) {
            try {
                decoded = URLDecoder.decode(decoded, StandardCharsets.UTF_8.name());
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

    private static double[] match(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            try {
                double lat = Double.parseDouble(matcher.group(1));
                double lng = Double.parseDouble(matcher.group(2));
                if (lat >= -90.0 && lat <= 90.0 && lng >= -180.0 && lng <= 180.0) {
                    return new double[]{lat, lng};
                }
            } catch (NumberFormatException ignored) {
                // Continue searching the text for another coordinate pair.
            }
        }
        return null;
    }

    private static String trimTrailingPunctuation(String value) {
        return value.replaceAll("[)\\]}>.,;]+$", "");
    }
}
