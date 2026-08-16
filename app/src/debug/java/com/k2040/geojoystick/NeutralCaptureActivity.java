/*
 * SPDX-License-Identifier: GPL-3.0-only
 * Copyright (c) 2026 K2040.
 */
package com.k2040.geojoystick;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;

/** Debug-only blank surface used behind real overlay store captures. */
public final class NeutralCaptureActivity extends Activity {
    private static final int BACKGROUND = Color.rgb(236, 239, 241);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                int flags = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                controller.setSystemBarsAppearance(flags, flags);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                            | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }

        FrameLayout background = new FrameLayout(this);
        background.setBackgroundColor(BACKGROUND);
        background.setContentDescription(null);
        setContentView(background);
    }
}
