/*
 * SPDX-License-Identifier: GPL-3.0-only
 * Copyright (c) 2026 K2040.
 * K2040-authored material in this file is also subject to the GPLv3 section 7(b)
 * attribution-preservation term in LICENSES/GPL-3.0-Section-7b-K2040.txt.
 */
package com.k2040.geojoystick;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

/** Shared, package-private access to GeoJoystick UI preferences. */
final class GeoSettings {
    static final String PREFS = "geojoystick";
    static final String PREF_LATITUDE = "last_latitude";
    static final String PREF_LONGITUDE = "last_longitude";
    static final String PREF_ALTITUDE = "last_altitude";
    static final String PREF_MANUAL_LATITUDE = "manual_latitude";
    static final String PREF_MANUAL_LONGITUDE = "manual_longitude";
    static final String PREF_MANUAL_ALTITUDE = "manual_altitude";
    static final String PREF_OVERLAY_X = "overlay_x";
    static final String PREF_OVERLAY_Y = "overlay_y";
    static final String PREF_APPEARANCE = "app_appearance";
    static final String PREF_LANGUAGE = "app_language";
    static final String PREF_RESTORE_LAST_POSITION = "restore_last_position";
    static final String PREF_OVERLAY_OPACITY = "overlay_opacity_percent";
    static final String PREF_OVERLAY_SIZE = "overlay_size_percent";
    static final String PREF_OVERLAY_HIGH_CONTRAST = "overlay_high_contrast";
    static final String PREF_CUSTOM_SPEED = "overlay_custom_speed";
    static final String PREF_CUSTOM_SPEED_NAME = "overlay_custom_speed_name";
    static final String PREF_WELCOME_ACKNOWLEDGED = "welcome_acknowledged";
    static final String PREF_LEGACY_LICENSE_ACCEPTED = "license_accepted";

    static final String APPEARANCE_SYSTEM = "system";
    static final String APPEARANCE_LIGHT = "light";
    static final String APPEARANCE_DARK = "dark";
    static final String LANGUAGE_SYSTEM = "system";
    static final String LANGUAGE_ENGLISH = "en";
    static final String LANGUAGE_GERMAN = "de";
    static final int FAVORITE_COUNT = 5;

    static final class Favorite {
        final String name;
        final double latitude;
        final double longitude;
        final double altitude;

        Favorite(String name, double latitude, double longitude, double altitude) {
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
            this.altitude = altitude;
        }
    }

    private final Context context;
    private final SharedPreferences preferences;

