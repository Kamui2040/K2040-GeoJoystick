package com.k2040.geojoystick;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
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

    private double selectedLatitude;
    private double selectedLongitude;
    private TextView coordinateText;
    private WebView webView;
    private boolean german;
    private int colorBackground;
    private int colorText;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadUiSettings();

        double requestedLatitude = getIntent().getDoubleExtra(EXTRA_LATITUDE, 52.520008);
        double requestedLongitude = getIntent().getDoubleExtra(EXTRA_LONGITUDE, 13.404954);
        selectedLatitude = validLatitude(requestedLatitude) ? requestedLatitude : 52.520008;
        selectedLongitude = validLongitude(requestedLongitude) ? requestedLongitude : 13.404954;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(colorBackground);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(8), dp(5), dp(8), dp(5));
        toolbar.setBackgroundColor(colorBackground);

        coordinateText = new TextView(this);
        coordinateText.setTextSize(13);
        coordinateText.setTextColor(colorText);
        updateCoordinateText();
        toolbar.addView(coordinateText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button cancel = new Button(this);
        cancel.setText(t("Cancel", "Abbrechen"));
        cancel.setAllCaps(false);
        cancel.setOnClickListener(view -> finish());
        toolbar.addView(cancel);

        Button use = new Button(this);
        use.setText(t("Use location", "Standort nutzen"));
        use.setAllCaps(false);
        use.setOnClickListener(view -> returnSelection());
        toolbar.addView(use);
        root.addView(toolbar);

        webView = new WebView(this);
        configureWebView(webView);
        root.addView(webView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f));

        setContentView(root);
        applySystemBarInsets(root);
        loadBundledMap();
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

        WebView.setWebContentsDebuggingEnabled(false);
        view.setWebViewClient(new RestrictedWebViewClient());
        view.addJavascriptInterface(new MapBridge(), "AndroidBridge");
    }

    private void loadBundledMap() {
        try {
            String html = readAsset("map.html");
            String baseUrl = String.format(
                    Locale.US,
                    "https://%s/map.html?lat=%.8f&lng=%.8f",
                    INTERNAL_HOST,
                    selectedLatitude,
                    selectedLongitude);
            webView.loadDataWithBaseURL(baseUrl, html, "text/html", StandardCharsets.UTF_8.name(), null);
        } catch (IOException exception) {
            coordinateText.setText(t("Map unavailable", "Karte nicht verfügbar"));
        }
    }

    private String readAsset(String name) throws IOException {
        StringBuilder content = new StringBuilder();
        try (InputStream input = getAssets().open(name);
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) >= 0) {
                content.append(buffer, 0, read);
            }
        }
        return content.toString();
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
                || (LANGUAGE_SYSTEM.equals(language) && Locale.getDefault().getLanguage().equals("de"));
        String appearance = preferences.getString(PREF_APPEARANCE, APPEARANCE_SYSTEM);
        boolean dark = APPEARANCE_DARK.equals(appearance)
                || (APPEARANCE_SYSTEM.equals(appearance) && isSystemDarkMode());
        colorBackground = dark ? 0xFF10171C : Color.WHITE;
        colorText = dark ? 0xFFECEFF1 : 0xFF263238;
    }

    private boolean isSystemDarkMode() {
        int nightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private void updateCoordinateText() {
        coordinateText.setText(String.format(
                Locale.US,
                t("Selected: %.6f, %.6f", "Ausgewählt: %.6f, %.6f"),
                selectedLatitude,
                selectedLongitude));
    }

    private void returnSelection() {
        Intent result = new Intent();
        result.putExtra(EXTRA_LATITUDE, selectedLatitude);
        result.putExtra(EXTRA_LONGITUDE, selectedLongitude);
        setResult(RESULT_OK, result);
        finish();
    }

    private boolean validLatitude(double value) {
        return Double.isFinite(value) && value >= -90.0 && value <= 90.0;
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
        return Math.round(value * getResources().getDisplayMetrics().density);
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
                || !TILE_HOST.equalsIgnoreCase(uri.getHost())) {
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
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
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
                updateCoordinateText();
            });
        }
    }
}
