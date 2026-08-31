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

import org.json.JSONObject;

public final class MapActivity extends Activity {
    static final String EXTRA_LATITUDE = "map_latitude";
    static final String EXTRA_LONGITUDE = "map_longitude";

    private static final String PREFS = "geojoystick";
    private static final String PREF_APPEARANCE = "app_appearance";
    private static final String APPEARANCE_SYSTEM = "system";
    private static final String APPEARANCE_DARK = "dark";
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
    private GeoUi.Palette palette;
    private GeoSettings settings;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = new GeoSettings(this);
        loadUiSettings();
        restoreInitialSelection(savedInstanceState, getIntent());

        FrameLayout stage = new FrameLayout(this);
        stage.setLayoutDirection(settings.layoutDirection());
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
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setElevation(dp(8));

        Button back = GeoUi.iconButton(this, palette, backChevron(), t(R.string.ui_164));
        back.setOnClickListener(view -> finish());

        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        titleBlock.setPaddingRelative(dp(10), 0, dp(10), 0);
        TextView title = GeoUi.text(this, t(R.string.ui_165), 17, palette.text);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        coordinateText = GeoUi.text(this, "", 11, palette.textDim);
        coordinateText.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        coordinateText.setTextDirection(View.TEXT_DIRECTION_LTR);
        coordinateText.setSingleLine(false);
        coordinateText.setMaxLines(2);
        titleBlock.addView(title);
        titleBlock.addView(coordinateText);

        String useLabel = t(R.string.ui_166);
        useButton = GeoUi.button(this, palette, useLabel, true);
        useButton.setEnabled(hasSelection);
        useButton.setAlpha(hasSelection ? 1f : 0.48f);
        useButton.setSingleLine(false);
        useButton.setMaxLines(2);
        useButton.setHorizontallyScrolling(false);
        useButton.setMinWidth(0);
        useButton.setMinimumWidth(0);
        useButton.setOnClickListener(view -> returnSelection());

        boolean stackedToolbar = shouldStackToolbar(useButton, useLabel);
        toolbar.setOrientation(
                stackedToolbar ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);

        if (stackedToolbar) {
            LinearLayout titleRow = new LinearLayout(this);
            titleRow.setOrientation(LinearLayout.HORIZONTAL);
            titleRow.setGravity(Gravity.CENTER_VERTICAL);
            titleRow.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
            titleRow.addView(titleBlock, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            toolbar.addView(titleRow, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            LinearLayout.LayoutParams useParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            useParams.topMargin = dp(6);
            toolbar.addView(useButton, useParams);
        } else {
            toolbar.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
            toolbar.addView(titleBlock, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            toolbar.addView(useButton, new LinearLayout.LayoutParams(
                    dp(128), ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        chrome.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView hint = GeoUi.text(
                this,
                t(R.string.ui_167),
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
            coordinateText.setText(t(R.string.ui_168));
            setUseButtonEnabled(false);
        }
    }

    private void localizeBundledMap(WebView view) {
        GeoSettings localeSettings = new GeoSettings(this);
        String language = localeSettings.resolvedLanguage();
        String direction = localeSettings.isRtl() ? "rtl" : "ltr";
        String title = t(R.string.ui_165);
        String message = t(R.string.map_instruction);
        String zoomIn = t(R.string.map_zoom_in);
        String zoomOut = t(R.string.map_zoom_out);
        String script = "(function(){"
                + "var localized={language:" + JSONObject.quote(language)
                + ",direction:" + JSONObject.quote(direction)
                + ",title:" + JSONObject.quote(title)
                + ",message:" + JSONObject.quote(message)
                + ",zoomIn:" + JSONObject.quote(zoomIn)
                + ",zoomOut:" + JSONObject.quote(zoomOut) + "};"
                + "document.documentElement.lang=localized.language;"
                + "document.documentElement.dir=localized.direction;"
                + "document.title=localized.title;"
                + "var message=document.getElementById('message');"
                + "if(message){message.textContent=localized.message;}"
                + "var zoomIn=document.getElementById('zoomIn');"
                + "if(zoomIn){zoomIn.setAttribute('aria-label',localized.zoomIn);}"
                + "var zoomOut=document.getElementById('zoomOut');"
                + "if(zoomOut){zoomOut.setAttribute('aria-label',localized.zoomOut);}"
                + "})();";
        view.evaluateJavascript(script, null);
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
        SharedPreferences preferences = settings.raw();
        String appearance = preferences.getString(PREF_APPEARANCE, APPEARANCE_SYSTEM);
        boolean dark = APPEARANCE_DARK.equals(appearance)
                || (APPEARANCE_SYSTEM.equals(appearance) && isSystemDarkMode());
        palette = new GeoUi.Palette(dark);
        getWindow().getDecorView().setLayoutDirection(settings.layoutDirection());
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
            coordinateText.setText(t(R.string.ui_169));
            return;
        }
        coordinateText.setText(String.format(
                Locale.US,
                t(R.string.ui_170),
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

    private String backChevron() {
        return settings.isRtl() ? "›" : "‹";
    }

    private String t(int resourceId) {
        return settings.text(resourceId);
    }

    private boolean shouldStackToolbar(Button actionButton, String actionLabel) {
        Configuration configuration = getResources().getConfiguration();
        if (configuration.fontScale > 1.10f || configuration.screenWidthDp < 360) {
            return true;
        }

        float lineWidth = dp(104);
        float longestWord = 0f;
        String trimmed = actionLabel == null ? "" : actionLabel.trim();
        if (!trimmed.isEmpty()) {
            for (String word : trimmed.split("\\s+")) {
                longestWord = Math.max(
                        longestWord,
                        actionButton.getPaint().measureText(word));
            }
        }
        float fullWidth = actionButton.getPaint().measureText(trimmed);
        return longestWord > lineWidth || fullWidth > lineWidth * 2f;
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

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (isAllowedInternalPage(Uri.parse(url))) {
                localizeBundledMap(view);
            }
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
