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
        handleIncomingIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerSimulationStateReceiver();
        updateStatus();
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

    @Override
    public void onBackPressed() {
        if ("license-welcome".equals(currentPage)) {
            showWelcomePage();
            return;
        }
        if ("license-about".equals(currentPage) || "changelog".equals(currentPage)) {
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
        } else if ("changelog".equals(page)) {
            showChangelogPage();
        } else if ("license-about".equals(page)) {
            showLicensePage(false);
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
        applySystemBarInsets(page);
        updateStatus();
    }

    private ScrollView buildHomePage() {
        ScrollView page = pageScroll();
        LinearLayout root = pageRoot();
        page.addView(root);
        root.addView(appHeader(), margin(0, 4));

        LinearLayout status = card();
        addCardTitle(status, t("Status", "Status"));
        mockStatus = addStatusRow(status, t("Mock location", "Mock-Standort"));
        overlayStatus = addStatusRow(status, t("Overlay permission", "Overlay-Berechtigung"));
        simulationStatus = addStatusRow(status, t("Simulation", "Simulation"));
        root.addView(status, margin(4, 6));

        LinearLayout coordinates = card();
        addCardTitle(coordinates, t("Coordinates", "Koordinaten"));
        double[] initial = initialCoordinates();
        latitudeInput = coordinateInput(t("Latitude", "Breitengrad"), initial[0]);
        longitudeInput = coordinateInput(t("Longitude", "Längengrad"), initial[1]);
        altitudeInput = coordinateInput(t("Altitude (m)", "Höhe (m)"), initial[2]);
        coordinates.addView(latitudeInput, innerRow());
        coordinates.addView(longitudeInput, innerRow());
        coordinates.addView(altitudeInput, innerRow());
        root.addView(coordinates, margin(4, 6));

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
        root.addView(quick, margin(2, 4));

        LinearLayout favoriteCard = card();
        LinearLayout favoriteHeader = new LinearLayout(this);
        favoriteHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView favoriteTitle = text(t("Favorites", "Favoriten"), 14, palette.text, true);
        favoriteHeader.addView(favoriteTitle,
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView favoriteHint = text(t("Tap to load · hold to edit",
                "Tippen zum Laden · halten zum Bearbeiten"), 10, palette.textDim, false);
        favoriteHeader.addView(favoriteHint);
        favoriteCard.addView(favoriteHeader, innerRow());

        HorizontalScrollView scroller = new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        LinearLayout favoriteRow = new LinearLayout(this);
        favoriteRow.setOrientation(LinearLayout.HORIZONTAL);
        favoriteRow.setPadding(0, dp(4), 0, dp(2));
        for (int slot = 0; slot < GeoSettings.FAVORITE_COUNT; slot++) {
            final int index = slot;
            Button button = favoriteButton(index);
            button.setOnClickListener(view -> applyFavorite(index));
            button.setOnLongClickListener(view -> {
                editFavorite(index, true);
                return true;
            });
            favoriteButtons[index] = button;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(112), dp(58));
            params.rightMargin = dp(8);
            favoriteRow.addView(button, params);
        }
        scroller.addView(favoriteRow);
        favoriteCard.addView(scroller, innerRow());
        root.addView(favoriteCard, margin(4, 6));
        refreshFavoriteButtons();

        Button start = GeoUi.button(this, palette,
                t("▶  Start simulation", "▶  Simulation starten"), true);
        start.setOnClickListener(view -> startMocking());
        root.addView(start, margin(8, 4));

        Button stop = GeoUi.button(this, palette,
                t("■  Stop simulation", "■  Simulation stoppen"), false);
        stop.setOnClickListener(view -> stopMocking());
        root.addView(stop, margin(4, 4));

        TextView footer = text(t("Open source · GPL-3.0-only · Local-first",
                "Open Source · GPL-3.0-only · Lokal"), 10, palette.textDim, false);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(dp(8), dp(8), dp(8), dp(4));
        footer.setOnClickListener(view -> showAboutPage());
        root.addView(footer, margin(2, 4));
        return page;
    }

    private LinearLayout appHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(2), dp(2), dp(2), dp(8));

        ImageView avatar = new ImageView(this);
        avatar.setImageResource(R.drawable.k2040_avatar);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatar.setContentDescription(t("About GeoJoystick", "Über GeoJoystick"));
        avatar.setOnClickListener(view -> showAboutPage());
        header.addView(avatar, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(12), 0, 0, 0);
        titles.addView(text("GeoJoystick", 22, palette.text, true));
        titles.addView(text(t("Transparent mock-location simulation",
                "Transparente Mock-Standort-Simulation"), 10, palette.textDim, false));
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

        root.addView(section(t("Setup", "Einrichtung")), margin(8, 2));
        LinearLayout setup = card();
        setup.addView(settingRow(
                t("Mock location settings", "Mock-Standort-Einstellungen"),
                t("Select GeoJoystick in Android Developer Options",
                        "GeoJoystick in den Android-Entwickleroptionen auswählen"),
                isSelectedMockLocationApp() ? t("Selected", "Ausgewählt") : t("Open", "Öffnen"),
                this::openDeveloperSettings), innerRow());
        setup.addView(settingRow(
                t("Overlay permission", "Overlay-Berechtigung"),
                t("Manage draw-over-other-apps access", "Berechtigung über anderen Apps verwalten"),
                Settings.canDrawOverlays(this) ? t("Granted", "Erteilt") : t("Open", "Öffnen"),
                this::openOverlaySettings), innerRow());
        setup.addView(settingRow(
                t("Reset overlay position", "Overlay-Position zurücksetzen"),
                t("Recenter the floating controls next time they appear",
                        "Schwebende Steuerung beim nächsten Anzeigen neu positionieren"),
                "›", this::resetOverlayPosition), innerRow());
        setup.addView(settingRow(
                t("Restore last position", "Letzte Position wiederherstellen"),
                t("Use last successfully published coordinates as the next draft",
                        "Zuletzt erfolgreich veröffentlichte Koordinaten als nächsten Entwurf verwenden"),
                settings.restoreLastPosition() ? t("On", "Ein") : t("Off", "Aus"),
                this::toggleRestoreLastPosition), innerRow());
        root.addView(setup, margin(2, 6));

        root.addView(section(t("Behavior", "Verhalten")), margin(8, 2));
        LinearLayout behavior = card();
        behavior.addView(settingRow(
                t("Simulation speed", "Simulationsgeschwindigkeit"),
                t("Edit the custom movement preset", "Eigene Bewegungsvoreinstellung bearbeiten"),
                String.format(Locale.US, "%.1f m/s", settings.customSpeed()),
                this::editCustomSpeed), innerRow());
        behavior.addView(settingRow(
                t("High contrast overlay", "Overlay mit hohem Kontrast"),
                t("Increase contrast of the floating controls", "Kontrast der schwebenden Steuerung erhöhen"),
                settings.highContrastOverlay() ? t("On", "Ein") : t("Off", "Aus"),
                this::toggleHighContrast), innerRow());
        root.addView(behavior, margin(2, 6));

        root.addView(section(t("Overlay", "Overlay")), margin(8, 2));
        LinearLayout overlay = card();
        TextView opacityLabel = text("", 13, palette.text, false);
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
        root.addView(overlay, margin(2, 6));

        root.addView(section(t("Appearance & language", "Darstellung & Sprache")), margin(8, 2));
        LinearLayout appearance = card();
        appearance.addView(settingRow(
                t("Theme", "Darstellung"),
                t("Follow system or choose light/dark", "System übernehmen oder Hell/Dunkel wählen"),
                appearanceLabel(), this::chooseAppearance), innerRow());
        appearance.addView(settingRow(
                t("Language", "Sprache"),
                t("System, English or Deutsch", "System, English oder Deutsch"),
                languageLabel(), this::chooseLanguage), innerRow());
        root.addView(appearance, margin(2, 6));

        setContentView(page);
        applySystemBarInsets(page);
    }

    private void showAboutPage() {
        saveVisibleCoordinates();
        currentPage = "about";
        ScrollView page = pageScroll();
        LinearLayout root = pageRoot();
        page.addView(root);
        root.addView(pageHeader(t("About", "Info"), this::showHomePage), margin(0, 4));

        LinearLayout identity = card();
        ImageView avatar = new ImageView(this);
        avatar.setImageResource(R.drawable.k2040_avatar);
        avatar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        avatar.setContentDescription(t("K2040 avatar", "K2040-Avatar"));
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(108), dp(108));
        avatarParams.gravity = Gravity.CENTER_HORIZONTAL;
        avatarParams.bottomMargin = dp(8);
        identity.addView(avatar, avatarParams);
        TextView name = text("GeoJoystick", 26, palette.text, true);
        name.setGravity(Gravity.CENTER);
        identity.addView(name, innerRow());
        TextView description = text(
                t("Transparent mock-location simulation for Android developer and emulator testing.",
                        "Transparente Mock-Standort-Simulation für Android-Entwicklung und Emulator-Tests."),
                13, palette.textDim, false);
        description.setGravity(Gravity.CENTER);
        description.setPadding(dp(10), 0, dp(10), dp(8));
        identity.addView(description, innerRow());
        root.addView(identity, margin(4, 6));

        root.addView(trustPanel(), margin(4, 6));

        LinearLayout info = card();
        info.addView(infoRow("Version 0.1.3", t("What's new", "Neuigkeiten"), this::showChangelogPage), innerRow());
        info.addView(infoRow(t("License & usage", "Lizenz & Nutzung"), "GPL-3.0-only", () -> showLicensePage(false)), innerRow());
        info.addView(infoRow(t("Support on Ko-fi", "Auf Ko-fi unterstützen"),
                t("Optional · no features unlocked", "Optional · keine Funktionen werden freigeschaltet"),
                () -> openExternalUrl("https://ko-fi.com/k2040")), innerRow());
        root.addView(info, margin(4, 4));

        TextView disclosure = text(supportDisclosureText(), 10, palette.textDim, false);
        disclosure.setGravity(Gravity.CENTER);
        disclosure.setPadding(dp(12), dp(4), dp(12), dp(8));
        root.addView(disclosure, margin(0, 4));

        setContentView(page);
        applySystemBarInsets(page);
    }

    private void showChangelogPage() {
        currentPage = "changelog";
        ScrollView page = pageScroll();
        LinearLayout root = pageRoot();
        page.addView(root);
        root.addView(pageHeader(t("What's new", "Neuigkeiten"), this::showAboutPage), margin(0, 4));
        TextView changes = text(changelogText(), 14, palette.text, false);
        changes.setTextIsSelectable(true);
        changes.setLineSpacing(0, 1.12f);
        changes.setPadding(dp(16), dp(16), dp(16), dp(16));
        changes.setBackground(GeoUi.surface(this, palette));
        root.addView(changes, margin(4, 6));
        setContentView(page);
        applySystemBarInsets(page);
    }

    private void showWelcomePage() {
        currentPage = "welcome";
        FrameLayout stage = new FrameLayout(this);
        stage.setBackgroundColor(palette.background);

        ScrollView background = buildHomePage();
        background.setAlpha(settings.isDark() ? 0.34f : 0.26f);
        background.setEnabled(false);
        background.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        stage.addView(background, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        View scrim = new View(this);
        scrim.setBackgroundColor(Color.BLACK);
        scrim.setAlpha(settings.isDark() ? 0.60f : 0.43f);
        scrim.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        stage.addView(scrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout modal = new LinearLayout(this);
        modal.setOrientation(LinearLayout.VERTICAL);
        modal.setPadding(dp(16), dp(16), dp(16), dp(14));
        modal.setBackground(GeoUi.elevated(this, palette));
        modal.setElevation(dp(18));

        ScrollView bodyScroll = new ScrollView(this);
        bodyScroll.setFillViewport(false);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        bodyScroll.addView(body);

        ImageView avatar = new ImageView(this);
        avatar.setImageResource(R.drawable.k2040_avatar);
        avatar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        avatar.setContentDescription(t("K2040 avatar", "K2040-Avatar"));
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(108), dp(108));
        avatarParams.gravity = Gravity.CENTER_HORIZONTAL;
        avatarParams.bottomMargin = dp(6);
        body.addView(avatar, avatarParams);

        TextView title = text("GeoJoystick", 28, palette.text, true);
        title.setGravity(Gravity.CENTER);
        body.addView(title, innerRow());
        TextView description = text(t("Transparent mock-location simulation for Android.",
                "Transparente Mock-Standort-Simulation für Android."), 13, palette.textDim, false);
        description.setGravity(Gravity.CENTER);
        description.setPadding(dp(8), 0, dp(8), dp(8));
        body.addView(description, innerRow());

        body.addView(welcomeRow("Version 0.1.3",
                t("Build and release information", "Build- und Versionsinformationen"),
                this::showVersionDialog), innerRow());
        body.addView(welcomeRow(t("License & usage", "Lizenz & Nutzung"),
                t("GPL-3.0-only · no warranty", "GPL-3.0-only · keine Gewährleistung"),
                () -> showLicensePage(true)), innerRow());
        body.addView(welcomeRow(t("Support on Ko-fi", "Auf Ko-fi unterstützen"),
                t("Optional donation · no features unlocked", "Optionale Spende · keine Funktionen werden freigeschaltet"),
                () -> openExternalUrl("https://ko-fi.com/k2040")), innerRow());

        TextView disclosure = text(supportDisclosureText(), 10, palette.textDim, false);
        disclosure.setGravity(Gravity.CENTER);
        disclosure.setPadding(dp(8), dp(2), dp(8), dp(6));
        body.addView(disclosure, innerRow());
        body.addView(trustPanel(), innerRow());

        TextView thanks = text(t("♥  Thank you for trying GeoJoystick.",
                "♥  Danke, dass du GeoJoystick ausprobierst."), 12, palette.textDim, false);
        thanks.setGravity(Gravity.CENTER);
        thanks.setPadding(dp(8), dp(8), dp(8), dp(2));
        body.addView(thanks, innerRow());

        TextView acknowledgement = text(
                t("Continuing confirms that you have acknowledged this notice. It is not acceptance of the GPL.",
                        "Mit „Weiter“ bestätigst du, dass du diesen Hinweis zur Kenntnis genommen hast. Dies ist keine Zustimmung zur GPL."),
                10, palette.textDim, false);
        acknowledgement.setGravity(Gravity.CENTER);
        acknowledgement.setPadding(dp(8), dp(2), dp(8), dp(4));
        body.addView(acknowledgement, innerRow());

        modal.addView(bodyScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        Button continueButton = GeoUi.button(this, palette,
                t("Acknowledge & continue", "Zur Kenntnis genommen & weiter"), true);
        continueButton.setOnClickListener(view -> {
            settings.acknowledgeWelcome();
            showHomePage();
            handleIncomingIntent(getIntent());
        });
        modal.addView(continueButton, margin(8, 0));

        int width = Math.min(dp(420), getResources().getDisplayMetrics().widthPixels - dp(32));
        int height = Math.min(dp(500), getResources().getDisplayMetrics().heightPixels - dp(64));
        FrameLayout.LayoutParams modalParams = new FrameLayout.LayoutParams(
                Math.max(dp(260), width),
                Math.max(dp(320), height),
                Gravity.CENTER);
        stage.addView(modal, modalParams);

        setContentView(stage);
        applySystemBarInsets(stage);
    }

    private void showVersionDialog() {
        appDialogBuilder()
                .setTitle("GeoJoystick 0.1.3")
                .setMessage(changelogText())
                .setPositiveButton(t("Close", "Schließen"), null)
                .show();
    }

    private void showLicensePage(boolean returnToWelcome) {
        currentPage = returnToWelcome && !settings.welcomeAcknowledged()
                ? "license-welcome" : "license-about";
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(16));
        root.setBackgroundColor(palette.background);
        root.addView(pageHeader("GPL-3.0-only",
                "license-welcome".equals(currentPage) ? this::showWelcomePage : this::showAboutPage),
                margin(0, 4));

        TextView note = text(t("The bundled English GPL text is the authoritative license text.",
                "Der enthaltene englische GPL-Text ist der maßgebliche Lizenztext."),
                10, palette.textDim, false);
        note.setGravity(Gravity.CENTER);
        root.addView(note, margin(0, 4));

        TextView license = text(reflowLicenseText(readAssetText("LICENSE")), 11, palette.text, false);
        license.setTextIsSelectable(true);
        license.setLineSpacing(0, 1.08f);
        license.setPadding(dp(16), dp(14), dp(16), dp(14));
        license.setBackground(GeoUi.surface(this, palette));
        ScrollView scroller = new ScrollView(this);
        scroller.addView(license);
        LinearLayout.LayoutParams scrollerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        scrollerParams.topMargin = dp(6);
        root.addView(scroller, scrollerParams);
        setContentView(root);
        applySystemBarInsets(root);
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

    private Button welcomeRow(String title, String subtitle, Runnable action) {
        return infoRow(title, subtitle, action);
    }

    private Button infoRow(String title, String subtitle, Runnable action) {
        Button row = GeoUi.button(this, palette, rowText(title, subtitle, "›"), false);
        row.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        row.setTextSize(12);
        row.setOnClickListener(view -> action.run());
        return row;
    }

    private Button settingRow(String title, String subtitle, String value, Runnable action) {
        Button row = GeoUi.button(this, palette, rowText(title, subtitle, value), false);
        row.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        row.setTextSize(12);
        row.setOnClickListener(view -> action.run());
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
        } else {
            showHomePage();
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
        if (latitudeInput == null || longitudeInput == null || altitudeInput == null) return;
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
        input.setPadding(dp(12), dp(10), dp(12), dp(10));
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
        root.setPadding(dp(16), dp(14), dp(16), dp(24));
        root.setBackgroundColor(palette.background);
        return root;
    }

    private LinearLayout card() {
        return GeoUi.card(this, palette);
    }

    private void addCardTitle(LinearLayout card, String value) {
        TextView title = text(value, 13, palette.text, true);
        title.setPadding(dp(2), 0, dp(2), dp(6));
        card.addView(title);
    }

    private TextView addStatusRow(LinearLayout card, String label) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = text(label, 13, palette.text, false);
        name.setGravity(Gravity.CENTER_VERTICAL);
        TextView value = text(t("Checking…", "Wird geprüft…"), 12, palette.textDim, true);
        value.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        row.addView(name, new LinearLayout.LayoutParams(0, dp(48), 1f));
        row.addView(value, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(48)));
        card.addView(row);
        return value;
    }

    private Button actionTile(String symbol, String label) {
        Button button = GeoUi.button(this, palette, symbol + "\n" + label, false);
        button.setTextSize(12);
        button.setMinHeight(dp(66));
        button.setMinimumHeight(dp(66));
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
        params.topMargin = dp(3);
        params.bottomMargin = dp(3);
        return params;
    }

    private LinearLayout.LayoutParams margin(int top, int bottom) {
        return GeoUi.matchWidth(this, top, bottom);
    }

    private LinearLayout.LayoutParams tileWeight() {
        LinearLayout.LayoutParams params = GeoUi.weighted(this, 3);
        params.height = dp(68);
        return params;
    }

    private AlertDialog.Builder appDialogBuilder() {
        return new AlertDialog.Builder(this,
                settings.isDark() ? R.style.AppDialogThemeDark : R.style.AppDialogThemeLight);
    }

    private String changelogText() {
        return t(
                "Version 0.1.3\n"
                        + "• Dialogs now follow the selected dark theme.\n"
                        + "• GeoJoystick now uses a dedicated icon in store listings.\n\n"
                        + "Version 0.1.0\n"
                        + "• Initial public release with coordinate and altitude entry, map selection and link import, favorites, appearance and language settings, and floating joystick controls.",
                "Version 0.1.3\n"
                        + "• Dialoge folgen nun dem ausgewählten dunklen Design.\n"
                        + "• GeoJoystick verwendet nun ein eigenes Symbol in Store-Einträgen.\n\n"
                        + "Version 0.1.0\n"
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
        String[] paragraphs = normalized.split("\\n[ \\t]*\\n");
        StringBuilder output = new StringBuilder();
        for (String paragraph : paragraphs) {
            StringBuilder joined = new StringBuilder();
            for (String line : paragraph.split("\\n")) {
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
