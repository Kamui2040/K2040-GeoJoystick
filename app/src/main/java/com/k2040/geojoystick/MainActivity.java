package com.k2040.geojoystick;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int REQUEST_MAP = 1001;
    private static final String PREFS = "geojoystick";
    private static final String PREF_LATITUDE = "last_latitude";
    private static final String PREF_LONGITUDE = "last_longitude";
    private static final String PREF_ALTITUDE = "last_altitude";
    private static final String PREF_MANUAL_LATITUDE = "manual_latitude";
    private static final String PREF_MANUAL_LONGITUDE = "manual_longitude";
    private static final String PREF_MANUAL_ALTITUDE = "manual_altitude";
    private static final String PREF_OVERLAY_X = "overlay_x";
    private static final String PREF_OVERLAY_Y = "overlay_y";
    private static final String PREF_APPEARANCE = "app_appearance";
    private static final String PREF_LANGUAGE = "app_language";
    private static final String PREF_RESTORE_LAST_POSITION = "restore_last_position";
    private static final String PREF_OVERLAY_OPACITY = "overlay_opacity_percent";
    private static final String PREF_OVERLAY_HIGH_CONTRAST = "overlay_high_contrast";
    private static final String PREF_CUSTOM_SPEED = "overlay_custom_speed";
    private static final String PREF_CUSTOM_SPEED_NAME = "overlay_custom_speed_name";
    private static final String PREF_LICENSE_ACCEPTED = "license_accepted";
    private static final String APPEARANCE_SYSTEM = "system";
    private static final String APPEARANCE_LIGHT = "light";
    private static final String APPEARANCE_DARK = "dark";
    private static final String LANGUAGE_SYSTEM = "system";
    private static final String LANGUAGE_ENGLISH = "en";
    private static final String LANGUAGE_GERMAN = "de";
    private static final int FAVORITE_COUNT = 5;
    private static final String STATE_CURRENT_PAGE = "current_page";

    private SharedPreferences preferences;
    private EditText latitudeInput;
    private EditText longitudeInput;
    private EditText altitudeInput;
    private TextView statusText;
    private final Button[] favoriteButtons = new Button[FAVORITE_COUNT];
    private boolean pendingStart;
    private String currentPage = "main";
    private boolean darkMode;
    private boolean german;
    private int colorBackground;
    private int colorCard;
    private int colorInput;
    private int colorText;
    private int colorTextDim;
    private int colorBorder;
    private int colorAccent;
    private int colorButton;
    private int colorButtonText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        loadUiSettings();
        buildInterface();
        handleIncomingIntent(getIntent());
        if (!preferences.getBoolean(PREF_LICENSE_ACCEPTED, false)) {
            statusText.post(this::showFirstLaunchDialogIfNeeded);
        } else if (savedInstanceState != null) {
            String restoredPage = savedInstanceState.getString(STATE_CURRENT_PAGE, "main");
            if ("settings".equals(restoredPage)) {
                showSettingsPage();
            } else if ("about".equals(restoredPage)) {
                showAboutPage();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
        if (pendingStart && Settings.canDrawOverlays(this) && isSelectedMockLocationApp()) {
            pendingStart = false;
            startMockingInternal();
        }
    }

    @Override
    protected void onPause() {
        saveVisibleCoordinateFields();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_CURRENT_PAGE, currentPage);
    }

    @Override
    public void onBackPressed() {
        if (!"main".equals(currentPage)) {
            buildInterface();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MAP && resultCode == RESULT_OK && data != null) {
            double lat = data.getDoubleExtra(MapActivity.EXTRA_LATITUDE, getLatitude());
            double lng = data.getDoubleExtra(MapActivity.EXTRA_LONGITUDE, getLongitude());
            setCoordinateInputs(lat, lng, getAltitude());
        }
    }

    private void loadUiSettings() {
        String language = preferences.getString(PREF_LANGUAGE, LANGUAGE_SYSTEM);
        german = LANGUAGE_GERMAN.equals(language)
                || (LANGUAGE_SYSTEM.equals(language) && Locale.getDefault().getLanguage().equals("de"));

        String appearance = preferences.getString(PREF_APPEARANCE, APPEARANCE_SYSTEM);
        darkMode = APPEARANCE_DARK.equals(appearance)
                || (APPEARANCE_SYSTEM.equals(appearance) && isSystemDarkMode());

        if (darkMode) {
            colorBackground = 0xFF10171C;
            colorCard = 0xFF182229;
            colorInput = 0xFF202C34;
            colorText = 0xFFECEFF1;
            colorTextDim = 0xFFB0BEC5;
            colorBorder = 0xFF455A64;
            colorAccent = 0xFF29A8FF;
            colorButton = 0xFF263238;
            colorButtonText = 0xFFECEFF1;
        } else {
            colorBackground = 0xFFF5F7F8;
            colorCard = 0xFFECEFF1;
            colorInput = 0xFFFFFFFF;
            colorText = 0xFF263238;
            colorTextDim = 0xFF546E7A;
            colorBorder = 0xFFB0BEC5;
            colorAccent = 0xFF1976D2;
            colorButton = 0xFFFFFFFF;
            colorButtonText = 0xFF263238;
        }
    }

    private boolean isSystemDarkMode() {
        int nightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private void buildInterface() {
        currentPage = "main";
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(colorBackground);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(24));
        root.setBackgroundColor(colorBackground);
        scrollView.addView(root);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        ImageView avatarButton = new ImageView(this);
        avatarButton.setImageResource(R.drawable.k2040_avatar);
        avatarButton.setContentDescription(t("About K2040", "Über K2040"));
        avatarButton.setAdjustViewBounds(true);
        avatarButton.setPadding(0, 0, 0, 0);
        avatarButton.setOnClickListener(view -> showAboutPage());
        titleRow.addView(avatarButton, new LinearLayout.LayoutParams(dp(46), dp(46)));

        TextView title = sectionText("GeoJoystick", 28, colorText);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 0);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, dp(46), 1f));

        Button settingsCog = fullButton("⚙");
        settingsCog.setTextSize(20);
        settingsCog.setContentDescription(t("Settings", "Einstellungen"));
        settingsCog.setOnClickListener(view -> showSettingsPage());
        titleRow.addView(settingsCog, new LinearLayout.LayoutParams(dp(46), dp(46)));
        root.addView(titleRow, matchWidth());

        statusText = new TextView(this);
        statusText.setTextSize(13);
        statusText.setTextColor(colorText);
        statusText.setPadding(dp(10), dp(9), dp(10), dp(9));
        statusText.setBackground(cardBackground());
        root.addView(statusText, matchWidth());

        double[] initial = loadInitialCoordinates();
        latitudeInput = coordinateInput(t("Latitude", "Breitengrad"), initial[0]);
        longitudeInput = coordinateInput(t("Longitude", "Längengrad"), initial[1]);
        altitudeInput = coordinateInput(t("Altitude (m)", "Höhe (m)"), initial[2]);
        root.addView(latitudeInput, matchWidth());
        root.addView(longitudeInput, matchWidth());
        root.addView(altitudeInput, matchWidth());

        Button saveFavoriteButton = fullButton(t("Save current coordinates as favorite", "Aktuelle Koordinaten als Favorit speichern"));
        saveFavoriteButton.setOnClickListener(view -> saveCurrentAsFavorite());
        root.addView(saveFavoriteButton, matchWidth());

        LinearLayout favoritesRow = new LinearLayout(this);
        favoritesRow.setGravity(Gravity.CENTER);
        for (int i = 0; i < FAVORITE_COUNT; i++) {
            final int slot = i;
            Button favoriteButton = compactFavoriteButton(slot);
            favoriteButton.setOnClickListener(view -> applyFavorite(slot));
            favoriteButton.setOnLongClickListener(view -> {
                editFavorite(slot, true);
                return true;
            });
            favoriteButtons[i] = favoriteButton;
            favoritesRow.addView(favoriteButton, favoriteWeight());
        }
        root.addView(favoritesRow, matchWidth());
        updateFavoriteButtons();

        LinearLayout mapRow = new LinearLayout(this);
        mapRow.setGravity(Gravity.CENTER);
        Button mapButton = fullButton(t("Choose on map", "Auf Karte wählen"));
        mapButton.setOnClickListener(view -> openMap());
        Button importButton = fullButton(t("Import map link from clipboard", "Kartenlink aus Zwischenablage importieren"));
        importButton.setOnClickListener(view -> importFromClipboard());
        mapRow.addView(mapButton, weighted());
        mapRow.addView(importButton, weighted());
        root.addView(mapRow, matchWidth());

        LinearLayout serviceRow = new LinearLayout(this);
        serviceRow.setGravity(Gravity.CENTER);
        Button startButton = fullButton(t("Start overlay", "Overlay starten"));
        startButton.setOnClickListener(view -> startMocking());
        Button stopButton = fullButton(t("Stop overlay", "Overlay stoppen"));
        stopButton.setOnClickListener(view -> stopMocking());
        serviceRow.addView(startButton, weighted());
        serviceRow.addView(stopButton, weighted());
        root.addView(serviceRow, matchWidth());

        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setGravity(Gravity.CENTER);
        Button settingsButton = fullButton(t("Settings", "Einstellungen"));
        settingsButton.setOnClickListener(view -> showSettingsPage());
        Button aboutButton = fullButton(t("About", "Info"));
        aboutButton.setOnClickListener(view -> showAboutPage());
        bottomRow.addView(settingsButton, weighted());
        bottomRow.addView(aboutButton, weighted());
        root.addView(bottomRow, matchWidth());

        Button supportButton = fullButton(t("Support K2040 on Ko-fi", "K2040 auf Ko-fi unterstützen"));
        supportButton.setOnClickListener(view -> openExternalUrl("https://ko-fi.com/k2040"));
        root.addView(supportButton, matchWidth());

        TextView bottomDescription = sectionText(aboutDescriptionText(), 13, colorTextDim);
        bottomDescription.setGravity(Gravity.CENTER);
        bottomDescription.setPadding(dp(4), dp(8), dp(4), 0);
        root.addView(bottomDescription, matchWidth());

        setContentView(scrollView);
        updateStatus();
    }

    private void showSettingsPage() {
        saveVisibleCoordinateFields();
        currentPage = "settings";
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(colorBackground);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(24));
        root.setBackgroundColor(colorBackground);
        scrollView.addView(root);

        TextView title = sectionText(t("Settings", "Einstellungen"), 28, colorText);
        title.setPadding(0, 0, 0, dp(10));
        root.addView(title);

        addSetupSection(root);
        addSettingsSection(root);

        Button backButton = fullButton(t("Back", "Zurück"));
        backButton.setOnClickListener(view -> buildInterface());
        root.addView(backButton, matchWidth());

        setContentView(scrollView);
    }

    private void showAboutPage() {
        saveVisibleCoordinateFields();
        currentPage = "about";
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(colorBackground);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(24));
        root.setBackgroundColor(colorBackground);
        scrollView.addView(root);

        TextView title = sectionText(t("About", "Info"), 28, colorText);
        title.setPadding(0, 0, 0, dp(10));
        root.addView(title);
        root.addView(aboutCardView(false), matchWidth());

        Button licenseButton = fullButton(t("License", "Lizenz"));
        licenseButton.setOnClickListener(view -> openLicenseDialog());
        root.addView(licenseButton, matchWidth());

        Button supportButton = fullButton(t("Support K2040 on Ko-fi", "K2040 auf Ko-fi unterstützen"));
        supportButton.setOnClickListener(view -> openExternalUrl("https://ko-fi.com/k2040"));
        root.addView(supportButton, matchWidth());

        Button backButton = fullButton(t("Back", "Zurück"));
        backButton.setOnClickListener(view -> buildInterface());
        root.addView(backButton, matchWidth());

        setContentView(scrollView);
    }

    private void addSetupSection(LinearLayout root) {
        TextView setupTitle = sectionText(t("Setup", "Einrichtung"), 18, colorText);
        setupTitle.setPadding(0, dp(14), 0, dp(2));
        root.addView(setupTitle);

        LinearLayout setupRow = new LinearLayout(this);
        setupRow.setGravity(Gravity.CENTER);
        Button overlayButton = fullButton(t("Overlay permission", "Overlay-Berechtigung"));
        overlayButton.setOnClickListener(view -> openOverlaySettings());
        Button mockButton = fullButton(t("Mock location settings", "Mock-Standort-Einstellungen"));
        mockButton.setOnClickListener(view -> openDeveloperSettings());
        setupRow.addView(overlayButton, weighted());
        setupRow.addView(mockButton, weighted());
        root.addView(setupRow, matchWidth());

        Button resetOverlayButton = fullButton(t("Reset overlay position", "Overlay-Position zurücksetzen"));
        resetOverlayButton.setOnClickListener(view -> resetOverlayPosition());
        root.addView(resetOverlayButton, matchWidth());
    }

    private void addSettingsSection(LinearLayout root) {
        LinearLayout appearanceLanguageRow = new LinearLayout(this);
        appearanceLanguageRow.setGravity(Gravity.CENTER);
        Button appearanceButton = fullButton(t("Appearance: ", "Darstellung: ") + appearanceLabel());
        appearanceButton.setOnClickListener(view -> chooseAppearance());
        Button languageButton = fullButton(t("Language: ", "Sprache: ") + languageLabel());
        languageButton.setOnClickListener(view -> chooseLanguage());
        appearanceLanguageRow.addView(appearanceButton, weighted());
        appearanceLanguageRow.addView(languageButton, weighted());
        root.addView(appearanceLanguageRow, matchWidth());

        Button restoreButton = fullButton(restoreLastPositionLabel());
        restoreButton.setOnClickListener(view -> toggleRestoreLastPosition());
        root.addView(restoreButton, matchWidth());

        TextView overlaySettingsTitle = sectionText(t("Overlay", "Overlay"), 15, colorText);
        overlaySettingsTitle.setPadding(0, dp(8), 0, 0);
        root.addView(overlaySettingsTitle);

        TextView opacityLabel = sectionText("", 12, colorTextDim);
        SeekBar opacitySlider = new SeekBar(this);
        opacitySlider.setMax(70);
        int opacity = getOverlayOpacity();
        opacitySlider.setProgress(opacity - 30);
        updateOpacityLabel(opacityLabel, opacity);
        opacitySlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = 30 + progress;
                updateOpacityLabel(opacityLabel, value);
                if (fromUser) {
                    preferences.edit().putInt(PREF_OVERLAY_OPACITY, value).apply();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // No-op.
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                preferences.edit().putInt(PREF_OVERLAY_OPACITY, 30 + seekBar.getProgress()).apply();
            }
        });
        root.addView(opacityLabel, matchWidth());
        root.addView(opacitySlider, matchWidth());

        LinearLayout overlayRow = new LinearLayout(this);
        overlayRow.setGravity(Gravity.CENTER);
        Button contrastButton = fullButton(highContrastLabel());
        contrastButton.setOnClickListener(view -> {
            boolean next = !preferences.getBoolean(PREF_OVERLAY_HIGH_CONTRAST, false);
            preferences.edit().putBoolean(PREF_OVERLAY_HIGH_CONTRAST, next).apply();
            contrastButton.setText(highContrastLabel());
        });
        Button customSpeedButton = fullButton(customSpeedLabel());
        customSpeedButton.setOnClickListener(view -> editCustomSpeed(customSpeedButton));
        overlayRow.addView(contrastButton, weighted());
        overlayRow.addView(customSpeedButton, weighted());
        root.addView(overlayRow, matchWidth());
    }

    private String aboutDescriptionText() {
        return t(
                "Created by K2040\nOpen-source, ad-free, account-free mock-location utility.",
                "Erstellt von K2040\nOpen-Source, werbefrei, ohne Konto und ohne Tracking.");
    }

    private LinearLayout aboutCardView(boolean firstLaunch) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        card.setBackground(cardBackground());

        LinearLayout aboutRow = new LinearLayout(this);
        aboutRow.setGravity(Gravity.CENTER_VERTICAL);

        ImageView avatar = new ImageView(this);
        avatar.setImageResource(R.drawable.k2040_avatar);
        avatar.setContentDescription("K2040 avatar");
        avatar.setAdjustViewBounds(true);
        aboutRow.addView(avatar, new LinearLayout.LayoutParams(dp(firstLaunch ? 72 : 64), dp(firstLaunch ? 72 : 64)));

        TextView aboutText = sectionText(aboutDescriptionText(), 13, colorTextDim);
        aboutText.setPadding(dp(12), 0, 0, 0);
        aboutRow.addView(aboutText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(aboutRow);

        if (firstLaunch) {
            TextView licenseNote = sectionText(t(
                    "Please review and accept the GPL-3.0-only license to continue.",
                    "Bitte prüfe und akzeptiere die GPL-3.0-only-Lizenz, um fortzufahren."), 12, colorTextDim);
            licenseNote.setPadding(0, dp(10), 0, 0);
            card.addView(licenseNote);
        }
        return card;
    }

    private AlertDialog.Builder appDialogBuilder() {
        return new AlertDialog.Builder(this, darkMode ? R.style.AppDialogThemeDark : R.style.AppDialogThemeLight);
    }

    private void showFirstLaunchDialogIfNeeded() {
        if (preferences.getBoolean(PREF_LICENSE_ACCEPTED, false) || isFinishing()) {
            return;
        }
        AlertDialog dialog = appDialogBuilder()
                .setTitle(t("About GeoJoystick", "Über GeoJoystick"))
                .setView(aboutCardView(true))
                .setPositiveButton(t("Accept", "Akzeptieren"), null)
                .setNegativeButton(t("Refuse", "Ablehnen"), null)
                .setNeutralButton(t("License", "Lizenz"), null)
                .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnCancelListener(cancel -> finish());
        dialog.setOnShowListener(window -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                preferences.edit().putBoolean(PREF_LICENSE_ACCEPTED, true).apply();
                dialog.dismiss();
            });
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(view -> {
                dialog.dismiss();
                finish();
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view -> openLicenseDialog());
        });
        dialog.show();
    }

    private void openLicenseDialog() {
        LinearLayout dialogRoot = new LinearLayout(this);
        dialogRoot.setOrientation(LinearLayout.VERTICAL);
        dialogRoot.setPadding(dp(14), dp(12), dp(14), dp(10));
        dialogRoot.setBackgroundColor(colorCard);

        TextView title = sectionText("GPL-3.0-only", 20, colorText);
        title.setPadding(dp(2), 0, dp(2), dp(8));
        dialogRoot.addView(title, matchWidth());

        TextView licenseText = sectionText(readAssetText("LICENSE"), 11, colorText);
        licenseText.setTextIsSelectable(true);
        licenseText.setPadding(dp(10), dp(8), dp(10), dp(8));
        licenseText.setBackgroundColor(colorInput);

        ScrollView scroller = new ScrollView(this);
        scroller.setBackground(cardBackground());
        scroller.addView(licenseText);
        int maxHeight = Math.min(dp(560), Math.round(getResources().getDisplayMetrics().heightPixels * 0.70f));
        LinearLayout.LayoutParams scrollerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                maxHeight);
        scrollerParams.setMargins(0, 0, 0, dp(8));
        dialogRoot.addView(scroller, scrollerParams);

        Button closeButton = fullButton(t("Close", "Schließen"));
        dialogRoot.addView(closeButton, matchWidth());

        AlertDialog dialog = appDialogBuilder()
                .setView(dialogRoot)
                .create();
        closeButton.setOnClickListener(view -> dialog.dismiss());
        dialog.setOnShowListener(window -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(colorCard));
            }
        });
        dialog.show();
    }

    private String readAssetText(String name) {
        StringBuilder builder = new StringBuilder();
        try (InputStream stream = getAssets().open(name);
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
        } catch (IOException exception) {
            return t("License text is unavailable in this build.", "Der Lizenztext ist in diesem Build nicht verfügbar.");
        }
        return builder.toString();
    }

    private double[] loadInitialCoordinates() {
        double fallbackLat = Double.longBitsToDouble(preferences.getLong(PREF_LATITUDE, Double.doubleToLongBits(52.520008)));
        double fallbackLng = Double.longBitsToDouble(preferences.getLong(PREF_LONGITUDE, Double.doubleToLongBits(13.404954)));
        double fallbackAlt = Double.longBitsToDouble(preferences.getLong(PREF_ALTITUDE, Double.doubleToLongBits(55.0)));
        return new double[]{
                Double.longBitsToDouble(preferences.getLong(PREF_MANUAL_LATITUDE, Double.doubleToLongBits(fallbackLat))),
                Double.longBitsToDouble(preferences.getLong(PREF_MANUAL_LONGITUDE, Double.doubleToLongBits(fallbackLng))),
                Double.longBitsToDouble(preferences.getLong(PREF_MANUAL_ALTITUDE, Double.doubleToLongBits(fallbackAlt)))
        };
    }

    private void chooseAppearance() {
        String[] labels = new String[]{
                t("System default", "Systemstandard"),
                t("Light", "Hell"),
                t("Dark", "Dunkel")
        };
        String[] values = new String[]{APPEARANCE_SYSTEM, APPEARANCE_LIGHT, APPEARANCE_DARK};
        String current = preferences.getString(PREF_APPEARANCE, APPEARANCE_SYSTEM);
        int checked = indexOf(values, current);
        appDialogBuilder()
                .setTitle(t("Appearance", "Darstellung"))
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    preferences.edit().putString(PREF_APPEARANCE, values[which]).apply();
                    dialog.dismiss();
                    refreshCurrentPageAfterUiSettingChange();
                })
                .setNegativeButton(t("Cancel", "Abbrechen"), null)
                .show();
    }

    private void chooseLanguage() {
        String[] labels = new String[]{"System default", "English", "Deutsch"};
        String[] values = new String[]{LANGUAGE_SYSTEM, LANGUAGE_ENGLISH, LANGUAGE_GERMAN};
        String current = preferences.getString(PREF_LANGUAGE, LANGUAGE_SYSTEM);
        int checked = indexOf(values, current);
        appDialogBuilder()
                .setTitle(t("Language", "Sprache"))
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    preferences.edit().putString(PREF_LANGUAGE, values[which]).apply();
                    dialog.dismiss();
                    refreshCurrentPageAfterUiSettingChange();
                })
                .setNegativeButton(t("Cancel", "Abbrechen"), null)
                .show();
    }

    private void refreshCurrentPageAfterUiSettingChange() {
        String page = currentPage;
        loadUiSettings();
        if ("settings".equals(page)) {
            showSettingsPage();
        } else if ("about".equals(page)) {
            showAboutPage();
        } else {
            buildInterface();
        }
    }

    private int indexOf(String[] values, String current) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(current)) {
                return i;
            }
        }
        return 0;
    }

    private void toggleRestoreLastPosition() {
        saveVisibleCoordinateFields();
        boolean next = !preferences.getBoolean(PREF_RESTORE_LAST_POSITION, true);
        SharedPreferences.Editor editor = preferences.edit().putBoolean(PREF_RESTORE_LAST_POSITION, next);
        if (next) {
            double lat = Double.longBitsToDouble(preferences.getLong(PREF_LATITUDE, Double.doubleToLongBits(52.520008)));
            double lng = Double.longBitsToDouble(preferences.getLong(PREF_LONGITUDE, Double.doubleToLongBits(13.404954)));
            double alt = Double.longBitsToDouble(preferences.getLong(PREF_ALTITUDE, Double.doubleToLongBits(55.0)));
            editor.putLong(PREF_MANUAL_LATITUDE, Double.doubleToRawLongBits(lat))
                    .putLong(PREF_MANUAL_LONGITUDE, Double.doubleToRawLongBits(lng))
                    .putLong(PREF_MANUAL_ALTITUDE, Double.doubleToRawLongBits(alt));
        }
        editor.apply();
        recreate();
    }

    private String appearanceLabel() {
        String value = preferences.getString(PREF_APPEARANCE, APPEARANCE_SYSTEM);
        if (APPEARANCE_LIGHT.equals(value)) {
            return t("Light", "Hell");
        }
        if (APPEARANCE_DARK.equals(value)) {
            return t("Dark", "Dunkel");
        }
        return t("System", "System");
    }

    private String languageLabel() {
        String value = preferences.getString(PREF_LANGUAGE, LANGUAGE_SYSTEM);
        if (LANGUAGE_ENGLISH.equals(value)) {
            return "English";
        }
        if (LANGUAGE_GERMAN.equals(value)) {
            return "Deutsch";
        }
        return t("System", "System");
    }

    private String restoreLastPositionLabel() {
        boolean enabled = preferences.getBoolean(PREF_RESTORE_LAST_POSITION, true);
        return t("Restore last position: ", "Letzte Position wiederherstellen: ")
                + (enabled ? t("On", "Ein") : t("Off", "Aus"));
    }

    private String highContrastLabel() {
        boolean enabled = preferences.getBoolean(PREF_OVERLAY_HIGH_CONTRAST, false);
        return t("High contrast overlay: ", "Overlay mit hohem Kontrast: ")
                + (enabled ? t("On", "Ein") : t("Off", "Aus"));
    }

    private String customSpeedLabel() {
        return t("Custom speed: ", "Eigene Geschwindigkeit: ") + customSpeedName()
                + String.format(Locale.US, " · %.1f m/s", customSpeed());
    }

    private void updateOpacityLabel(TextView label, int opacity) {
        label.setText(t("Overlay opacity: ", "Overlay-Deckkraft: ") + opacity + "%");
    }

    private int getOverlayOpacity() {
        return Math.max(30, Math.min(100, preferences.getInt(PREF_OVERLAY_OPACITY, 85)));
    }

    private String customSpeedName() {
        String name = preferences.getString(PREF_CUSTOM_SPEED_NAME, "Custom");
        if (name == null || name.trim().isEmpty()) {
            return "Custom";
        }
        return name.trim();
    }

    private double customSpeed() {
        return clampSpeed(Double.longBitsToDouble(
                preferences.getLong(PREF_CUSTOM_SPEED, Double.doubleToLongBits(5.0))));
    }

    private void editCustomSpeed(Button sourceButton) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(8), 0, dp(8), 0);

        EditText nameInput = textInput(t("Name", "Name"), customSpeedName());
        EditText speedInput = coordinateInput(t("Speed in m/s", "Geschwindigkeit in m/s"), customSpeed());
        TextView note = sectionText(String.format(
                Locale.US,
                t("%.1f m/s = %.1f km/h", "%.1f m/s = %.1f km/h"),
                customSpeed(),
                customSpeed() * 3.6), 12, colorTextDim);
        form.addView(nameInput, matchWidth());
        form.addView(speedInput, matchWidth());
        form.addView(note, matchWidth());

        AlertDialog dialog = appDialogBuilder()
                .setTitle(t("Custom speed", "Eigene Geschwindigkeit"))
                .setView(form)
                .setPositiveButton(t("Save", "Speichern"), null)
                .setNegativeButton(t("Cancel", "Abbrechen"), null)
                .create();
        dialog.setOnShowListener(window -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            try {
                double value = clampSpeed(Double.parseDouble(speedInput.getText().toString().trim()));
                String name = nameInput.getText().toString().trim();
                if (name.isEmpty()) {
                    name = "Custom";
                }
                SharedPreferences.Editor editor = preferences.edit()
                        .putString(PREF_CUSTOM_SPEED_NAME, name)
                        .putLong(PREF_CUSTOM_SPEED, Double.doubleToRawLongBits(value));
                editor.apply();
                sourceButton.setText(customSpeedLabel());
                dialog.dismiss();
            } catch (NumberFormatException exception) {
                Toast.makeText(this, t("Enter a valid speed", "Gib eine gültige Geschwindigkeit ein"), Toast.LENGTH_SHORT).show();
            }
        }));
        dialog.show();
    }

    private double clampSpeed(double value) {
        if (!Double.isFinite(value)) {
            return 5.0;
        }
        return Math.max(0.1, Math.min(50.0, value));
    }

    private void saveCurrentAsFavorite() {
        if (!validCoordinates()) {
            return;
        }
        String[] slots = new String[FAVORITE_COUNT];
        for (int i = 0; i < FAVORITE_COUNT; i++) {
            slots[i] = favoriteDisplayName(i);
        }
        appDialogBuilder()
                .setTitle(t("Choose favorite slot", "Favoritenplatz wählen"))
                .setItems(slots, (dialog, which) -> editFavorite(which, false))
                .setNegativeButton(t("Cancel", "Abbrechen"), null)
                .show();
    }

    private void applyFavorite(int slot) {
        if (!isFavoriteSet(slot)) {
            if (!validCoordinates()) {
                return;
            }
            editFavorite(slot, false);
            return;
        }
        double lat = favoriteDouble(slot, "latitude", getLatitude());
        double lng = favoriteDouble(slot, "longitude", getLongitude());
        double alt = favoriteDouble(slot, "altitude", getAltitude());
        setCoordinateInputs(lat, lng, alt);
        Toast.makeText(this, t("Favorite loaded into coordinate fields", "Favorit in Koordinatenfelder geladen"), Toast.LENGTH_SHORT).show();
    }

    private void editFavorite(int slot, boolean useSavedValues) {
        if (!useSavedValues && !validCoordinates()) {
            return;
        }
        double fallbackLat = safeLatitude();
        double fallbackLng = safeLongitude();
        double fallbackAlt = safeAltitude();
        double lat = useSavedValues && isFavoriteSet(slot) ? favoriteDouble(slot, "latitude", fallbackLat) : fallbackLat;
        double lng = useSavedValues && isFavoriteSet(slot) ? favoriteDouble(slot, "longitude", fallbackLng) : fallbackLng;
        double alt = useSavedValues && isFavoriteSet(slot) ? favoriteDouble(slot, "altitude", fallbackAlt) : fallbackAlt;
        String name = useSavedValues && isFavoriteSet(slot) ? favoriteName(slot) : t("Favorite ", "Favorit ") + (slot + 1);

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(8), 0, dp(8), 0);
        EditText nameInput = textInput(t("Favorite name", "Favoritenname"), name);
        EditText latInput = coordinateInput(t("Latitude", "Breitengrad"), lat);
        EditText lngInput = coordinateInput(t("Longitude", "Längengrad"), lng);
        EditText altInput = coordinateInput(t("Altitude (m)", "Höhe (m)"), alt);
        form.addView(nameInput, matchWidth());
        form.addView(latInput, matchWidth());
        form.addView(lngInput, matchWidth());
        form.addView(altInput, matchWidth());

        AlertDialog dialog = appDialogBuilder()
                .setTitle(t("Favorite ", "Favorit ") + (slot + 1))
                .setView(form)
                .setPositiveButton(t("Save", "Speichern"), null)
                .setNegativeButton(t("Cancel", "Abbrechen"), null)
                .setNeutralButton(t("Clear", "Leeren"), null)
                .create();
        dialog.setOnShowListener(window -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                try {
                    double favoriteLat = Double.parseDouble(latInput.getText().toString().trim());
                    double favoriteLng = Double.parseDouble(lngInput.getText().toString().trim());
                    double favoriteAlt = Double.parseDouble(altInput.getText().toString().trim());
                    if (!validCoordinateValues(favoriteLat, favoriteLng, favoriteAlt)) {
                        throw new NumberFormatException("Favorite coordinate out of range");
                    }
                    String favoriteName = nameInput.getText().toString().trim();
                    if (favoriteName.isEmpty()) {
                        favoriteName = t("Favorite ", "Favorit ") + (slot + 1);
                    }
                    preferences.edit()
                            .putBoolean(favoriteKey(slot, "set"), true)
                            .putString(favoriteKey(slot, "name"), favoriteName)
                            .putLong(favoriteKey(slot, "latitude"), Double.doubleToRawLongBits(favoriteLat))
                            .putLong(favoriteKey(slot, "longitude"), Double.doubleToRawLongBits(favoriteLng))
                            .putLong(favoriteKey(slot, "altitude"), Double.doubleToRawLongBits(favoriteAlt))
                            .apply();
                    updateFavoriteButtons();
                    dialog.dismiss();
                } catch (NumberFormatException exception) {
                    Toast.makeText(this, t("Enter valid favorite coordinates", "Gib gültige Favoriten-Koordinaten ein"), Toast.LENGTH_SHORT).show();
                }
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view -> {
                clearFavorite(slot);
                updateFavoriteButtons();
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private void updateFavoriteButtons() {
        for (int i = 0; i < FAVORITE_COUNT; i++) {
            favoriteButtons[i].setText(favoriteDisplayName(i));
            favoriteButtons[i].setContentDescription(favoriteButtonDescription(i));
        }
    }

    private String favoriteDisplayName(int slot) {
        if (isFavoriteSet(slot)) {
            String name = favoriteName(slot);
            return name.length() > 9 ? name.substring(0, 9) : name;
        }
        return t("Fav ", "Fav ") + (slot + 1);
    }

    private String favoriteButtonDescription(int slot) {
        if (isFavoriteSet(slot)) {
            return favoriteName(slot);
        }
        return t("Empty favorite ", "Leerer Favorit ") + (slot + 1);
    }

    private boolean isFavoriteSet(int slot) {
        return preferences.getBoolean(favoriteKey(slot, "set"), false);
    }

    private String favoriteName(int slot) {
        return preferences.getString(favoriteKey(slot, "name"), t("Favorite ", "Favorit ") + (slot + 1));
    }

    private double favoriteDouble(int slot, String field, double fallback) {
        return Double.longBitsToDouble(preferences.getLong(favoriteKey(slot, field), Double.doubleToLongBits(fallback)));
    }

    private String favoriteKey(int slot, String field) {
        return "favorite_" + (slot + 1) + "_" + field;
    }

    private void clearFavorite(int slot) {
        preferences.edit()
                .remove(favoriteKey(slot, "set"))
                .remove(favoriteKey(slot, "name"))
                .remove(favoriteKey(slot, "latitude"))
                .remove(favoriteKey(slot, "longitude"))
                .remove(favoriteKey(slot, "altitude"))
                .apply();
    }

    private void openExternalUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        try {
            startActivity(intent);
        } catch (RuntimeException exception) {
            Toast.makeText(this, t("No browser app available", "Keine Browser-App verfügbar"), Toast.LENGTH_SHORT).show();
        }
    }

    private EditText coordinateInput(String hint, double value) {
        EditText input = textInput(hint, String.format(Locale.US, "%.6f", value));
        input.setSelectAllOnFocus(true);
        input.setInputType(InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_DECIMAL
                | InputType.TYPE_NUMBER_FLAG_SIGNED);
        return input;
    }

    private EditText textInput(String hint, String value) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setText(value);
        input.setTextSize(14);
        input.setTextColor(colorText);
        input.setHintTextColor(colorTextDim);
        input.setPadding(dp(10), dp(10), dp(10), dp(10));
        input.setBackground(cardBackground());
        return input;
    }

    private Button fullButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(13);
        button.setTextColor(colorButtonText);
        button.setMinHeight(0);
        button.setPadding(dp(8), dp(7), dp(8), dp(7));
        button.setBackground(buttonBackground(false));
        return button;
    }

    private Button compactFavoriteButton(int slot) {
        Button button = fullButton(favoriteDisplayName(slot));
        button.setTextSize(10);
        button.setSingleLine(true);
        button.setPadding(0, dp(5), 0, dp(5));
        return button;
    }

    private TextView sectionText(String text, int sizeSp, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        return view;
    }

    private GradientDrawable cardBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(colorCard);
        drawable.setCornerRadius(dp(6));
        drawable.setStroke(dp(1), colorBorder);
        return drawable;
    }

    private GradientDrawable buttonBackground(boolean active) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(active ? colorAccent : colorButton);
        drawable.setCornerRadius(dp(6));
        drawable.setStroke(dp(1), active ? colorAccent : colorBorder);
        return drawable;
    }

    private LinearLayout.LayoutParams matchWidth() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(5), 0, dp(5));
        return params;
    }

    private LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f);
        params.setMargins(dp(2), dp(4), dp(2), dp(4));
        return params;
    }

    private LinearLayout.LayoutParams favoriteWeight() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f);
        params.setMargins(dp(1), dp(2), dp(1), dp(2));
        return params;
    }

    private void updateStatus() {
        if (statusText == null) {
            return;
        }
        boolean overlayGranted = Settings.canDrawOverlays(this);
        boolean mockSelected = isSelectedMockLocationApp();
        statusText.setText(String.format(
                Locale.US,
                t("Overlay: %s     Mock location app: %s", "Overlay: %s     Mock-Standort-App: %s"),
                overlayGranted ? t("ready", "bereit") : t("not granted", "nicht erlaubt"),
                mockSelected ? t("selected", "ausgewählt") : t("not selected", "nicht ausgewählt")));
        statusText.setTextColor(overlayGranted && mockSelected ? 0xFF2E7D32 : 0xFFC62828);
    }

    private void startMocking() {
        if (!validCoordinates()) {
            return;
        }
        pendingStart = true;
        if (!Settings.canDrawOverlays(this)) {
            openOverlaySettings();
            return;
        }
        if (!isSelectedMockLocationApp()) {
            appDialogBuilder()
                    .setTitle(t("Select GeoJoystick", "GeoJoystick auswählen"))
                    .setMessage(t(
                            "In Developer options, choose GeoJoystick under Select mock location app, then return here.",
                            "Wähle in den Entwickleroptionen GeoJoystick unter Mock-Standort-App auswählen und kehre dann hierher zurück."))
                    .setPositiveButton(t("Open settings", "Einstellungen öffnen"), (dialog, which) -> openDeveloperSettings())
                    .setNegativeButton(t("Cancel", "Abbrechen"), (dialog, which) -> pendingStart = false)
                    .show();
            return;
        }
        pendingStart = false;
        startMockingInternal();
    }

    private void startMockingInternal() {
        double lat = getLatitude();
        double lng = getLongitude();
        double alt = getAltitude();
        saveCoordinates(lat, lng, alt);
        requestNotificationPermissionIfNeeded();

        Intent intent = new Intent(this, MockLocationService.class)
                .setAction(MockLocationService.ACTION_START)
                .putExtra(MockLocationService.EXTRA_LATITUDE, lat)
                .putExtra(MockLocationService.EXTRA_LONGITUDE, lng)
                .putExtra(MockLocationService.EXTRA_ALTITUDE, alt);
        startForegroundService(intent);
        Toast.makeText(this, t("GeoJoystick started", "GeoJoystick gestartet"), Toast.LENGTH_SHORT).show();
    }

    private void stopMocking() {
        Intent intent = new Intent(this, MockLocationService.class)
                .setAction(MockLocationService.ACTION_STOP);
        startService(intent);
    }

    private void resetOverlayPosition() {
        preferences.edit()
                .remove(PREF_OVERLAY_X)
                .remove(PREF_OVERLAY_Y)
                .apply();
        Toast.makeText(this, t("Overlay position reset for next show", "Overlay-Position für die nächste Anzeige zurückgesetzt"), Toast.LENGTH_SHORT).show();
    }

    private void openMap() {
        if (!validCoordinates()) {
            return;
        }
        Intent intent = new Intent(this, MapActivity.class)
                .putExtra(MapActivity.EXTRA_LATITUDE, getLatitude())
                .putExtra(MapActivity.EXTRA_LONGITUDE, getLongitude());
        startActivityForResult(intent, REQUEST_MAP);
    }

    private void importFromClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null || !clipboard.hasPrimaryClip()) {
            Toast.makeText(this, t("Clipboard is empty", "Zwischenablage ist leer"), Toast.LENGTH_SHORT).show();
            return;
        }
        ClipData clip = clipboard.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) {
            Toast.makeText(this, t("Clipboard is empty", "Zwischenablage ist leer"), Toast.LENGTH_SHORT).show();
            return;
        }
        CharSequence text = clip.getItemAt(0).coerceToText(this);
        importLocationText(text == null ? null : text.toString());
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())) {
            return;
        }
        CharSequence text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        if (text != null) {
            importLocationText(text.toString());
        }
    }

    private void importLocationText(String text) {
        if (text == null || text.trim().isEmpty()) {
            Toast.makeText(this, t("No location link found", "Kein Standortlink gefunden"), Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, t("Reading location link…", "Standortlink wird gelesen…"), Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            double[] coordinates = LocationLinkParser.resolveCoordinates(text);
            runOnUiThread(() -> {
                if (coordinates == null) {
                    Toast.makeText(this, t("Could not extract coordinates from that link", "Aus diesem Link konnten keine Koordinaten gelesen werden"), Toast.LENGTH_LONG).show();
                } else {
                    setCoordinateInputs(coordinates[0], coordinates[1], getAltitude());
                    Toast.makeText(this, t("Coordinates imported", "Koordinaten importiert"), Toast.LENGTH_SHORT).show();
                }
            });
        }, "MapLinkResolver").start();
    }

    private void openInMaps() {
        if (!validCoordinates()) {
            return;
        }
        double lat = getLatitude();
        double lng = getLongitude();
        Intent intent = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse(String.format(Locale.US, "geo:%.8f,%.8f?q=%.8f,%.8f", lat, lng, lat, lng)));
        try {
            startActivity(intent);
        } catch (RuntimeException noMapApp) {
            openExternalUrl(String.format(
                    Locale.US,
                    "https://www.openstreetmap.org/?mlat=%.8f&mlon=%.8f#map=17/%.8f/%.8f",
                    lat,
                    lng,
                    lat,
                    lng));
        }
    }

    private void openOverlaySettings() {
        Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void openDeveloperSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        } catch (RuntimeException exception) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private boolean isSelectedMockLocationApp() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        if (appOps == null) {
            return false;
        }
        int mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_MOCK_LOCATION,
                Process.myUid(),
                getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2040);
        }
    }

    private boolean validCoordinates() {
        try {
            double lat = getLatitude();
            double lng = getLongitude();
            double alt = getAltitude();
            if (!validCoordinateValues(lat, lng, alt)) {
                throw new NumberFormatException("Coordinate out of range");
            }
            return true;
        } catch (NumberFormatException exception) {
            Toast.makeText(this, t("Enter valid latitude, longitude, and altitude", "Gib gültige Werte für Breitengrad, Längengrad und Höhe ein"), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private boolean validCoordinateValues(double lat, double lng, double alt) {
        return Double.isFinite(lat) && Double.isFinite(lng) && Double.isFinite(alt)
                && lat >= -90.0 && lat <= 90.0 && lng >= -180.0 && lng <= 180.0;
    }

    private double getLatitude() {
        return Double.parseDouble(latitudeInput.getText().toString().trim());
    }

    private double getLongitude() {
        return Double.parseDouble(longitudeInput.getText().toString().trim());
    }

    private double getAltitude() {
        return Double.parseDouble(altitudeInput.getText().toString().trim());
    }

    private double safeLatitude() {
        try {
            return getLatitude();
        } catch (NumberFormatException exception) {
            return 52.520008;
        }
    }

    private double safeLongitude() {
        try {
            return getLongitude();
        } catch (NumberFormatException exception) {
            return 13.404954;
        }
    }

    private double safeAltitude() {
        try {
            return getAltitude();
        } catch (NumberFormatException exception) {
            return 55.0;
        }
    }

    private void setCoordinateInputs(double latitude, double longitude, double altitude) {
        latitudeInput.setText(String.format(Locale.US, "%.6f", latitude));
        longitudeInput.setText(String.format(Locale.US, "%.6f", longitude));
        altitudeInput.setText(String.format(Locale.US, "%.6f", altitude));
        saveManualCoordinates(latitude, longitude, altitude);
    }

    private void saveVisibleCoordinateFields() {
        if (latitudeInput == null || longitudeInput == null || altitudeInput == null) {
            return;
        }
        try {
            double lat = getLatitude();
            double lng = getLongitude();
            double alt = getAltitude();
            if (validCoordinateValues(lat, lng, alt)) {
                saveManualCoordinates(lat, lng, alt);
            }
        } catch (NumberFormatException ignored) {
            // Keep the last valid saved values while the user is editing invalid text.
        }
    }

    private void saveManualCoordinates(double latitude, double longitude, double altitude) {
        preferences.edit()
                .putLong(PREF_MANUAL_LATITUDE, Double.doubleToRawLongBits(latitude))
                .putLong(PREF_MANUAL_LONGITUDE, Double.doubleToRawLongBits(longitude))
                .putLong(PREF_MANUAL_ALTITUDE, Double.doubleToRawLongBits(altitude))
                .apply();
    }

    private void saveCoordinates(double latitude, double longitude, double altitude) {
        preferences.edit()
                .putLong(PREF_MANUAL_LATITUDE, Double.doubleToRawLongBits(latitude))
                .putLong(PREF_MANUAL_LONGITUDE, Double.doubleToRawLongBits(longitude))
                .putLong(PREF_MANUAL_ALTITUDE, Double.doubleToRawLongBits(altitude))
                .putLong(PREF_LATITUDE, Double.doubleToRawLongBits(latitude))
                .putLong(PREF_LONGITUDE, Double.doubleToRawLongBits(longitude))
                .putLong(PREF_ALTITUDE, Double.doubleToRawLongBits(altitude))
                .apply();
    }

    private String t(String english, String germanText) {
        return german ? germanText : english;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
