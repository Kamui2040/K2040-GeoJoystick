package com.k2040.geojoystick;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

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
        selectedLatitude = getIntent().getDoubleExtra(EXTRA_LATITUDE, 52.520008);
        selectedLongitude = getIntent().getDoubleExtra(EXTRA_LONGITUDE, 13.404954);

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
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(false);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new MapBridge(), "AndroidBridge");
        root.addView(webView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f));

        setContentView(root);
        String url = String.format(
                Locale.US,
                "file:///android_asset/map.html?lat=%.8f&lng=%.8f",
                selectedLatitude,
                selectedLongitude);
        webView.loadUrl(url);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.removeJavascriptInterface("AndroidBridge");
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

    private String t(String english, String germanText) {
        return german ? germanText : english;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private final class MapBridge {
        @JavascriptInterface
        public void onLocationSelected(double latitude, double longitude) {
            runOnUiThread(() -> {
                selectedLatitude = latitude;
                selectedLongitude = longitude;
                updateCoordinateText();
            });
        }
    }
}
