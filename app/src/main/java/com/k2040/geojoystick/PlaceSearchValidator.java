package com.k2040.geojoystick;

final class PlaceSearchValidator {
    static final int MAX_QUERY_CHARS = 256;
    static final int STATUS_SUCCESS = 0;
    static final int STATUS_NO_RESULT = 1;
    static final int STATUS_AMBIGUOUS = 2;
    static final int STATUS_INVALID = 3;

    private static final double MAX_MAP_LATITUDE = 85.05112878;
    private static final double SAME_LOCATION_EPSILON = 0.000001;

    static final class Resolution {
        final int status;
        final double latitude;
        final double longitude;

        Resolution(int status, double latitude, double longitude) {
            this.status = status;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    private PlaceSearchValidator() {
    }

    static String sanitizeQuery(String raw) {
        if (raw == null) {
            return null;
        }
        String query = raw.trim();
        if (query.isEmpty() || query.length() > MAX_QUERY_CHARS) {
            return null;
        }
        for (int index = 0; index < query.length(); index++) {
            char value = query.charAt(index);
            if (Character.isISOControl(value) && !Character.isWhitespace(value)) {
                return null;
            }
        }
        return query;
    }

    static Resolution resolve(double[][] candidates) {
        if (candidates == null || candidates.length == 0) {
            return new Resolution(STATUS_NO_RESULT, Double.NaN, Double.NaN);
        }

        double acceptedLatitude = Double.NaN;
        double acceptedLongitude = Double.NaN;
        boolean accepted = false;

        for (double[] candidate : candidates) {
            if (candidate == null
                    || candidate.length < 2
                    || !validLatitude(candidate[0])
                    || !validLongitude(candidate[1])) {
                return new Resolution(STATUS_INVALID, Double.NaN, Double.NaN);
            }

            if (!accepted) {
                acceptedLatitude = candidate[0];
                acceptedLongitude = candidate[1];
                accepted = true;
                continue;
            }

            if (Math.abs(candidate[0] - acceptedLatitude) > SAME_LOCATION_EPSILON
                    || Math.abs(candidate[1] - acceptedLongitude)
                    > SAME_LOCATION_EPSILON) {
                return new Resolution(STATUS_AMBIGUOUS, Double.NaN, Double.NaN);
            }
        }

        return accepted
                ? new Resolution(STATUS_SUCCESS, acceptedLatitude, acceptedLongitude)
                : new Resolution(STATUS_NO_RESULT, Double.NaN, Double.NaN);
    }

    static boolean validLatitude(double value) {
        return Double.isFinite(value)
                && value >= -MAX_MAP_LATITUDE
                && value <= MAX_MAP_LATITUDE;
    }

    static boolean validLongitude(double value) {
        return Double.isFinite(value) && value >= -180.0 && value <= 180.0;
    }
}
