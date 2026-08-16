/*
 * SPDX-License-Identifier: GPL-3.0-only
 * Copyright (c) 2026 K2040.
 */
package com.k2040.geojoystick;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;

/** Debug-only blank surface used behind real overlay store captures. */
public final class NeutralCaptureActivity extends Activity {
    private static final int BACKGROUND = Color.rgb(236, 239, 241);
    static final String EXTRA_STOP_SIMULATION = "geojoystick_debug_stop_simulation";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null
                && getIntent().getBooleanExtra(EXTRA_STOP_SIMULATION, false)) {
            stopService(new Intent(this, MockLocationService.class));
            finish();
            return;
        }

        FrameLayout background = new FrameLayout(this);
        background.setBackgroundColor(BACKGROUND);
        background.setContentDescription(null);
        setContentView(background);

        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);

        View decorView = getWindow().getDecorView();
        int legacyFlags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        decorView.setSystemUiVisibility(legacyFlags);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = decorView.getWindowInsetsController();
            if (controller != null) {
                int flags = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                controller.setSystemBarsAppearance(flags, flags);
            }
        }
    }
}
