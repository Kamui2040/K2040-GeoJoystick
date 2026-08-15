package com.k2040.geojoystick;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class MapActivity extends Activity {
    static final String EXTRA_LATITUDE = "map_latitude";
    static final String EXTRA_LONGITUDE = "map_longitude";

    private static final String PREFS = "geojoystick";
    private static final String PREF_APPEARANCE = "app_appearance";
    private static final String PREF_LANGUAGE = "app_language";
    private static final String APPEARANCE_SYSTEM = "system";
    private static final String APPEARANCE_DARK = "dark";
    private static final String LANGUAGE_SYSTEM = "system";
    private static final String LANGUAGE_GERMAN = "de";
    private static final String INTERNAL_HOST = "appassets.androidplatform.net";
    private static final String TILE_HOST = "tile.openstreetmap.org";
    private static final String STATE_HAS_SELECTION = "map_has_selection";
    private static final String STATE_LATITUDE = "map_selected_latitude";
    private static final String STATE_LONGITUDE = "map_selected_longitude";
    private static final double MAX_MAP_LATITUDE = 85.05112878;

    private double selectedLatitude = Double.NaN;
    private double selectedLongitude = Double.NaN;
    private boolean hasSelection;
    private TextView coordinateText;
    private Button useButton;
    private WebView webView;
    private boolean german;
    private GeoUi.Palette palette;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadUiSettings();
        restoreInitialSelection(savedInstanceState, getIntent());

        FrameLayout stage = new FrameLayout(this);
        stage.setBackgroundColor(palette.background);

        webView = new WebView(this);
        configureWebView(webView);
        stage.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout chrome = new LinearLayout(this);
        chrome.setOrientation(LinearLayout.VERTICAL);
        chrome.setPadding(dp(12), dp(12), dp(12), dp(12));
        chrome.setClickable(false);

        LinearLayout toolbar = GeoUi.card(this, palette);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setElevation(dp(8));

        Button back = GeoUi.iconButton(this, palette, "‹", t("Cancel map selection", "Kartenauswahl abbrechen"));
        back.setOnClickListener(view -> finish());
        toolbar.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        titleBlock.setPadding(dp(10), 0, dp(10), 0);
        TextView title = GeoUi.text(this, t("Choose location", "Standort wählen"), 17, palette.text);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        coordinateText = GeoUi.text(this, "", 11, palette.textDim);
        coordinateText.setSingleLine(true);
        titleBlock.addView(title);
        titleBlock.addView(coordinateText);
        toolbar.addView(titleBlock, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        useButton = GeoUi.button(this, palette, t("Use location", "Standort nutzen"), true);
        useButton.setEnabled(hasSelection);
        useButton.setAlpha(hasSelection ? 1f : 0.48f);
        useButton.setOnClickListener(view -> returnSelection());
        toolbar.addView(useButton, new LinearLayout.LayoutParams(dp(128), dp(48)));
        chrome.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView hint = GeoUi.text(
                this,
                t("Tap the map to place the marker. No location is selected automatically.",
                        "Tippe auf die Karte, um die Markierung zu setzen. Es wird kein Standort automatisch ausgewählt."),
                11,
                palette.textDim);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(dp(12), dp(8), dp(12), dp(8));
        hint.setBackground(GeoUi.rounded(this, palette.elevated, 12, palette.border, 1));
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        hintParams.topMargin = dp(8);
        chrome.addView(hint, hintParams);

        FrameLayout.LayoutParams chromeParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP);
        stage.addView(chrome, chromeParams);

        updateCoordinateText();
        setContentView(stage);
        applySystemBarInsets(stage);
        loadBundledMap();
    }

    private void restoreInitialSelection(Bundle savedInstanceState, Intent launchIntent) {
        if (savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_HAS_SELECTION, false)) {
            double latitude = savedInstanceState.getDouble(STATE_LATITUDE, Double.NaN);
            double longitude = savedInstanceState.getDouble(STATE_LONGITUDE, Double.NaN);
            if (validLatitude(latitude) && validLongitude(longitude)) {
                selectedLatitude = latitude;
                selectedLongitude = longitude;
                hasSelection = true;
                return;
            }
        }

        if (launchIntent == null
                || !launchIntent.hasExtra(EXTRA_LATITUDE)
                || !launchIntent.hasExtra(EXTRA_LONGITUDE)) {
            return;
        }

        double latitude = launchIntent.getDoubleExtra(EXTRA_LATITUDE, Double.NaN);
        double longitude = launchIntent.getDoubleExtra(EXTRA_LONGITUDE, Double.NaN);
        if (validLatitude(latitude) && validLongitude(longitude)) {
            selectedLatitude = latitude;
            selectedLongitude = longitude;
            hasSelection = true;
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView(WebView view) {
        WebSettings settings = view.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(false);
        settings.setDatabaseEnabled(false);
        settings.setGeolocationEnabled(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSafeBrowsingEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        String userAgent = settings.getUserAgentString();
        settings.setUserAgentString(
                (userAgent == null ? "" : userAgent + " ")
                        + "GeoJoystick/"
                        + BuildConfig.VERSION_NAME
                        + " (com.k2040.geojoystick; map picker)");

        WebView.setWebContentsDebuggingEnabled(false);
        view.setWebViewClient(new RestrictedWebViewClient());
        view.addJavascriptInterface(new MapBridge(), "AndroidBridge");
    }

    private void loadBundledMap() {
        try {
            String html = readAsset("map.html");
            String baseUrl = "https://" + INTERNAL_HOST + "/map.html";
            if (hasSelection) {
                baseUrl = String.format(
                        Locale.US,
                        "%s?lat=%.8f&lng=%.8f",
                        baseUrl,
                        selectedLatitude,
                        selectedLongitude);
            }
            webView.loadDataWithBaseURL(
                    baseUrl,
                    html,
                    "text/html",
                    StandardCharsets.UTF_8.name(),
                    null);
        } catch (IOException exception) {
            coordinateText.setText(t("Map unavailable", "Karte nicht verfügbar"));
            setUseButtonEnabled(false);
        }
    }

    private String readAsset(String name) throws IOException {
        StringBuilder content = new StringBuilder();
        try (InputStream input = getAssets().open(name);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(input, StandardCharsets.UTF_8))) {
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) >= 0) {
                content.append(buffer, 0, read);
            }
        }
        return content.toString();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_HAS_SELECTION, hasSelection);
        if (hasSelection) {
            outState.putDouble(STATE_LATITUDE, selectedLatitude);
            outState.putDouble(STATE_LONGITUDE, selectedLongitude);
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.removeJavascriptInterface("AndroidBridge");
            webView.stopLoading();
            webView.loadUrl("about:blank");
            webView.destroy();
        }
        super.onDestroy();
    }

    private void loadUiSettings() {
        SharedPreferences preferences = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String language = preferences.getString(PREF_LANGUAGE, LANGUAGE_SYSTEM);
        german = LANGUAGE_GERMAN.equals(language)
                || (LANGUAGE_SYSTEM.equals(language)
                && Locale.getDefault().getLanguage().equals("de"));
        String appearance = preferences.getString(PREF_APPEARANCE, APPEARANCE_SYSTEM);
        boolean dark = APPEARANCE_DARK.equals(appearance)
                || (APPEARANCE_SYSTEM.equals(appearance) && isSystemDarkMode());
        palette = new GeoUi.Palette(dark);
        getWindow().setStatusBarColor(palette.background);
        getWindow().setNavigationBarColor(palette.background);
    }

    private boolean isSystemDarkMode() {
        int nightMode = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private void updateCoordinateText() {
        if (!hasSelection) {
            coordinateText.setText(t(
                    "No location selected",
                    "Kein Standort ausgewählt"));
            return;
        }
        coordinateText.setText(String.format(
                Locale.US,
                t("%.6f, %.6f", "%.6f, %.6f"),
                selectedLatitude,
                selectedLongitude));
    }

    private void setUseButtonEnabled(boolean enabled) {
        useButton.setEnabled(enabled);
        useButton.setAlpha(enabled ? 1f : 0.48f);
    }

    private void returnSelection() {
        if (!hasSelection
                || !validLatitude(selectedLatitude)
                || !validLongitude(selectedLongitude)) {
            return;
        }
        Intent result = new Intent();
        result.putExtra(EXTRA_LATITUDE, selectedLatitude);
        result.putExtra(EXTRA_LONGITUDE, selectedLongitude);
        setResult(RESULT_OK, result);
        finish();
    }

    private boolean validLatitude(double value) {
        return Double.isFinite(value)
                && value >= -MAX_MAP_LATITUDE
                && value <= MAX_MAP_LATITUDE;
    }

    private boolean validLongitude(double value) {
        return Double.isFinite(value) && value >= -180.0 && value <= 180.0;
    }

    private void applySystemBarInsets(View view) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return;
        }
        int baseLeft = view.getPaddingLeft();
        int baseTop = view.getPaddingTop();
        int baseRight = view.getPaddingRight();
        int baseBottom = view.getPaddingBottom();
        view.setOnApplyWindowInsetsListener((target, insets) -> {
            android.graphics.Insets safe = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            target.setPadding(
                    baseLeft + safe.left,
                    baseTop + safe.top,
                    baseRight + safe.right,
                    baseBottom + safe.bottom);
            return insets;
        });
        view.requestApplyInsets();
    }

    private String t(String english, String germanText) {
        return german ? germanText : english;
    }

    private int dp(int value) {
        return GeoUi.dp(this, value);
    }

    private static boolean isAllowedInternalPage(Uri uri) {
        return uri != null
                && "https".equalsIgnoreCase(uri.getScheme())
                && INTERNAL_HOST.equalsIgnoreCase(uri.getHost())
                && "/map.html".equals(uri.getPath());
    }

    private static boolean isAllowedResource(Uri uri) {
        if (isAllowedInternalPage(uri)) {
            return true;
        }
        if (uri == null
                || !"https".equalsIgnoreCase(uri.getScheme())
                || !TILE_HOST.equalsIgnoreCase(uri.getHost())
                || uri.getPort() != -1
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null) {
            return false;
        }
        String path = uri.getPath();
        return path != null && path.matches("/\\d{1,2}/\\d+/\\d+\\.png");
    }

    private static WebResourceResponse blockedResponse() {
        return new WebResourceResponse(
                "text/plain",
                StandardCharsets.UTF_8.name(),
                new ByteArrayInputStream(new byte[0]));
    }

    private final class RestrictedWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            if (!request.isForMainFrame()) {
                return false;
            }
            return !isAllowedInternalPage(request.getUrl());
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return !isAllowedInternalPage(Uri.parse(url));
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(
                WebView view,
                WebResourceRequest request) {
            return isAllowedResource(request.getUrl()) ? null : blockedResponse();
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            return isAllowedResource(Uri.parse(url)) ? null : blockedResponse();
        }
    }

    private final class MapBridge {
        @JavascriptInterface
        public void onLocationSelected(double latitude, double longitude) {
            if (!validLatitude(latitude) || !validLongitude(longitude)) {
                return;
            }
            runOnUiThread(() -> {
                selectedLatitude = latitude;
                selectedLongitude = longitude;
                hasSelection = true;
                setUseButtonEnabled(true);
                updateCoordinateText();
            });
        }
    }
}
