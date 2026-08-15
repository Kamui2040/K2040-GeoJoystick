/*
 * SPDX-License-Identifier: GPL-3.0-only
 * Copyright (c) 2026 K2040.
 * K2040-authored material in this file is also subject to the GPLv3 section 7(b)
 * attribution-preservation term in LICENSES/GPL-3.0-Section-7b-K2040.txt.
 */
package com.k2040.geojoystick;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Small dependency-free visual system shared by GeoJoystick's programmatic UI.
 *
 * <p>The project intentionally does not depend on Material Components. Keeping the visual tokens
 * here gives the activity and overlay surfaces a consistent presentation without coupling runtime
 * state to UI styling.</p>
 */
final class GeoUi {
    static final class Palette {
        final int background;
        final int surface;
        final int elevated;
        final int input;
        final int text;
        final int textDim;
        final int border;
        final int accent;
        final int accentSoft;
        final int success;
        final int warning;
        final int danger;

        Palette(boolean dark) {
            if (dark) {
                background = 0xFF07111C;
                surface = 0xFF0E1A27;
                elevated = 0xFF132333;
                input = 0xFF0A1621;
                text = 0xFFF3F7FB;
                textDim = 0xFFA8B6C5;
                border = 0xFF2B3B4C;
                accent = 0xFF2F8CFF;
                accentSoft = 0xFF173B66;
                success = 0xFF72D487;
                warning = 0xFFFFB85C;
                danger = 0xFFFF7474;
            } else {
                background = 0xFFF4F7FA;
                surface = 0xFFFFFFFF;
                elevated = 0xFFF9FBFD;
                input = 0xFFF5F8FB;
                text = 0xFF14202B;
                textDim = 0xFF647485;
                border = 0xFFD6E0E8;
                accent = 0xFF1769D2;
                accentSoft = 0xFFDDEBFF;
                success = 0xFF2E7D43;
                warning = 0xFF9A6200;
                danger = 0xFFB3261E;
            }
        }
    }

    private GeoUi() {
    }

    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    static GradientDrawable rounded(
            Context context,
            int fill,
            int radiusDp,
            int stroke,
            int strokeDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(context, radiusDp));
        if (strokeDp > 0) {
            drawable.setStroke(dp(context, strokeDp), stroke);
        }
        return drawable;
    }

    static GradientDrawable surface(Context context, Palette palette) {
        return rounded(context, palette.surface, 14, palette.border, 1);
    }

    static GradientDrawable elevated(Context context, Palette palette) {
        return rounded(context, palette.elevated, 18, palette.border, 1);
    }

    static GradientDrawable input(Context context, Palette palette) {
        return rounded(context, palette.input, 12, palette.border, 1);
    }

    static GradientDrawable primary(Context context, Palette palette) {
        return rounded(context, palette.accent, 12, palette.accent, 1);
    }

    static GradientDrawable secondary(Context context, Palette palette) {
        return rounded(context, palette.surface, 12, palette.border, 1);
    }

    static LinearLayout card(Context context, Palette palette) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10));
        card.setBackground(surface(context, palette));
        return card;
    }

    static TextView text(Context context, String value, int sizeSp, int color) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextSize(sizeSp);
        text.setTextColor(color);
        return text;
    }

    static TextView sectionLabel(Context context, Palette palette, String value) {
        TextView label = text(context, value, 12, palette.textDim);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setAllCaps(true);
        label.setLetterSpacing(0.08f);
        label.setPadding(dp(context, 2), dp(context, 4), dp(context, 2), dp(context, 3));
        return label;
    }

    static Button button(
            Context context,
            Palette palette,
            String label,
            boolean primary) {
        Button button = new Button(context);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTextColor(primary ? 0xFFFFFFFF : palette.text);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(context, 48));
        button.setMinimumHeight(dp(context, 48));
        button.setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 8));
        button.setBackground(primary ? primary(context, palette) : secondary(context, palette));
        button.setStateListAnimator(null);
        return button;
    }

    static Button iconButton(
            Context context,
            Palette palette,
            String symbol,
            String description) {
        Button button = button(context, palette, symbol, false);
        button.setTextSize(21);
        button.setContentDescription(description);
        button.setPadding(0, 0, 0, 0);
        return button;
    }

    static LinearLayout.LayoutParams matchWidth(Context context, int topDp, int bottomDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(context, topDp);
        params.bottomMargin = dp(context, bottomDp);
        return params;
    }

    static LinearLayout.LayoutParams weighted(Context context, int horizontalMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f);
        params.leftMargin = dp(context, horizontalMarginDp);
        params.rightMargin = dp(context, horizontalMarginDp);
        return params;
    }

    static void setAccessible(View view, String description) {
        view.setContentDescription(description);
        view.setMinimumWidth(dp(view.getContext(), 48));
        view.setMinimumHeight(dp(view.getContext(), 48));
    }
}