    GeoSettings(Context context) {
        this.context = context;
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    SharedPreferences raw() {
        return preferences;
    }

    boolean isGerman() {
        String language = preferences.getString(PREF_LANGUAGE, LANGUAGE_SYSTEM);
        return LANGUAGE_GERMAN.equals(language)
                || (LANGUAGE_SYSTEM.equals(language)
                && Locale.getDefault().getLanguage().equals("de"));
    }

    boolean isDark() {
        String appearance = preferences.getString(PREF_APPEARANCE, APPEARANCE_SYSTEM);
        if (APPEARANCE_DARK.equals(appearance)) {
            return true;
        }
        if (APPEARANCE_LIGHT.equals(appearance)) {
            return false;
        }
        int nightMode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    String appearance() {
        return preferences.getString(PREF_APPEARANCE, APPEARANCE_SYSTEM);
    }

    void setAppearance(String value) {
        preferences.edit().putString(PREF_APPEARANCE, value).apply();
    }

    String language() {
        return preferences.getString(PREF_LANGUAGE, LANGUAGE_SYSTEM);
    }

    void setLanguage(String value) {
        preferences.edit().putString(PREF_LANGUAGE, value).apply();
    }

    boolean welcomeAcknowledged() {
        return preferences.getBoolean(PREF_WELCOME_ACKNOWLEDGED, false)
                || preferences.getBoolean(PREF_LEGACY_LICENSE_ACCEPTED, false);
    }

    void acknowledgeWelcome() {
        preferences.edit().putBoolean(PREF_WELCOME_ACKNOWLEDGED, true).apply();
    }

    boolean restoreLastPosition() {
        return preferences.getBoolean(PREF_RESTORE_LAST_POSITION, true);
    }

    void setRestoreLastPosition(boolean value) {
        preferences.edit().putBoolean(PREF_RESTORE_LAST_POSITION, value).apply();
    }

    int overlayOpacity() {
        return Math.max(30, Math.min(100, preferences.getInt(PREF_OVERLAY_OPACITY, 85)));
    }

    void setOverlayOpacity(int value) {
        preferences.edit().putInt(PREF_OVERLAY_OPACITY, Math.max(30, Math.min(100, value))).apply();
    }

    int overlaySize() {
        return Math.max(70, Math.min(120, preferences.getInt(PREF_OVERLAY_SIZE, 80)));
    }

    void setOverlaySize(int value) {
        preferences.edit().putInt(PREF_OVERLAY_SIZE, Math.max(70, Math.min(120, value))).apply();
    }

    boolean highContrastOverlay() {
        return preferences.getBoolean(PREF_OVERLAY_HIGH_CONTRAST, false);
    }

    void setHighContrastOverlay(boolean value) {
        preferences.edit().putBoolean(PREF_OVERLAY_HIGH_CONTRAST, value).apply();
    }

    double customSpeed() {
        double value = Double.longBitsToDouble(preferences.getLong(
                PREF_CUSTOM_SPEED,
                Double.doubleToLongBits(5.0)));
        if (!Double.isFinite(value)) {
            return 5.0;
        }
        return Math.max(0.1, Math.min(50.0, value));
    }

    String customSpeedName() {
        String value = preferences.getString(PREF_CUSTOM_SPEED_NAME, "Custom");
        if (value == null || value.trim().isEmpty()) {
            return "Custom";
        }
        return value.trim();
    }

    void setCustomSpeed(String name, double value) {
        preferences.edit()
                .putString(PREF_CUSTOM_SPEED_NAME, name)
                .putLong(PREF_CUSTOM_SPEED, Double.doubleToRawLongBits(value))
                .apply();
    }

    void resetOverlayPosition() {
        preferences.edit().remove(PREF_OVERLAY_X).remove(PREF_OVERLAY_Y).apply();
    }

    double[] manualCoordinates() {
        return readCoordinates(PREF_MANUAL_LATITUDE, PREF_MANUAL_LONGITUDE, PREF_MANUAL_ALTITUDE);
    }

    double[] lastActiveCoordinates() {
        return readCoordinates(PREF_LATITUDE, PREF_LONGITUDE, PREF_ALTITUDE);
    }

    void saveManualCoordinates(double latitude, double longitude, double altitude) {
        if (!validCoordinates(latitude, longitude, altitude)) {
            return;
        }
        preferences.edit()
                .putLong(PREF_MANUAL_LATITUDE, Double.doubleToRawLongBits(latitude))
                .putLong(PREF_MANUAL_LONGITUDE, Double.doubleToRawLongBits(longitude))
                .putLong(PREF_MANUAL_ALTITUDE, Double.doubleToRawLongBits(altitude))
                .apply();
    }

    private double[] readCoordinates(String latitudeKey, String longitudeKey, String altitudeKey) {
        if (!preferences.contains(latitudeKey)
                || !preferences.contains(longitudeKey)
                || !preferences.contains(altitudeKey)) {
            return null;
        }
        double latitude = Double.longBitsToDouble(preferences.getLong(latitudeKey, 0L));
        double longitude = Double.longBitsToDouble(preferences.getLong(longitudeKey, 0L));
        double altitude = Double.longBitsToDouble(preferences.getLong(altitudeKey, 0L));
        if (!validCoordinates(latitude, longitude, altitude)) {
            return null;
        }
        return new double[]{latitude, longitude, altitude};
    }

    Favorite favorite(int slot) {
        if (slot < 0 || slot >= FAVORITE_COUNT
                || !preferences.getBoolean(favoriteKey(slot, "set"), false)) {
            return null;
        }
        String name = preferences.getString(favoriteKey(slot, "name"), "Favorite " + (slot + 1));
        double latitude = favoriteDouble(slot, "latitude");
        double longitude = favoriteDouble(slot, "longitude");
        double altitude = favoriteDouble(slot, "altitude");
        if (!validCoordinates(latitude, longitude, altitude)) {
            return null;
        }
        return new Favorite(name == null ? "Favorite " + (slot + 1) : name,
                latitude,
                longitude,
                altitude);
    }

    void saveFavorite(int slot, String name, double latitude, double longitude, double altitude) {
        if (slot < 0 || slot >= FAVORITE_COUNT || !validCoordinates(latitude, longitude, altitude)) {
            return;
        }
        preferences.edit()
                .putBoolean(favoriteKey(slot, "set"), true)
                .putString(favoriteKey(slot, "name"), name)
                .putLong(favoriteKey(slot, "latitude"), Double.doubleToRawLongBits(latitude))
                .putLong(favoriteKey(slot, "longitude"), Double.doubleToRawLongBits(longitude))
                .putLong(favoriteKey(slot, "altitude"), Double.doubleToRawLongBits(altitude))
                .apply();
    }

    void clearFavorite(int slot) {
        if (slot < 0 || slot >= FAVORITE_COUNT) {
            return;
        }
        preferences.edit()
                .remove(favoriteKey(slot, "set"))
                .remove(favoriteKey(slot, "name"))
                .remove(favoriteKey(slot, "latitude"))
                .remove(favoriteKey(slot, "longitude"))
                .remove(favoriteKey(slot, "altitude"))
                .apply();
    }

    private double favoriteDouble(int slot, String field) {
        return Double.longBitsToDouble(preferences.getLong(
                favoriteKey(slot, field),
                Double.doubleToLongBits(Double.NaN)));
    }

    private String favoriteKey(int slot, String field) {
        return "favorite_" + (slot + 1) + "_" + field;
    }

    static boolean validCoordinates(double latitude, double longitude, double altitude) {
        return validHorizontal(latitude, longitude) && Double.isFinite(altitude);
    }

    static boolean validHorizontal(double latitude, double longitude) {
        return Double.isFinite(latitude)
                && Double.isFinite(longitude)
                && latitude >= -90.0
                && latitude <= 90.0
                && longitude >= -180.0
                && longitude <= 180.0;
    }
}
