/*
 * SPDX-License-Identifier: GPL-3.0-only
 * K2040-authored portions: Copyright (c) 2026 K2040.
 * K2040-authored material in this file is also subject to the GPLv3 section 7(b)
 * attribution-preservation term in LICENSES/GPL-3.0-Section-7b-K2040.txt.
 * Upstream and third-party material retains its own notices; see NOTICE.md.
 */
package com.k2040.geojoystick;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
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
    private static final int REQUEST_NOTIFICATIONS = 2040;
    private static final String STATE_CURRENT_PAGE = "current_page";
    private static final String STATE_DRAFT_INITIALIZED = "draft_initialized";
    private static final String STATE_DRAFT_LATITUDE = "draft_latitude";
    private static final String STATE_DRAFT_LONGITUDE = "draft_longitude";
    private static final String STATE_DRAFT_ALTITUDE = "draft_altitude";
    private static final String STATE_INCOMING_INTENT_CONSUMED = "incoming_intent_consumed";

    private GeoSettings settings;
    private GeoUi.Palette palette;
    private boolean german;
    private EditText latitudeInput;
    private EditText longitudeInput;
    private EditText altitudeInput;
    private TextView mockStatus;
    private TextView overlayStatus;
    private TextView simulationStatus;
    private Button simulationStartButton;
    private Button simulationStopButton;
    private final Button[] favoriteButtons = new Button[GeoSettings.FAVORITE_COUNT];
    private boolean pendingStart;
    private boolean receiverRegistered;
    private boolean incomingIntentConsumed;
    private volatile int importRequestId;
    private boolean draftInitialized;
    private double draftLatitude = Double.NaN;
    private double draftLongitude = Double.NaN;
    private double draftAltitude = Double.NaN;
    private String currentPage = "main";
    private Object backInvokedCallback;
    private boolean backInvokedCallbackRegistered;

    private final BroadcastReceiver simulationStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null
                    && MockLocationService.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                updateStatus();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = new GeoSettings(this);
        loadUiSettings();
        restoreDraft(savedInstanceState);
        incomingIntentConsumed = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_INCOMING_INTENT_CONSUMED, false);

        if (!settings.welcomeAcknowledged()) {
            showWelcomePage();
            return;
        }

        showHomePage();
        handleIncomingIntent(getIntent());
        if (savedInstanceState != null) {
            restorePage(savedInstanceState.getString(STATE_CURRENT_PAGE, "main"));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        incomingIntentConsumed = false;
        if (!settings.welcomeAcknowledged()) {
            return;
        }
        handleIncomingIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerSimulationStateReceiver();
        if ("settings".equals(currentPage)) {
            showSettingsPage();
        } else {
            updateStatus();
        }
        if (pendingStart && Settings.canDrawOverlays(this) && isSelectedMockLocationApp()) {
            pendingStart = false;
            startMockingInternal();
        }
    }

    @Override
    protected void onPause() {
        saveVisibleCoordinates();
        unregisterSimulationStateReceiver();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        importRequestId++;
        unregisterSimulationStateReceiver();
        updateBackCallbackRegistration(false);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        saveVisibleCoordinates();
        super.onSaveInstanceState(outState);
        outState.putString(STATE_CURRENT_PAGE, currentPage);
        outState.putBoolean(STATE_DRAFT_INITIALIZED, draftInitialized);
        outState.putBoolean(STATE_INCOMING_INTENT_CONSUMED, incomingIntentConsumed);
        if (draftInitialized) {
            outState.putDouble(STATE_DRAFT_LATITUDE, draftLatitude);
            outState.putDouble(STATE_DRAFT_LONGITUDE, draftLongitude);
            outState.putDouble(STATE_DRAFT_ALTITUDE, draftAltitude);
        }
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            super.onBackPressed();
            return;
        }
        handleBackNavigation();
    }

    private void handleBackNavigation() {
        if ("license-text".equals(currentPage)
                || "license-osm".equals(currentPage)
                || "license-artwork".equals(currentPage)) {
            showLicensePage(false);
            return;
        }
        if ("license-text-welcome".equals(currentPage)
                || "license-osm-welcome".equals(currentPage)
                || "license-artwork-welcome".equals(currentPage)) {
            showLicensePage(true);
            return;
        }
        if ("license-welcome".equals(currentPage)) {
            showWelcomePage();
            return;
        }
        if ("license-about".equals(currentPage)
                || "changelog-about".equals(currentPage)
                || "sources-about".equals(currentPage)) {
            showAboutPage();
            return;
        }
        if ("settings".equals(currentPage) || "about".equals(currentPage)) {
            showHomePage();
            return;
        }
        if (!"welcome".equals(currentPage)) {
            super.onBackPressed();
        }
    }

    private void updateBackCallbackRegistration(boolean shouldRegister) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;

        if (backInvokedCallback == null) {
            backInvokedCallback = Api33BackNavigation.create(this::handleBackNavigation);
        }

        if (shouldRegister && !backInvokedCallbackRegistered) {
            Api33BackNavigation.register(this, backInvokedCallback);
            backInvokedCallbackRegistered = true;
        } else if (!shouldRegister && backInvokedCallbackRegistered) {
            Api33BackNavigation.unregister(this, backInvokedCallback);
            backInvokedCallbackRegistered = false;
        }
    }

    @android.annotation.TargetApi(Build.VERSION_CODES.TIRAMISU)
    private static final class Api33BackNavigation {
        private Api33BackNavigation() { }

        static Object create(Runnable action) {
            return (android.window.OnBackInvokedCallback) action::run;
        }

        static void register(Activity activity, Object callback) {
            activity.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    (android.window.OnBackInvokedCallback) callback);
        }

        static void unregister(Activity activity, Object callback) {
            activity.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(
                    (android.window.OnBackInvokedCallback) callback);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_MAP || resultCode != RESULT_OK || data == null) {
            return;
        }
        if (!data.hasExtra(MapActivity.EXTRA_LATITUDE)
                || !data.hasExtra(MapActivity.EXTRA_LONGITUDE)) {
            toast(t("The map returned no valid coordinates",
                    "Die Karte hat keine gültigen Koordinaten zurückgegeben"), true);
            return;
        }
        double latitude = data.getDoubleExtra(MapActivity.EXTRA_LATITUDE, Double.NaN);
        double longitude = data.getDoubleExtra(MapActivity.EXTRA_LONGITUDE, Double.NaN);
        if (!setHorizontalCoordinates(latitude, longitude)) {
            toast(t("The map returned invalid coordinates",
                    "Die Karte hat ungültige Koordinaten zurückgegeben"), true);
            return;
        }
        if (!Double.isFinite(safeAltitude())) {
            toast(t("Location selected. Enter an altitude before starting.",
                    "Standort ausgewählt. Gib vor dem Start eine Höhe ein."), true);
        }
    }

    private void restorePage(String page) {
        if ("settings".equals(page)) {
            showSettingsPage();
        } else if ("about".equals(page)) {
            showAboutPage();
        } else if ("changelog".equals(page) || "changelog-about".equals(page)) {
            showChangelogPage(false);
        } else if ("license-about".equals(page)) {
            showLicensePage(false);
        } else if ("license-text".equals(page)) {
            showLicenseTextPage();
        } else if ("license-osm".equals(page)) {
            showOsmLicensePage();
        } else if ("license-artwork".equals(page)) {
            showArtworkLicensePage(false);
        } else if ("sources-about".equals(page)) {
            showSourcesPage();
        }
    }

    private void loadUiSettings() {
        german = settings.isGerman();
        palette = new GeoUi.Palette(settings.isDark());
        getWindow().setStatusBarColor(palette.background);
        getWindow().setNavigationBarColor(palette.background);
    }

    private void showHomePage() {
        currentPage = "main";
        ScrollView page = buildHomePage();
        setContentView(page);
        updateBackCallbackRegistration(false);
        applySystemBarInsets(page);
        updateStatus();
    }

    private ScrollView buildHomePage() {
        ScrollView page = pageScroll();
        LinearLayout root = pageRoot();
        page.addView(root);
        root.addView(appHeader(), margin(0, 2));

        LinearLayout status = card();
        status.setPadding(dp(12), 0, dp(12), 0);

        LinearLayout statusHeader = new LinearLayout(this);
        statusHeader.setGravity(Gravity.CENTER_VERTICAL);
        statusHeader.setMinimumHeight(dp(48));
        statusHeader.setPadding(dp(2), 0, dp(2), 0);
        statusHeader.setClickable(true);
        statusHeader.setFocusable(true);

        TextView statusTitle = text(t("Status", "Status"), 13, palette.text, true);
        statusTitle.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        statusTitle.setIncludeFontPadding(false);
        statusTitle.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        statusHeader.addView(statusTitle,
                new LinearLayout.LayoutParams(0, dp(48), 1f));

        TextView statusIndicator = text("⌄", 18, palette.textDim, false);
        statusIndicator.setGravity(Gravity.CENTER);
        statusIndicator.setIncludeFontPadding(false);
        statusIndicator.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        statusHeader.addView(statusIndicator,
                new LinearLayout.LayoutParams(dp(32), dp(48)));

        LinearLayout statusDetails = new LinearLayout(this);
        statusDetails.setOrientation(LinearLayout.VERTICAL);
        statusDetails.setPadding(0, 0, 0, dp(4));
        statusDetails.setVisibility(View.GONE);
        mockStatus = addStatusRow(statusDetails, t("Mock location", "Mock-Standort"));
        overlayStatus = addStatusRow(statusDetails, t("Overlay permission", "Overlay-Berechtigung"));
        simulationStatus = addStatusRow(statusDetails, t("Simulation", "Simulation"));

        String collapsedStatusDescription = t(
                "Status collapsed. Tap to expand",
                "Status eingeklappt. Zum Öffnen tippen");
        String expandedStatusDescription = t(
                "Status expanded. Tap to collapse",
                "Status ausgeklappt. Zum Schließen tippen");
        statusHeader.setContentDescription(collapsedStatusDescription);
        statusHeader.setOnClickListener(view -> {
            boolean expand = statusDetails.getVisibility() != View.VISIBLE;
            statusDetails.setVisibility(expand ? View.VISIBLE : View.GONE);
            statusIndicator.setText(expand ? "⌃" : "⌄");
            statusHeader.setContentDescription(
                    expand ? expandedStatusDescription : collapsedStatusDescription);
        });

        status.addView(statusHeader);
        status.addView(statusDetails);
        root.addView(status, margin(2, 4));

        LinearLayout coordinates = card();
        addCardTitle(coordinates, t("Coordinates", "Koordinaten"));
        double[] initial = initialCoordinates();
        latitudeInput = coordinateInput(t("Latitude", "Breitengrad"), initial[0]);
        longitudeInput = coordinateInput(t("Longitude", "Längengrad"), initial[1]);
        altitudeInput = coordinateInput(t("Altitude (m)", "Höhe (m)"), initial[2]);
        coordinates.addView(latitudeInput, innerRow());
        coordinates.addView(longitudeInput, innerRow());
        coordinates.addView(altitudeInput, innerRow());
        root.addView(coordinates, margin(2, 4));

        LinearLayout quick = new LinearLayout(this);
        quick.setOrientation(LinearLayout.HORIZONTAL);
        quick.setGravity(Gravity.CENTER);
        Button map = actionTile("⌖", t("Map", "Karte"));
        map.setOnClickListener(view -> openMap());
        Button paste = actionTile("↓", t("Paste link", "Link einfügen"));
        paste.setOnClickListener(view -> importFromClipboard());
        Button favorite = actionTile("☆", t("Favorite", "Favorit"));
        favorite.setOnClickListener(view -> chooseFavoriteSlot());
        quick.addView(map, tileWeight());
        quick.addView(paste, tileWeight());
        quick.addView(favorite, tileWeight());
        root.addView(quick, margin(1, 3));

        LinearLayout favoriteCard = card();
        LinearLayout favoriteHeader = new LinearLayout(this);
        favoriteHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView favoriteTitle = text(t("Favorites", "Favoriten"), 13, palette.text, true);
        favoriteHeader.addView(favoriteTitle,
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView favoriteHint = text(t("Tap · hold to edit",
                "Tippen · halten zum Bearbeiten"), 9, palette.textDim, false);
        favoriteHeader.addView(favoriteHint);
        favoriteCard.addView(favoriteHeader, innerRow());

        HorizontalScrollView scroller = new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        LinearLayout favoriteRow = new LinearLayout(this);
        favoriteRow.setOrientation(LinearLayout.HORIZONTAL);
        favoriteRow.setPadding(0, dp(2), 0, 0);
        for (int slot = 0; slot < GeoSettings.FAVORITE_COUNT; slot++) {
            final int index = slot;
            Button button = favoriteButton(index);
            button.setOnClickListener(view -> applyFavorite(index));
            button.setOnLongClickListener(view -> {
                editFavorite(index, true);
                return true;
            });
            favoriteButtons[index] = button;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(100), dp(52));
            params.rightMargin = dp(6);
            favoriteRow.addView(button, params);
        }
        scroller.addView(favoriteRow);
        favoriteCard.addView(scroller, innerRow());
        root.addView(favoriteCard, margin(2, 4));
        refreshFavoriteButtons();

        LinearLayout simulationCard = card();
        simulationCard.setPadding(dp(12), dp(4), dp(12), dp(4));

        LinearLayout simulationRow = new LinearLayout(this);
        simulationRow.setGravity(Gravity.CENTER);

        TextView simulationTitle = text(t("Simulation", "Simulation"),
                13, palette.text, true);
        simulationTitle.setGravity(Gravity.CENTER_VERTICAL);
        simulationTitle.setIncludeFontPadding(false);
        simulationTitle.setPadding(0, 0, dp(8), 0);
        simulationRow.addView(simulationTitle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(48)));

        simulationStartButton = GeoUi.button(this, palette, "▶", true);
        simulationStartButton.setTextSize(18);
        simulationStartButton.setMinWidth(0);
        simulationStartButton.setMinimumWidth(0);
        simulationStartButton.setContentDescription(
                t("Start simulation", "Simulation starten"));
        simulationStartButton.setOnClickListener(view -> startMocking());

        simulationStopButton = GeoUi.button(this, palette, "■", false);
        simulationStopButton.setTextSize(18);
        simulationStopButton.setTextColor(palette.danger);
        simulationStopButton.setMinWidth(0);
        simulationStopButton.setMinimumWidth(0);
        simulationStopButton.setContentDescription(
                t("Stop simulation", "Simulation stoppen"));
        simulationStopButton.setOnClickListener(view -> stopMocking());

        LinearLayout.LayoutParams simulationButtonParams =
                new LinearLayout.LayoutParams(dp(48), dp(48));
        simulationButtonParams.leftMargin = dp(2);
        simulationButtonParams.rightMargin = dp(2);
        simulationRow.addView(simulationStartButton, simulationButtonParams);

        LinearLayout.LayoutParams stopButtonParams =
                new LinearLayout.LayoutParams(dp(48), dp(48));
        stopButtonParams.leftMargin = dp(2);
        stopButtonParams.rightMargin = dp(2);
        simulationRow.addView(simulationStopButton, stopButtonParams);

        simulationCard.addView(simulationRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        root.addView(simulationCard, margin(5, 3));

        TextView footer = text(t("Open source · GPL-3.0-only · Local-first",
                "Open Source · GPL-3.0-only · Lokal"), 10, palette.textDim, false);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(dp(8), dp(6), dp(8), dp(2));
        footer.setOnClickListener(view -> showAboutPage());
        root.addView(footer, margin(1, 2));
        return page;
    }

    private LinearLayout appHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(2), dp(2), dp(2), dp(4));

        ImageView avatar = new ImageView(this);
        avatar.setImageResource(R.drawable.geojoystick_mascot);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatar.setPadding(dp(4), dp(4), dp(4), dp(4));
        avatar.setContentDescription(t("About GeoJoystick", "Über GeoJoystick"));
        avatar.setOnClickListener(view -> showAboutPage());
        header.addView(avatar, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(10), 0, 0, 0);
        titles.addView(text("GeoJoystick", 20, palette.text, true));
        titles.addView(text(t("Transparent mock-location simulation",
                "Transparente Mock-Standort-Simulation"), 9, palette.textDim, false));
        header.addView(titles,
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button settingsButton = GeoUi.iconButton(this, palette, "⚙",
                t("Settings", "Einstellungen"));
        settingsButton.setOnClickListener(view -> showSettingsPage());
        header.addView(settingsButton, new LinearLayout.LayoutParams(dp(48), dp(48)));
        return header;
    }

    private void showSettingsPage() {
        saveVisibleCoordinates();
        currentPage = "settings";
        ScrollView page = pageScroll();
        LinearLayout root = pageRoot();
        page.addView(root);
        root.addView(pageHeader(t("Settings", "Einstellungen"), this::showHomePage), margin(0, 4));

        root.addView(section(t("Setup", "Einrichtung")), margin(6, 1));
        LinearLayout setup = card();

        boolean mockSelected = isSelectedMockLocationApp();
        setup.addView(stateSettingRow(
                t("Mock location", "Mock-Standort"),
                mockSelected
                        ? t("Selected", "Ausgewählt")
                        : t("Not selected", "Nicht ausgewählt"),
                mockSelected,
                this::openDeveloperSettings), innerRow());

        boolean overlayGranted = Settings.canDrawOverlays(this);
        setup.addView(stateSettingRow(
                t("Overlay permission", "Overlay-Berechtigung"),
                overlayGranted
                        ? t("Granted", "Erteilt")
                        : t("Not granted", "Nicht erteilt"),
                overlayGranted,
                this::openOverlaySettings), innerRow());

        setup.addView(settingRow(
                t("Reset overlay position", "Overlay-Position zurücksetzen"),
                "›",
                this::resetOverlayPosition), innerRow());

        boolean restoreEnabled = settings.restoreLastPosition();
        setup.addView(stateSettingRow(
                t("Restore last position", "Letzte Position wiederherstellen"),
                restoreEnabled ? t("On", "Ein") : t("Off", "Aus"),
                restoreEnabled,
                this::toggleRestoreLastPosition), innerRow());

        root.addView(setup, margin(1, 4));

        root.addView(section(t("Behavior", "Verhalten")), margin(6, 1));
        LinearLayout behavior = card();

        behavior.addView(settingRow(
                t("Simulation speed", "Simulationsgeschwindigkeit"),
                String.format(Locale.US, "%.1f m/s", settings.customSpeed()),
                this::editCustomSpeed), innerRow());

        boolean highContrastEnabled = settings.highContrastOverlay();
        behavior.addView(stateSettingRow(
                t("High contrast overlay", "Overlay mit hohem Kontrast"),
                highContrastEnabled ? t("On", "Ein") : t("Off", "Aus"),
                highContrastEnabled,
                this::toggleHighContrast), innerRow());

        root.addView(behavior, margin(1, 4));

        root.addView(section(t("Overlay", "Overlay")), margin(6, 1));
        LinearLayout overlay = card();

        TextView sizeLabel = text("", 12, palette.text, false);
        SeekBar size = new SeekBar(this);
        size.setMax(50);
        size.setProgress(settings.overlaySize() - 70);
        updateOverlaySizeLabel(sizeLabel, settings.overlaySize());
        size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = progress + 70;
                updateOverlaySizeLabel(sizeLabel, value);
                if (fromUser) settings.setOverlaySize(value);
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                settings.setOverlaySize(seekBar.getProgress() + 70);
            }
        });
        overlay.addView(sizeLabel, innerRow());
        overlay.addView(size, innerRow());

        TextView opacityLabel = text("", 12, palette.text, false);
        SeekBar opacity = new SeekBar(this);
        opacity.setMax(70);
        opacity.setProgress(settings.overlayOpacity() - 30);
        updateOpacityLabel(opacityLabel, settings.overlayOpacity());
        opacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = progress + 30;
                updateOpacityLabel(opacityLabel, value);
                if (fromUser) {
                    settings.setOverlayOpacity(value);
                }
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                settings.setOverlayOpacity(seekBar.getProgress() + 30);
            }
        });
        overlay.addView(opacityLabel, innerRow());
        overlay.addView(opacity, innerRow());
        root.addView(overlay, margin(1, 4));

        root.addView(section(t("Appearance & language", "Darstellung & Sprache")), margin(6, 1));
        LinearLayout appearance = card();
        appearance.addView(settingRow(
                t("Theme", "Darstellung"),
                appearanceLabel(),
                this::chooseAppearance), innerRow());
        appearance.addView(settingRow(
                t("Language", "Sprache"),
                languageLabel(),
                this::chooseLanguage), innerRow());
        root.addView(appearance, margin(1, 4));

        setContentView(page);
        updateBackCallbackRegistration(true);
        applySystemBarInsets(page);
    }

    private void showAboutPage() {
        saveVisibleCoordinates();
        currentPage = "about";

        FrameLayout stage = modalStage();
        LinearLayout modal = modalCard(dp(16), dp(14));

        LinearLayout identity = new LinearLayout(this);
        identity.setGravity(Gravity.CENTER_VERTICAL);

        ImageView avatar = new ImageView(this);
        avatar.setImageResource(R.drawable.k2040_avatar);
        avatar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        avatar.setContentDescription(t("K2040 avatar", "K2040-Avatar"));
        identity.addView(avatar, new LinearLayout.LayoutParams(dp(68), dp(68)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(10), 0, dp(4), 0);
        titles.addView(text("GeoJoystick", 22, palette.text, true));
        titles.addView(text(BuildConfig.VERSION_NAME, 10, palette.textDim, false));
        identity.addView(titles,
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView close = text("×", 24, palette.text, true);
        close.setGravity(Gravity.CENTER);
        close.setClickable(true);
        close.setFocusable(true);
        close.setContentDescription(t("Close About", "Info schließen"));
        close.setOnClickListener(view -> showHomePage());
        identity.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        modal.addView(identity, innerRow());

        ScrollView bodyScroll = new ScrollView(this);
        bodyScroll.setFillViewport(false);
        bodyScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        bodyScroll.addView(body, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView about = text(t(
                "Transparent mock-location joystick for Android developer and emulator testing.",
                "Transparenter Mock-Standort-Joystick für Android-Entwicklung und Emulator-Tests."),
                12, palette.text, false);
        about.setGravity(Gravity.CENTER);
        about.setLineSpacing(0, 1.08f);
        about.setPadding(dp(10), dp(8), dp(10), dp(4));
        body.addView(about, innerRow());

        TextView trust = text(t(
                "Local · offline-first · no account · no ads · no analytics · no tracking",
                "Lokal · offline-first · kein Konto · keine Werbung · keine Analysen · kein Tracking"),
                10, palette.textDim, true);
        trust.setGravity(Gravity.CENTER);
        trust.setPadding(dp(8), dp(2), dp(8), dp(6));
        body.addView(trust, innerRow());

        body.addView(welcomeNavigationRow(t("Changelog", "Änderungsverlauf"),
                () -> showChangelogPage(false)), innerRow());
        body.addView(welcomeNavigationRow(t("License & usage", "Lizenz & Nutzung"),
                () -> showLicensePage(false)), innerRow());
        body.addView(welcomeNavigationRow(t("Sources", "Quellen"),
                this::showSourcesPage), innerRow());
        body.addView(welcomeNavigationRow(t("Support on Ko-fi", "Auf Ko-fi unterstützen"),
                () -> openExternalUrl("https://ko-fi.com/k2040")), innerRow());

        TextView disclosure = text(supportDisclosureText(), 9, palette.textDim, false);
        disclosure.setGravity(Gravity.CENTER);
        disclosure.setPadding(dp(10), dp(5), dp(10), dp(2));
        body.addView(disclosure, innerRow());

        modal.addView(bodyScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        showModal(stage, modal, 336, 500, false);
        bodyScroll.post(() -> bodyScroll.scrollTo(0, 0));
    }

    private void showChangelogPage(boolean returnToWelcome) {
        currentPage = "changelog-about";
        FrameLayout stage = modalStage();
        LinearLayout modal = modalCard(dp(16), dp(14));

        TextView heading = text(t("Changelog", "Änderungsverlauf"), 24, palette.text, true);
        heading.setPadding(dp(4), dp(2), dp(4), dp(4));
        modal.addView(heading, innerRow());

        TextView version = text(BuildConfig.VERSION_NAME, 11, palette.textDim, true);
        version.setPadding(dp(4), 0, dp(4), dp(6));
        modal.addView(version, innerRow());

        ScrollView bodyScroll = new ScrollView(this);
        bodyScroll.setFillViewport(false);
        bodyScroll.setFocusable(false);
        bodyScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        TextView changes = text(changelogText(), 14, palette.text, false);
        changes.setTextIsSelectable(true);
        changes.setLineSpacing(0, 1.12f);
        changes.setPadding(dp(4), dp(2), dp(4), dp(4));
        bodyScroll.addView(changes);
        modal.addView(bodyScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        Button close = welcomeActionButton(t("Close", "Schließen"), false);
        close.setContentDescription(t("Close changelog", "Änderungsverlauf schließen"));
        close.setOnClickListener(view -> showAboutPage());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(dp(140), dp(48));
        closeParams.gravity = Gravity.CENTER_HORIZONTAL;
        closeParams.topMargin = dp(4);
        modal.addView(close, closeParams);

        showModal(stage, modal, 336, 430, false);
        bodyScroll.post(() -> bodyScroll.scrollTo(0, 0));
    }

    private void showWelcomePage() {
        currentPage = "welcome";

        FrameLayout stage = modalStage();
        LinearLayout modal = modalCard(dp(18), dp(14));
        modal.setGravity(Gravity.CENTER_HORIZONTAL);

        ImageView avatar = new ImageView(this);
        avatar.setImageResource(R.drawable.k2040_avatar);
        avatar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        avatar.setContentDescription(t("K2040 avatar", "K2040-Avatar"));
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(72), dp(72));
        avatarParams.gravity = Gravity.CENTER_HORIZONTAL;
        avatarParams.bottomMargin = dp(4);
        modal.addView(avatar, avatarParams);

        TextView title = text("GeoJoystick", 25, palette.text, true);
        title.setGravity(Gravity.CENTER);
        modal.addView(title, innerRow());

        TextView summary = text(t(
                "Transparent mock-location joystick for Android developer and emulator testing. "
                        + "Local-first, with no account, ads, analytics, or tracking.",
                "Transparenter Mock-Standort-Joystick für Android-Entwicklung und Emulator-Tests. "
                        + "Lokal, ohne Konto, Werbung, Analysen oder Tracking."),
                11, palette.textDim, false);
        summary.setGravity(Gravity.CENTER);
        summary.setLineSpacing(0, 1.08f);
        summary.setPadding(dp(8), dp(4), dp(8), dp(6));
        modal.addView(summary, innerRow());

        TextView legal = text(t(
                "App code: GPL-3.0-only\nK2040 artwork: CC BY 4.0\nMap data: © OpenStreetMap contributors · ODbL 1.0",
                "App-Code: GPL-3.0-only\nK2040-Grafik: CC BY 4.0\nKartendaten: © OpenStreetMap-Mitwirkende · ODbL 1.0"),
                10, palette.textDim, true);
        legal.setGravity(Gravity.CENTER);
        legal.setLineSpacing(0, 1.08f);
        legal.setPadding(dp(8), dp(5), dp(8), dp(7));
        legal.setMinimumHeight(dp(48));
        legal.setClickable(true);
        legal.setFocusable(true);
        legal.setContentDescription(t(
                "Licenses: app code GPL-3.0-only, K2040 artwork CC BY 4.0, and OpenStreetMap ODbL 1.0. Open license details",
                "Lizenzen: App-Code GPL-3.0-only, K2040-Grafik CC BY 4.0 und OpenStreetMap ODbL 1.0. Lizenzdetails öffnen"));
        legal.setOnClickListener(view -> showLicensePage(true));
        modal.addView(legal, innerRow());

        Button continueButton = welcomeActionButton(t("Continue", "Weiter"), true);
        continueButton.setContentDescription(t(
                "Continue to GeoJoystick",
                "Weiter zu GeoJoystick"));
        continueButton.setOnClickListener(view -> {
            settings.acknowledgeWelcome();
            showHomePage();
            handleIncomingIntent(getIntent());
        });
        LinearLayout.LayoutParams continueParams = new LinearLayout.LayoutParams(dp(180), dp(48));
        continueParams.gravity = Gravity.CENTER_HORIZONTAL;
        continueParams.topMargin = dp(6);
        modal.addView(continueButton, continueParams);

        showModal(stage, modal, 320, 0, true);
    }

    private void showLicensePage(boolean returnToWelcome) {
        currentPage = returnToWelcome ? "license-welcome" : "license-about";
        FrameLayout stage = modalStage();
        LinearLayout modal = modalCard(dp(16), dp(14));

        TextView heading = text(t("License & usage", "Lizenz & Nutzung"), 24, palette.text, true);
        heading.setPadding(dp(4), dp(2), dp(4), dp(8));
        modal.addView(heading, innerRow());
        TextView localHeading = text(t("LOCAL DATA", "LOKALE DATEN"), 11, palette.textDim, false);
        modal.addView(localHeading, innerRow());
        TextView localBody = text(t(
                "Coordinates and settings stay on this device. The map fetches OpenStreetMap tiles only when you open it; supported link resolution may use bounded HTTPS requests. No account, analytics, or hidden uploads.",
                "Koordinaten und Einstellungen bleiben auf diesem Gerät. Die Karte lädt OpenStreetMap-Kacheln nur, wenn du sie öffnest; unterstützte Linkauflösung kann begrenzte HTTPS-Anfragen verwenden. Kein Konto, keine Analysen und keine versteckten Uploads."),
                11, palette.text, false);
        localBody.setLineSpacing(0, 1.08f);
        localBody.setPadding(0, dp(2), 0, dp(8));
        modal.addView(localBody, innerRow());

        TextView licenseHeading = text(t("LICENSES", "LIZENZEN"), 11, palette.textDim, false);
        modal.addView(licenseHeading, innerRow());
        TextView licenseHint = text(t(
                "Tap an entry for the maintained license details.",
                "Tippe auf einen Eintrag für die gepflegten Lizenzdetails."),
                10, palette.textDim, false);
        licenseHint.setPadding(0, dp(1), 0, dp(5));
        modal.addView(licenseHint, innerRow());

        TextView scope = text(t(
                "Application code is GPL-3.0-only. K2040-authored GPL code also carries a separate section 7(b) attribution term. Original K2040 artwork is CC BY 4.0. Third-party material retains its own terms.",
                "Der Anwendungscode ist GPL-3.0-only. Von K2040 verfasster GPL-Code trägt zusätzlich eine separate §7(b)-Namensnennungsklausel. Originale K2040-Grafik ist CC BY 4.0. Drittmaterial behält seine eigenen Bedingungen."),
                10, palette.textDim, false);
        scope.setLineSpacing(0, 1.08f);
        scope.setPadding(0, 0, 0, dp(5));
        modal.addView(scope, innerRow());

        modal.addView(infoNavigationRow("GPL-3.0-only",
                t("Application code · K2040-authored portions also carry §7(b) attribution",
                        "Anwendungscode · K2040-verfasste Teile zusätzlich mit §7(b)-Namensnennung"),
                () -> showLicenseTextPage(returnToWelcome)), innerRow());
        modal.addView(infoNavigationRow(t("K2040 artwork · CC BY 4.0", "K2040-Grafik · CC BY 4.0"),
                t("Original K2040 artwork and UI artwork · attribution required",
                        "Originale K2040-Grafik und UI-Grafik · Namensnennung erforderlich"),
                () -> showArtworkLicensePage(returnToWelcome)), innerRow());
        modal.addView(infoNavigationRow("OpenStreetMap · ODbL 1.0",
                t("Map data · © OpenStreetMap contributors",
                        "Kartendaten · © OpenStreetMap-Mitwirkende"),
                () -> showOsmLicensePage(returnToWelcome)), innerRow());

        Button close = welcomeActionButton(t("Close", "Schließen"), false);
        close.setOnClickListener(view -> {
            if (returnToWelcome) {
                showWelcomePage();
            } else {
                showAboutPage();
            }
        });
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(dp(140), dp(48));
        closeParams.gravity = Gravity.CENTER_HORIZONTAL;
        closeParams.topMargin = dp(6);
        modal.addView(close, closeParams);

        showModal(stage, modal, 336, 500, true);
    }

    private void showLicenseTextPage() {
        showLicenseTextPage(false);
    }

    private void showLicenseTextPage(boolean returnToWelcome) {
        currentPage = returnToWelcome ? "license-text-welcome" : "license-text";
        FrameLayout stage = modalStage();
        LinearLayout modal = modalCard(dp(16), dp(14));

        TextView heading = text("GPL-3.0-only", 24, palette.text, true);
        heading.setPadding(dp(4), dp(2), dp(4), dp(6));
        modal.addView(heading, innerRow());

        TextView note = text(t(
                "The bundled English GPL text is the authoritative GPL text. Original K2040-authored GPL code also carries the separate section 7(b) attribution-preservation term requiring 'Copyright (c) 2026 K2040.' GoGoGo-derived and other third-party material retain their own notices.",
                "Der enthaltene englische GPL-Text ist der maßgebliche GPL-Text. Originaler, von K2040 verfasster GPL-Code trägt zusätzlich die separate §7(b)-Namensnennungsklausel mit dem Hinweis 'Copyright (c) 2026 K2040.' Von GoGoGo abgeleitetes und sonstiges Drittmaterial behält seine eigenen Hinweise."),
                10, palette.textDim, false);
        note.setPadding(dp(4), 0, dp(4), dp(6));
        modal.addView(note, innerRow());

        ScrollView scroller = new ScrollView(this);
        scroller.setOverScrollMode(View.OVER_SCROLL_NEVER);
        TextView license = text(reflowLicenseText(readAssetText("LICENSE")), 11, palette.text, false);
        license.setTextIsSelectable(true);
        license.setLineSpacing(0, 1.08f);
        license.setPadding(dp(4), dp(2), dp(4), dp(4));
        scroller.addView(license);
        modal.addView(scroller, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        Button close = welcomeActionButton(t("Close", "Schließen"), false);
        close.setOnClickListener(view -> showLicensePage(returnToWelcome));
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(dp(140), dp(48));
        closeParams.gravity = Gravity.CENTER_HORIZONTAL;
        closeParams.topMargin = dp(4);
        modal.addView(close, closeParams);

        showModal(stage, modal, 336, 560, false);
        scroller.post(() -> scroller.scrollTo(0, 0));
    }

    private void showArtworkLicensePage(boolean returnToWelcome) {
        currentPage = returnToWelcome ? "license-artwork-welcome" : "license-artwork";
        FrameLayout stage = modalStage();
        LinearLayout modal = modalCard(dp(16), dp(14));

        TextView heading = text(t("K2040 artwork · CC BY 4.0", "K2040-Grafik · CC BY 4.0"), 22, palette.text, true);
        heading.setPadding(dp(4), dp(2), dp(4), dp(8));
        modal.addView(heading, innerRow());

        TextView body = text(t(
                "Original artwork and UI artwork authored by K2040 and identified by project provenance is licensed under Creative Commons Attribution 4.0 International (CC BY 4.0).\n\nWhen sharing covered artwork, credit K2040, identify CC BY 4.0, link to the licence where reasonably practicable, and indicate modifications. Third-party and upstream assets are excluded and retain their own terms.",
                "Originale Grafiken und UI-Grafiken, die von K2040 erstellt und durch die Projekt-Provenienz ausgewiesen sind, stehen unter Creative Commons Attribution 4.0 International (CC BY 4.0).\n\nBei Weitergabe der erfassten Grafiken ist K2040 zu nennen, CC BY 4.0 anzugeben, soweit praktikabel auf die Lizenz zu verlinken und auf Änderungen hinzuweisen. Dritt- und Upstream-Assets sind ausgenommen und behalten ihre eigenen Bedingungen."),
                12, palette.text, false);
        body.setLineSpacing(0, 1.1f);
        body.setPadding(dp(4), dp(2), dp(4), dp(8));
        modal.addView(body, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        Button official = welcomeActionButton(t("Open official license", "Offizielle Lizenz öffnen"), false);
        official.setOnClickListener(view -> openExternalUrl("https://creativecommons.org/licenses/by/4.0/legalcode"));
        LinearLayout.LayoutParams officialParams = new LinearLayout.LayoutParams(dp(210), dp(48));
        officialParams.gravity = Gravity.CENTER_HORIZONTAL;
        modal.addView(official, officialParams);

        Button close = welcomeActionButton(t("Close", "Schließen"), false);
        close.setOnClickListener(view -> showLicensePage(returnToWelcome));
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(dp(140), dp(48));
        closeParams.gravity = Gravity.CENTER_HORIZONTAL;
        closeParams.topMargin = dp(2);
        modal.addView(close, closeParams);

        showModal(stage, modal, 336, 470, false);
    }

    private void showOsmLicensePage() {
        showOsmLicensePage(false);
    }

    private void showOsmLicensePage(boolean returnToWelcome) {
        currentPage = returnToWelcome ? "license-osm-welcome" : "license-osm";
        FrameLayout stage = modalStage();
        LinearLayout modal = modalCard(dp(16), dp(14));

        TextView heading = text("OpenStreetMap", 24, palette.text, true);
        heading.setPadding(dp(4), dp(2), dp(4), dp(6));
        modal.addView(heading, innerRow());

        TextView body = text(t(
                "GeoJoystick's built-in map loads tiles from OpenStreetMap only when the map is used. Map data is © OpenStreetMap contributors and is made available under the Open Data Commons Open Database License (ODbL) 1.0.\n\nThe map keeps visible contributor attribution. The official OpenStreetMap copyright and licensing page contains the maintained details and any additional notices that apply to the tile service.",
                "Die integrierte GeoJoystick-Karte lädt Kacheln von OpenStreetMap nur, wenn die Karte verwendet wird. Kartendaten sind © OpenStreetMap-Mitwirkende und werden unter der Open Data Commons Open Database License (ODbL) 1.0 bereitgestellt.\n\nDie Karte behält die sichtbare Mitwirkenden-Nennung bei. Die offizielle OpenStreetMap-Seite zu Urheberrecht und Lizenzierung enthält die gepflegten Details und zusätzliche Hinweise, die für den Kacheldienst gelten."),
                12, palette.text, false);
        body.setLineSpacing(0, 1.1f);
        body.setPadding(dp(4), dp(2), dp(4), dp(8));
        modal.addView(body, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        Button official = welcomeActionButton(t("Open official details", "Offizielle Details öffnen"), false);
        official.setOnClickListener(view -> openExternalUrl("https://www.openstreetmap.org/copyright"));
        LinearLayout.LayoutParams officialParams = new LinearLayout.LayoutParams(dp(210), dp(48));
        officialParams.gravity = Gravity.CENTER_HORIZONTAL;
        modal.addView(official, officialParams);

        Button close = welcomeActionButton(t("Close", "Schließen"), false);
        close.setOnClickListener(view -> showLicensePage(returnToWelcome));
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(dp(140), dp(48));
        closeParams.gravity = Gravity.CENTER_HORIZONTAL;
        closeParams.topMargin = dp(2);
        modal.addView(close, closeParams);

        showModal(stage, modal, 336, 470, false);
    }

    private void showSourcesPage() {
        currentPage = "sources-about";
        FrameLayout stage = modalStage();
        LinearLayout modal = modalCard(dp(16), dp(14));

        TextView heading = text(t("Sources", "Quellen"), 24, palette.text, true);
        heading.setPadding(dp(4), dp(2), dp(4), dp(8));
        modal.addView(heading, innerRow());

        TextView intro = text(t(
                "External projects and material GeoJoystick builds on or uses directly.",
                "Externe Projekte und Materialien, auf denen GeoJoystick aufbaut oder die direkt verwendet werden."),
                11, palette.text, false);
        intro.setLineSpacing(0, 1.08f);
        intro.setPadding(dp(4), 0, dp(4), dp(8));
        modal.addView(intro, innerRow());

        modal.addView(infoNavigationRow("GoGoGo / 影梭",
                t("Original upstream project · ZCShou and contributors",
                        "Ursprüngliches Upstream-Projekt · ZCShou und Mitwirkende"),
                () -> openExternalUrl("https://github.com/ZCShou/GoGoGo")), innerRow());
        modal.addView(infoNavigationRow("OpenStreetMap",
                t("Map tiles and data · © OpenStreetMap contributors",
                        "Kartenkacheln und -daten · © OpenStreetMap-Mitwirkende"),
                () -> openExternalUrl("https://www.openstreetmap.org/copyright")), innerRow());

        TextView provenance = text(t(
                "GeoJoystick is a GPL-3.0-only derivative informed by GoGoGo. Its mock-location service and joystick movement design were adapted and substantially simplified. GoGoGo-derived material keeps its upstream GPLv3 attribution; the K2040 §7(b) term applies only to K2040-authored GPL material. Proprietary Baidu SDK components and advertising are not included.",
                "GeoJoystick ist ein GPL-3.0-only-Derivat, das auf GoGoGo aufbaut. Mock-Standort-Dienst und Joystick-Bewegungsdesign wurden angepasst und deutlich vereinfacht. Von GoGoGo abgeleitetes Material behält seine GPLv3-Upstream-Namensnennung; die K2040-§7(b)-Klausel gilt nur für von K2040 verfasstes GPL-Material. Proprietäre Baidu-SDK-Komponenten und Werbung sind nicht enthalten."),
                10, palette.textDim, false);
        provenance.setLineSpacing(0, 1.08f);
        provenance.setPadding(dp(4), dp(8), dp(4), dp(4));
        modal.addView(provenance, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        Button close = welcomeActionButton(t("Close", "Schließen"), false);
        close.setOnClickListener(view -> showAboutPage());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(dp(140), dp(48));
        closeParams.gravity = Gravity.CENTER_HORIZONTAL;
        closeParams.topMargin = dp(4);
        modal.addView(close, closeParams);

        showModal(stage, modal, 336, 500, false);
    }

    private FrameLayout modalStage() {
        FrameLayout stage = new FrameLayout(this);
        stage.setBackgroundColor(palette.background);
        stage.setClickable(true);
        stage.setFocusable(true);

        ScrollView background = buildHomePage();
        background.setAlpha(settings.isDark() ? 0.34f : 0.26f);
        background.setEnabled(false);
        background.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        background.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        stage.addView(background, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        View scrim = new View(this);
        scrim.setBackgroundColor(Color.BLACK);
        scrim.setAlpha(settings.isDark() ? 0.60f : 0.43f);
        scrim.setClickable(true);
        scrim.setFocusable(true);
        scrim.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        stage.addView(scrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        return stage;
    }

    private LinearLayout modalCard(int horizontalPadding, int verticalPadding) {
        LinearLayout modal = new LinearLayout(this);
        modal.setOrientation(LinearLayout.VERTICAL);
        modal.setPadding(horizontalPadding, verticalPadding,
                horizontalPadding, verticalPadding);
        modal.setBackground(GeoUi.elevated(this, palette));
        modal.setElevation(dp(18));
        modal.setClickable(true);
        modal.setFocusable(true);
        modal.setFocusableInTouchMode(true);
        return modal;
    }

    private void showModal(FrameLayout stage, LinearLayout modal,
                           int widthDp, int heightDp, boolean wrapHeight) {
        int width = Math.min(dp(widthDp), getResources().getDisplayMetrics().widthPixels - dp(56));
        FrameLayout.LayoutParams params;
        if (wrapHeight) {
            params = new FrameLayout.LayoutParams(
                    Math.max(dp(260), width),
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER);
        } else {
            int availableHeight = Math.max(dp(300),
                    getResources().getDisplayMetrics().heightPixels - dp(72));
            int height = Math.min(dp(heightDp), availableHeight);
            params = new FrameLayout.LayoutParams(
                    Math.max(dp(260), width),
                    height,
                    Gravity.CENTER);
        }
        stage.addView(modal, params);
        setContentView(stage);
        updateBackCallbackRegistration(true);
        applySystemBarInsets(stage);
        modal.requestFocus();
    }

    private LinearLayout infoNavigationRow(String title, String subtitle, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(8), dp(8), dp(8));
        row.setMinimumHeight(dp(56));
        row.setBackground(GeoUi.surface(this, palette));
        row.setClickable(true);
        row.setFocusable(true);
        row.setContentDescription(title + ". " + subtitle + ". " + t("Open", "Öffnen"));
        row.setOnClickListener(view -> action.run());

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        TextView primary = text(title, 12, palette.text, true);
        TextView secondary = text(subtitle, 10, palette.textDim, false);
        labels.addView(primary);
        labels.addView(secondary);
        row.addView(labels, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView chevron = text("›", 20, palette.textDim, true);
        chevron.setGravity(Gravity.CENTER);
        chevron.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(chevron, new LinearLayout.LayoutParams(dp(28), dp(40)));
        return row;
    }

    private LinearLayout trustPanel() {
        LinearLayout trust = new LinearLayout(this);
        trust.setOrientation(LinearLayout.VERTICAL);
        trust.setPadding(dp(14), dp(12), dp(14), dp(12));
        trust.setBackground(GeoUi.rounded(this, palette.accentSoft, 14, palette.accent, 1));
        TextView headline = text(t("Local · No account · No unnecessary tracking",
                "Lokal · Kein Konto · Kein unnötiges Tracking"), 12, palette.text, true);
        trust.addView(headline);
        TextView detail = text(t("Coordinates and settings stay on your device. No analytics and no account system.",
                "Koordinaten und Einstellungen bleiben auf deinem Gerät. Keine Analysen und kein Kontosystem."),
                10, palette.textDim, false);
        detail.setPadding(0, dp(4), 0, 0);
        trust.addView(detail);
        return trust;
    }

    private LinearLayout welcomeNavigationRow(String title, Runnable action) {
        LinearLayout row = welcomeRowHeader(title, "›");
        row.setBackground(GeoUi.surface(this, palette));
        row.setContentDescription(title + ". " + t("Open", "Öffnen"));
        row.setOnClickListener(view -> action.run());
        return row;
    }

    private LinearLayout welcomeRowHeader(String title, String chevronText) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), 0, dp(10), 0);
        row.setMinimumHeight(dp(48));
        row.setClickable(true);
        row.setFocusable(true);

        TextView label = text(title, 12, palette.text, false);
        label.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        label.setSingleLine(true);
        row.addView(label, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        TextView chevron = text(chevronText, 18, palette.textDim, false);
        chevron.setGravity(Gravity.CENTER);
        chevron.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(chevron, new LinearLayout.LayoutParams(dp(28), ViewGroup.LayoutParams.MATCH_PARENT));
        return row;
    }

    private Button welcomeActionButton(String label, boolean primary) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(12);
        button.setTextColor(primary ? Color.WHITE : palette.text);
        button.setGravity(Gravity.CENTER);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(10), 0, dp(10), 0);
        button.setBackground(new android.graphics.drawable.InsetDrawable(
                GeoUi.rounded(this,
                        primary ? palette.accent : palette.surface,
                        20,
                        primary ? palette.accent : palette.border,
                        1),
                dp(6), dp(4), dp(6), dp(4)));
        button.setStateListAnimator(null);
        return button;
    }

    private String welcomeAboutText() {
        return t(
                "Transparent mock-location joystick for Android developer and emulator testing.\n\n"
                        + "Local-first · No account · No ads · No analytics · No tracking\n"
                        + "Coordinates and settings stay on your device.",
                "Transparenter Mock-Standort-Joystick für Android-Entwicklung und Emulator-Tests.\n\n"
                        + "Lokal · Kein Konto · Keine Werbung · Keine Analysen · Kein Tracking\n"
                        + "Koordinaten und Einstellungen bleiben auf deinem Gerät.");
    }

    private Button infoRow(String title, String subtitle, Runnable action) {
        Button row = GeoUi.button(this, palette, rowText(title, subtitle, "›"), false);
        row.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        row.setTextSize(12);
        row.setOnClickListener(view -> action.run());
        return row;
    }

    private LinearLayout settingRow(String title, String value, Runnable action) {
        return settingRow(title, value, palette.textDim, action);
    }

    private LinearLayout stateSettingRow(
            String title,
            String value,
            boolean active,
            Runnable action) {
        return settingRow(
                title,
                value,
                active ? palette.success : palette.danger,
                action);
    }

    private LinearLayout settingRow(
            String title,
            String value,
            int valueColor,
            Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(48));
        row.setPadding(dp(12), 0, dp(12), 0);
        row.setBackground(GeoUi.surface(this, palette));
        row.setClickable(true);
        row.setFocusable(true);
        row.setContentDescription(title + ". " + value);
        row.setOnClickListener(view -> action.run());

        TextView label = text(title, 13, palette.text, false);
        label.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        label.setSingleLine(true);
        row.addView(label, new LinearLayout.LayoutParams(
                0, dp(48), 1f));

        TextView state = text(value, 12, valueColor, true);
        state.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        state.setSingleLine(true);
        row.addView(state, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(48)));

        return row;
    }

    private String rowText(String title, String subtitle, String value) {
        String suffix = value == null || value.isEmpty() ? "" : "\n" + value;
        return title + "\n" + subtitle + suffix;
    }

    private LinearLayout pageHeader(String title, Runnable backAction) {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button back = GeoUi.iconButton(this, palette, "‹", t("Back", "Zurück"));
        back.setOnClickListener(view -> backAction.run());
        header.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView heading = text(title, 22, palette.text, true);
        heading.setPadding(dp(10), 0, 0, 0);
        header.addView(heading,
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return header;
    }

    private void chooseAppearance() {
        String[] labels = new String[]{t("System default", "Systemstandard"), t("Light", "Hell"), t("Dark", "Dunkel")};
        String[] values = new String[]{GeoSettings.APPEARANCE_SYSTEM, GeoSettings.APPEARANCE_LIGHT, GeoSettings.APPEARANCE_DARK};
        appDialogBuilder()
                .setTitle(t("Appearance", "Darstellung"))
                .setSingleChoiceItems(labels, indexOf(values, settings.appearance()), (dialog, which) -> {
                    settings.setAppearance(values[which]);
                    dialog.dismiss();
                    refreshUiSettings();
                })
                .setNegativeButton(t("Cancel", "Abbrechen"), null)
                .show();
    }

    private void chooseLanguage() {
        String[] labels = new String[]{"System default", "English", "Deutsch"};
        String[] values = new String[]{GeoSettings.LANGUAGE_SYSTEM, GeoSettings.LANGUAGE_ENGLISH, GeoSettings.LANGUAGE_GERMAN};
        appDialogBuilder()
                .setTitle(t("Language", "Sprache"))
                .setSingleChoiceItems(labels, indexOf(values, settings.language()), (dialog, which) -> {
                    settings.setLanguage(values[which]);
                    dialog.dismiss();
                    refreshUiSettings();
                })
                .setNegativeButton(t("Cancel", "Abbrechen"), null)
                .show();
    }

    private void refreshUiSettings() {
        String page = currentPage;
        loadUiSettings();
        if ("settings".equals(page)) {
            showSettingsPage();
        } else if ("about".equals(page)) {
            showAboutPage();
        } else if ("welcome".equals(page)) {
            showWelcomePage();
        } else if ("changelog-about".equals(page)) {
            showChangelogPage(false);
        } else if ("license-about".equals(page)) {
            showLicensePage(false);
        } else if ("license-text".equals(page)) {
            showLicenseTextPage();
        } else if ("license-osm".equals(page)) {
            showOsmLicensePage();
        } else if ("license-artwork".equals(page)) {
            showArtworkLicensePage(false);
        } else if ("sources-about".equals(page)) {
            showSourcesPage();
        } else {
            showHomePage();
        }
    }

    private int indexOf(String[] values, String current) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(current)) return i;
        }
        return 0;
    }

    private String appearanceLabel() {
        String value = settings.appearance();
        if (GeoSettings.APPEARANCE_DARK.equals(value)) return t("Dark", "Dunkel");
        if (GeoSettings.APPEARANCE_LIGHT.equals(value)) return t("Light", "Hell");
        return t("System", "System");
    }

    private String languageLabel() {
        String value = settings.language();
        if (GeoSettings.LANGUAGE_ENGLISH.equals(value)) return "English";
        if (GeoSettings.LANGUAGE_GERMAN.equals(value)) return "Deutsch";
        return t("System", "System");
    }

    private void toggleRestoreLastPosition() {
        saveVisibleCoordinates();
        boolean next = !settings.restoreLastPosition();
        settings.setRestoreLastPosition(next);
        if (next) {
            double[] last = settings.lastActiveCoordinates();
            if (last != null) {
                settings.saveManualCoordinates(last[0], last[1], last[2]);
                setDraft(last[0], last[1], last[2]);
            }
        }
        showSettingsPage();
    }

    private void resetOverlayPosition() {
        settings.resetOverlayPosition();
        toast(t("Overlay position reset for next show",
                "Overlay-Position für die nächste Anzeige zurückgesetzt"), false);
    }

    private void toggleHighContrast() {
        settings.setHighContrastOverlay(!settings.highContrastOverlay());
        showSettingsPage();
    }

    private void editCustomSpeed() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(8), 0, dp(8), 0);
        EditText name = textInput(t("Name", "Name"), settings.customSpeedName());
        EditText speed = coordinateInput(t("Speed in m/s", "Geschwindigkeit in m/s"), settings.customSpeed());
        form.addView(name, innerRow());
        form.addView(speed, innerRow());

        AlertDialog dialog = appDialogBuilder()
                .setTitle(t("Custom speed", "Eigene Geschwindigkeit"))
                .setView(form)
                .setPositiveButton(t("Save", "Speichern"), null)
                .setNegativeButton(t("Cancel", "Abbrechen"), null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    try {
                        double value = Double.parseDouble(speed.getText().toString().trim());
                        if (!Double.isFinite(value) || value < 0.1 || value > 50.0) {
                            throw new NumberFormatException("speed");
                        }
                        String label = name.getText().toString().trim();
                        if (label.isEmpty()) label = "Custom";
                        settings.setCustomSpeed(label, value);
                        dialog.dismiss();
                        showSettingsPage();
                    } catch (NumberFormatException exception) {
                        toast(t("Enter a valid speed", "Gib eine gültige Geschwindigkeit ein"), false);
                    }
                }));
        dialog.show();
    }

    private void updateOverlaySizeLabel(TextView label, int sizePercent) {
        label.setText(String.format(Locale.US,
                t("Overlay size: %d%%", "Overlay-Größe: %d%%"), sizePercent));
    }

    private void updateOpacityLabel(TextView label, int opacity) {
        label.setText(String.format(Locale.US,
                t("Overlay opacity: %d%%", "Overlay-Deckkraft: %d%%"), opacity));
    }

    private void chooseFavoriteSlot() {
        if (!validCoordinates()) return;
        String[] slots = new String[GeoSettings.FAVORITE_COUNT];
        for (int i = 0; i < slots.length; i++) {
            GeoSettings.Favorite favorite = settings.favorite(i);
            slots[i] = favorite == null ? t("Favorite ", "Favorit ") + (i + 1) : favorite.name;
        }
        appDialogBuilder()
                .setTitle(t("Choose favorite slot", "Favoritenplatz wählen"))
                .setItems(slots, (dialog, which) -> editFavorite(which, false))
                .setNegativeButton(t("Cancel", "Abbrechen"), null)
                .show();
    }

    private void applyFavorite(int slot) {
        GeoSettings.Favorite favorite = settings.favorite(slot);
        if (favorite == null) {
            if (validCoordinates()) editFavorite(slot, false);
            return;
        }
        setCoordinates(favorite.latitude, favorite.longitude, favorite.altitude);
        toast(t("Favorite loaded into coordinate fields", "Favorit in Koordinatenfelder geladen"), false);
    }

    private void editFavorite(int slot, boolean useSaved) {
        if (!useSaved && !validCoordinates()) return;
        GeoSettings.Favorite saved = settings.favorite(slot);
        double latitude = useSaved && saved != null ? saved.latitude : safeLatitude();
        double longitude = useSaved && saved != null ? saved.longitude : safeLongitude();
        double altitude = useSaved && saved != null ? saved.altitude : safeAltitude();
        String initialName = useSaved && saved != null ? saved.name : t("Favorite ", "Favorit ") + (slot + 1);

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(8), 0, dp(8), 0);
        EditText name = textInput(t("Favorite name", "Favoritenname"), initialName);
        EditText lat = coordinateInput(t("Latitude", "Breitengrad"), latitude);
        EditText lng = coordinateInput(t("Longitude", "Längengrad"), longitude);
        EditText alt = coordinateInput(t("Altitude (m)", "Höhe (m)"), altitude);
        form.addView(name, innerRow());
        form.addView(lat, innerRow());
        form.addView(lng, innerRow());
        form.addView(alt, innerRow());

        AlertDialog dialog = appDialogBuilder()
                .setTitle(t("Favorite ", "Favorit ") + (slot + 1))
                .setView(form)
                .setPositiveButton(t("Save", "Speichern"), null)
                .setNegativeButton(t("Cancel", "Abbrechen"), null)
                .setNeutralButton(t("Clear", "Leeren"), null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                try {
                    double favoriteLat = Double.parseDouble(lat.getText().toString().trim());
                    double favoriteLng = Double.parseDouble(lng.getText().toString().trim());
                    double favoriteAlt = Double.parseDouble(alt.getText().toString().trim());
                    if (!GeoSettings.validCoordinates(favoriteLat, favoriteLng, favoriteAlt)) {
                        throw new NumberFormatException("coordinates");
                    }
                    String favoriteName = name.getText().toString().trim();
                    if (favoriteName.isEmpty()) favoriteName = t("Favorite ", "Favorit ") + (slot + 1);
                    settings.saveFavorite(slot, favoriteName, favoriteLat, favoriteLng, favoriteAlt);
                    refreshFavoriteButtons();
                    dialog.dismiss();
                } catch (NumberFormatException exception) {
                    toast(t("Enter valid favorite coordinates", "Gib gültige Favoriten-Koordinaten ein"), false);
                }
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view -> {
                settings.clearFavorite(slot);
                refreshFavoriteButtons();
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private void refreshFavoriteButtons() {
        for (int i = 0; i < favoriteButtons.length; i++) {
            Button button = favoriteButtons[i];
            if (button == null) continue;
            GeoSettings.Favorite favorite = settings.favorite(i);
            String name = favorite == null ? t("Fav ", "Fav ") + (i + 1) : favorite.name;
            button.setText(name.length() > 13 ? name.substring(0, 13) : name);
            button.setContentDescription(favorite == null
                    ? t("Empty favorite ", "Leerer Favorit ") + (i + 1)
                    : favorite.name);
        }
    }

    private Button favoriteButton(int slot) {
        Button button = GeoUi.button(this, palette, t("Fav ", "Fav ") + (slot + 1), false);
        button.setTextSize(11);
        button.setSingleLine(true);
        return button;
    }

    private void openMap() {
        Intent intent = new Intent(this, MapActivity.class);
        double latitude = safeLatitude();
        double longitude = safeLongitude();
        if (GeoSettings.validHorizontal(latitude, longitude)) {
            intent.putExtra(MapActivity.EXTRA_LATITUDE, latitude);
            intent.putExtra(MapActivity.EXTRA_LONGITUDE, longitude);
        }
        startActivityForResult(intent, REQUEST_MAP);
    }

    private void importFromClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null || !clipboard.hasPrimaryClip()) {
            toast(t("Clipboard is empty", "Zwischenablage ist leer"), false);
            return;
        }
        ClipData clip = clipboard.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) {
            toast(t("Clipboard is empty", "Zwischenablage ist leer"), false);
            return;
        }
        CharSequence text = clip.getItemAt(0).coerceToText(this);
        importLocationText(text == null ? null : text.toString());
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null || incomingIntentConsumed || !Intent.ACTION_SEND.equals(intent.getAction())) {
            return;
        }
        incomingIntentConsumed = true;
        CharSequence text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        intent.setAction(null);
        intent.removeExtra(Intent.EXTRA_TEXT);
        if (text != null) importLocationText(text.toString());
    }

    private void importLocationText(String value) {
        if (value == null || value.trim().isEmpty()) {
            toast(t("No location link found", "Kein Standortlink gefunden"), false);
            return;
        }
        int requestId = ++importRequestId;
        toast(t("Reading location link…", "Standortlink wird gelesen…"), false);
        new Thread(() -> {
            double[] coordinates = LocationLinkParser.resolveCoordinates(value);
            runOnUiThread(() -> {
                if (requestId != importRequestId || isFinishing() || isDestroyed()) return;
                if (coordinates == null) {
                    toast(t("Could not extract coordinates from that link",
                            "Aus diesem Link konnten keine Koordinaten gelesen werden"), true);
                    return;
                }
                if (!setHorizontalCoordinates(coordinates[0], coordinates[1])) {
                    toast(t("The imported coordinates were invalid",
                            "Die importierten Koordinaten waren ungültig"), true);
                    return;
                }
                toast(Double.isFinite(safeAltitude())
                        ? t("Coordinates imported", "Koordinaten importiert")
                        : t("Coordinates imported. Enter an altitude before starting.",
                                "Koordinaten importiert. Gib vor dem Start eine Höhe ein."), true);
            });
        }, "MapLinkResolver-" + requestId).start();
    }

    private void startMocking() {
        if (!validCoordinates()) return;
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
                    .setPositiveButton(t("Open settings", "Einstellungen öffnen"),
                            (dialog, which) -> openDeveloperSettings())
                    .setNegativeButton(t("Cancel", "Abbrechen"),
                            (dialog, which) -> pendingStart = false)
                    .show();
            return;
        }
        pendingStart = false;
        startMockingInternal();
    }

    private void startMockingInternal() {
        double latitude = safeLatitude();
        double longitude = safeLongitude();
        double altitude = safeAltitude();
        if (!GeoSettings.validCoordinates(latitude, longitude, altitude)) {
            toast(t("Enter valid latitude, longitude, and altitude",
                    "Gib gültige Werte für Breitengrad, Längengrad und Höhe ein"), true);
            return;
        }
        settings.saveManualCoordinates(latitude, longitude, altitude);
        requestNotificationPermissionIfNeeded();
        Intent intent = new Intent(this, MockLocationService.class)
                .setAction(MockLocationService.ACTION_START)
                .putExtra(MockLocationService.EXTRA_LATITUDE, latitude)
                .putExtra(MockLocationService.EXTRA_LONGITUDE, longitude)
                .putExtra(MockLocationService.EXTRA_ALTITUDE, altitude);
        try {
            startForegroundService(intent);
            toast(t("GeoJoystick start requested", "GeoJoystick-Start angefordert"), false);
            scheduleStatusRefresh();
        } catch (RuntimeException exception) {
            toast(t("GeoJoystick could not be started", "GeoJoystick konnte nicht gestartet werden"), true);
            updateStatus();
        }
    }

    private void stopMocking() {
        Intent intent = new Intent(this, MockLocationService.class).setAction(MockLocationService.ACTION_STOP);
        try {
            startService(intent);
            toast(t("GeoJoystick stop requested", "GeoJoystick-Stopp angefordert"), false);
            scheduleStatusRefresh();
        } catch (RuntimeException exception) {
            toast(t("GeoJoystick could not be stopped", "GeoJoystick konnte nicht gestoppt werden"), true);
            updateStatus();
        }
    }

    private void scheduleStatusRefresh() {
        if (simulationStatus == null) return;
        simulationStatus.post(this::updateStatus);
        simulationStatus.postDelayed(this::updateStatus, 300L);
        simulationStatus.postDelayed(this::updateStatus, 1000L);
    }

    private void updateStatus() {
        if (mockStatus == null || overlayStatus == null || simulationStatus == null) return;
        boolean mockSelected = isSelectedMockLocationApp();
        boolean overlayGranted = Settings.canDrawOverlays(this);
        boolean starting = MockLocationService.isSimulationStarting();
        boolean active = MockLocationService.isSimulationActive();
        setStatus(mockStatus, mockSelected ? t("Selected", "Ausgewählt") : t("Not selected", "Nicht ausgewählt"),
                mockSelected ? palette.success : palette.danger);
        setStatus(overlayStatus, overlayGranted ? t("Granted", "Erteilt") : t("Not granted", "Nicht erteilt"),
                overlayGranted ? palette.success : palette.danger);
        if (active) {
            setStatus(simulationStatus, t("Active", "Aktiv"), palette.success);
        } else if (starting) {
            setStatus(simulationStatus, t("Starting", "Wird gestartet"), palette.warning);
        } else {
            setStatus(simulationStatus, t("Inactive", "Inaktiv"), palette.accent);
        }

        if (simulationStartButton != null) {
            boolean startEnabled = !active && !starting;
            simulationStartButton.setEnabled(startEnabled);
            simulationStartButton.setAlpha(startEnabled ? 1.0f : 0.45f);
        }
        if (simulationStopButton != null) {
            boolean stopEnabled = active || starting;
            simulationStopButton.setEnabled(stopEnabled);
            simulationStopButton.setAlpha(stopEnabled ? 1.0f : 0.45f);
        }
    }

    private void setStatus(TextView view, String value, int color) {
        view.setText(value);
        view.setTextColor(color);
    }

    private boolean isSelectedMockLocationApp() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        if (appOps == null) return false;
        return appOps.checkOpNoThrow(AppOpsManager.OPSTR_MOCK_LOCATION,
                Process.myUid(), getPackageName()) == AppOpsManager.MODE_ALLOWED;
    }

    private void openOverlaySettings() {
        startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())));
    }

    private void openDeveloperSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        } catch (RuntimeException exception) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerSimulationStateReceiver() {
        if (receiverRegistered) return;
        IntentFilter filter = new IntentFilter(MockLocationService.ACTION_STATE_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(simulationStateReceiver, filter,
                    MockLocationService.PERMISSION_INTERNAL_STATE, null, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(simulationStateReceiver, filter,
                    MockLocationService.PERMISSION_INTERNAL_STATE, null);
        }
        receiverRegistered = true;
    }

    private void unregisterSimulationStateReceiver() {
        if (!receiverRegistered) return;
        try {
            unregisterReceiver(simulationStateReceiver);
        } catch (IllegalArgumentException ignored) {
            // Lifecycle teardown may already have removed it.
        }
        receiverRegistered = false;
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        }
    }

    private double[] initialCoordinates() {
        if (draftInitialized) return new double[]{draftLatitude, draftLongitude, draftAltitude};
        double[] manual = settings.manualCoordinates();
        if (manual != null) {
            setDraft(manual[0], manual[1], manual[2]);
            return manual;
        }
        if (settings.restoreLastPosition()) {
            double[] active = settings.lastActiveCoordinates();
            if (active != null) {
                setDraft(active[0], active[1], active[2]);
                return active;
            }
        }
        setDraft(Double.NaN, Double.NaN, Double.NaN);
        return new double[]{Double.NaN, Double.NaN, Double.NaN};
    }

    private boolean validCoordinates() {
        double latitude = safeLatitude();
        double longitude = safeLongitude();
        double altitude = safeAltitude();
        if (GeoSettings.validCoordinates(latitude, longitude, altitude)) return true;
        toast(t("Enter valid latitude, longitude, and altitude",
                "Gib gültige Werte für Breitengrad, Längengrad und Höhe ein"), true);
        return false;
    }

    private double safeLatitude() { return safeDouble(latitudeInput); }
    private double safeLongitude() { return safeDouble(longitudeInput); }
    private double safeAltitude() { return safeDouble(altitudeInput); }

    private double safeDouble(EditText input) {
        if (input == null) return Double.NaN;
        try {
            double value = Double.parseDouble(input.getText().toString().trim());
            return Double.isFinite(value) ? value : Double.NaN;
        } catch (RuntimeException exception) {
            return Double.NaN;
        }
    }

    private void setCoordinates(double latitude, double longitude, double altitude) {
        if (!GeoSettings.validCoordinates(latitude, longitude, altitude)
                || latitudeInput == null || longitudeInput == null || altitudeInput == null) return;
        setDraft(latitude, longitude, altitude);
        latitudeInput.setText(format(latitude));
        longitudeInput.setText(format(longitude));
        altitudeInput.setText(format(altitude));
        settings.saveManualCoordinates(latitude, longitude, altitude);
    }

    private boolean setHorizontalCoordinates(double latitude, double longitude) {
        if (!GeoSettings.validHorizontal(latitude, longitude)
                || latitudeInput == null || longitudeInput == null) return false;
        double altitude = safeAltitude();
        setDraft(latitude, longitude, altitude);
        latitudeInput.setText(format(latitude));
        longitudeInput.setText(format(longitude));
        if (GeoSettings.validCoordinates(latitude, longitude, altitude)) {
            settings.saveManualCoordinates(latitude, longitude, altitude);
        }
        return true;
    }

    private void saveVisibleCoordinates() {
        // Only the home page owns live coordinate editors; other pages retain detached references.
        if (!"main".equals(currentPage)
                || latitudeInput == null
                || longitudeInput == null
                || altitudeInput == null) {
            return;
        }
        double latitude = safeLatitude();
        double longitude = safeLongitude();
        double altitude = safeAltitude();
        setDraft(latitude, longitude, altitude);
        if (GeoSettings.validCoordinates(latitude, longitude, altitude)) {
            settings.saveManualCoordinates(latitude, longitude, altitude);
        }
    }

    private void setDraft(double latitude, double longitude, double altitude) {
        draftLatitude = latitude;
        draftLongitude = longitude;
        draftAltitude = altitude;
        draftInitialized = true;
    }

    private void restoreDraft(Bundle state) {
        if (state == null || !state.getBoolean(STATE_DRAFT_INITIALIZED, false)) return;
        setDraft(state.getDouble(STATE_DRAFT_LATITUDE, Double.NaN),
                state.getDouble(STATE_DRAFT_LONGITUDE, Double.NaN),
                state.getDouble(STATE_DRAFT_ALTITUDE, Double.NaN));
    }

    private String format(double value) {
        return Double.isFinite(value) ? String.format(Locale.US, "%.6f", value) : "";
    }

    private EditText coordinateInput(String hint, double value) {
        EditText input = textInput(hint, format(value));
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
        input.setTextColor(palette.text);
        input.setHintTextColor(palette.textDim);
        input.setPadding(dp(12), dp(8), dp(12), dp(8));
        input.setMinHeight(dp(48));
        input.setBackground(GeoUi.input(this, palette));
        return input;
    }

    private ScrollView pageScroll() {
        ScrollView page = new ScrollView(this);
        page.setFillViewport(true);
        page.setClipToPadding(false);
        page.setBackgroundColor(palette.background);
        return page;
    }

    private LinearLayout pageRoot() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(10), dp(16), dp(18));
        root.setBackgroundColor(palette.background);
        return root;
    }

    private LinearLayout card() {
        return GeoUi.card(this, palette);
    }

    private void addCardTitle(LinearLayout card, String value) {
        TextView title = text(value, 13, palette.text, true);
        title.setPadding(dp(2), 0, dp(2), dp(4));
        card.addView(title);
    }

    private TextView addStatusRow(LinearLayout card, String label) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = text(label, 13, palette.text, false);
        name.setGravity(Gravity.CENTER_VERTICAL);
        TextView value = text(t("Checking…", "Wird geprüft…"), 12, palette.textDim, true);
        value.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        row.addView(name, new LinearLayout.LayoutParams(0, dp(42), 1f));
        row.addView(value, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(42)));
        card.addView(row);
        return value;
    }

    private Button actionTile(String symbol, String label) {
        Button button = GeoUi.button(this, palette, symbol + "\n" + label, false);
        button.setTextSize(11);
        button.setMinHeight(dp(58));
        button.setMinimumHeight(dp(58));
        return button;
    }

    private TextView section(String value) {
        return GeoUi.sectionLabel(this, palette, value);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView text = GeoUi.text(this, value, size, color);
        if (bold) text.setTypeface(Typeface.DEFAULT_BOLD);
        return text;
    }

    private LinearLayout.LayoutParams innerRow() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(2);
        params.bottomMargin = dp(2);
        return params;
    }

    private LinearLayout.LayoutParams margin(int top, int bottom) {
        return GeoUi.matchWidth(this, top, bottom);
    }

    private LinearLayout.LayoutParams tileWeight() {
        LinearLayout.LayoutParams params = GeoUi.weighted(this, 2);
        params.height = dp(60);
        return params;
    }

    private AlertDialog.Builder appDialogBuilder() {
        return new AlertDialog.Builder(this,
                settings.isDark() ? R.style.AppDialogThemeDark : R.style.AppDialogThemeLight);
    }

    private String changelogText() {
        return t(
                "• Redesigned Home, Settings, and overlay controls with clearer status and compact mode.\n"
                        + "• Improved simulation recovery, restore-last-position, and invalid-input handling.\n"
                        + "• Added onboarding plus About, Changelog, License & usage, and Sources screens.\n"
                        + "• Expanded map-link parsing and map picker reliability.\n"
                        + "• Improved accessibility, large-font behavior, and touch targets.\n"
                        + "• Added the new GeoJoystick mascot/launcher identity and Android 16/API 36 back-navigation support.\n\n"
                        + "0.1.3\n"
                        + "• Dialogs now follow the selected dark theme.\n"
                        + "• GeoJoystick now uses a dedicated icon in store listings.\n\n"
                        + "0.1.0\n"
                        + "• Initial public release with coordinate and altitude entry, map selection and link import, favorites, appearance and language settings, and floating joystick controls.",
                "• Startseite, Einstellungen und Overlay-Steuerung wurden mit klarerem Status und besserem Kompaktmodus überarbeitet.\n"
                        + "• Simulations-Wiederherstellung, letzte Position und Fehlerbehandlung wurden verbessert.\n"
                        + "• Onboarding sowie Info-, Änderungsverlauf-, Lizenz- und Quellenansichten wurden ergänzt.\n"
                        + "• Kartenlink-Import und Kartenwahl wurden robuster.\n"
                        + "• Barrierefreiheit, große Schrift und Touch-Ziele wurden verbessert.\n"
                        + "• Neues GeoJoystick-Maskottchen/App-Symbol und Android-16/API-36-Zurücknavigation.\n\n"
                        + "0.1.3\n"
                        + "• Dialoge folgen nun dem ausgewählten dunklen Design.\n"
                        + "• GeoJoystick verwendet nun ein eigenes Symbol in Store-Einträgen.\n\n"
                        + "0.1.0\n"
                        + "• Erste öffentliche Version mit Koordinaten- und Höheneingabe, Kartenauswahl und Linkimport, Favoriten, Darstellungs- und Spracheinstellungen sowie schwebender Joystick-Steuerung.");
    }

    private String supportDisclosureText() {
        return t("Donations are entirely optional. They do not unlock features or provide any additional benefits.",
                "Spenden sind vollständig freiwillig. Sie schalten keine Funktionen frei und bieten keinerlei zusätzliche Vorteile.");
    }

    private String readAssetText(String name) {
        StringBuilder builder = new StringBuilder();
        try (InputStream stream = getAssets().open(name);
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) builder.append(line).append('\n');
        } catch (IOException exception) {
            return t("License text is unavailable in this build.",
                    "Der Lizenztext ist in diesem Build nicht verfügbar.");
        }
        return builder.toString();
    }

    private String reflowLicenseText(String text) {
        if (text == null || text.isEmpty()) return "";
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] paragraphs = normalized.split("\n[ \\t]*\n");
        StringBuilder output = new StringBuilder();
        for (String paragraph : paragraphs) {
            StringBuilder joined = new StringBuilder();
            for (String line : paragraph.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                if (joined.length() > 0) joined.append(' ');
                joined.append(trimmed);
            }
            if (joined.length() == 0) continue;
            if (output.length() > 0) output.append("\n\n");
            output.append(joined);
        }
        return output.toString();
    }

    private void openExternalUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (RuntimeException exception) {
            toast(t("No browser app available", "Keine Browser-App verfügbar"), false);
        }
    }

    private void applySystemBarInsets(View view) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return;
        int left = view.getPaddingLeft();
        int top = view.getPaddingTop();
        int right = view.getPaddingRight();
        int bottom = view.getPaddingBottom();
        view.setOnApplyWindowInsetsListener((target, insets) -> {
            android.graphics.Insets safe = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            target.setPadding(left + safe.left, top + safe.top, right + safe.right, bottom + safe.bottom);
            return insets;
        });
        view.requestApplyInsets();
    }

    private void toast(String value, boolean longDuration) {
        Toast.makeText(this, value, longDuration ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
    }

    private String t(String english, String germanText) {
        return german ? germanText : english;
    }

    private int dp(int value) {
        return GeoUi.dp(this, value);
    }
}
