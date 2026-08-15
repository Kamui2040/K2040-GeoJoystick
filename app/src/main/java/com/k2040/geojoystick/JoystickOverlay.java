package com.k2040.geojoystick;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

final class JoystickOverlay {
    interface Listener {
        void onVectorChanged(double east, double north);
        void onSpeedChanged(double metersPerSecond);
        void onStopRequested();
    }

    private static final String PREFS = "geojoystick";
    private static final String PREF_OVERLAY_X = "overlay_x";
    private static final String PREF_OVERLAY_Y = "overlay_y";
    private static final String PREF_OVERLAY_COMPACT_MODE = "overlay_compact_mode";
    private static final String PREF_SELECTED_SPEED = "overlay_selected_speed";
    private static final String PREF_SELECTED_SPEED_KIND = "overlay_selected_speed_kind";
    private static final String PREF_OVERLAY_OPACITY = "overlay_opacity_percent";
    private static final String PREF_OVERLAY_SIZE = "overlay_size_percent";
    private static final String PREF_OVERLAY_HIGH_CONTRAST = "overlay_high_contrast";
    private static final String PREF_CUSTOM_SPEED = "overlay_custom_speed";
    private static final String PREF_CUSTOM_SPEED_NAME = "overlay_custom_speed_name";
    private static final String PREF_LANGUAGE = "app_language";
    private static final String LANGUAGE_SYSTEM = "system";
    private static final String LANGUAGE_GERMAN = "de";

    private static final String SPEED_WALK = "walk";
    private static final String SPEED_RUN = "run";
    private static final String SPEED_BIKE = "bike";
    private static final String SPEED_CUSTOM = "custom";
    private static final double WALK_SPEED = 1.2;
    private static final double RUN_SPEED = 3.6;
    private static final double BIKE_SPEED = 10.0;

    private static final int ICON_WALK = 1;
    private static final int ICON_RUN = 2;
    private static final int ICON_BIKE = 3;
    private static final int ICON_GAUGE = 4;
    private static final int ICON_LOCK = 5;
    private static final int ICON_PLAY_PAUSE = 6;
    private static final int ICON_STOP = 7;

    private final Context context;
    private final Listener listener;
    private final WindowManager windowManager;
    private final SharedPreferences preferences;
    private final WindowManager.LayoutParams params;
    private final LinearLayout root;
    private final LinearLayout titleRow;
    private final DragTextView dragHandle;
    private final DragButton toggleModeButton;
    private final JoystickView joystickView;
    private final LinearLayout speedRow;
    private final LinearLayout controlRow;
    private final TextView coordinateText;
    private final IconButton walkButton;
    private final IconButton runButton;
    private final IconButton bikeButton;
    private final IconButton customButton;
    private final IconButton pauseButton;
    private final IconButton holdButton;
    private final IconButton stopButton;

    private boolean shown;
    private boolean compactMode;
    private boolean holdEnabled;
    private boolean paused;
    private boolean highContrast;
    private int overlayOpacityPercent;
    private int overlaySizePercent;
    private int colorPanel;
    private int colorBorder;
    private int colorButton;
    private int colorButtonActive;
    private int colorText;
    private int colorTextDim;
    private int colorAccent;
    private String selectedSpeedKind;
    private double currentLatitude = Double.NaN;
    private double currentLongitude = Double.NaN;
    private double currentSpeed = WALK_SPEED;

    JoystickOverlay(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        loadStyleSettings(true);

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = preferences.getInt(PREF_OVERLAY_X, dp(24));
        params.y = preferences.getInt(PREF_OVERLAY_Y, dp(120));

        compactMode = preferences.getBoolean(PREF_OVERLAY_COMPACT_MODE, false);
        selectedSpeedKind = loadSavedSpeedKind();
        currentSpeed = speedForKind(selectedSpeedKind);

        root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(6), dp(6), dp(6), dp(8));
        root.setBackground(panelBackground());
        root.setElevation(dp(10));

        titleRow = new LinearLayout(context);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        dragHandle = new DragTextView(context);
        dragHandle.setText(R.string.app_name);
        dragHandle.setTextColor(colorText);
        dragHandle.setTextSize(13);
        dragHandle.setGravity(Gravity.CENTER_VERTICAL);
        dragHandle.setPadding(dp(4), 0, dp(6), 0);
        dragHandle.setContentDescription(t("Move GeoJoystick overlay", "GeoJoystick-Overlay verschieben"));
        dragHandle.setOnTouchListener(new DragListener());
        titleRow.addView(dragHandle, new LinearLayout.LayoutParams(0, dp(48), 1f));

        toggleModeButton = controlButton("−", 10);
        toggleModeButton.setContentDescription(t("Switch to compact overlay", "Zum kompakten Overlay wechseln"));
        toggleModeButton.setOnClickListener(view -> toggleOverlayMode());
        toggleModeButton.setOnTouchListener(new DragListener());
        titleRow.addView(toggleModeButton, new LinearLayout.LayoutParams(dp(48), dp(48)));
        root.addView(titleRow, new LinearLayout.LayoutParams(dp(200), dp(48)));

        joystickView = new JoystickView(context);
        joystickView.setHighContrast(highContrast);
        joystickView.setOverlayOpacity(overlayOpacityPercent);
        joystickView.setListener((east, north) -> {
            if (paused) {
                listener.onVectorChanged(0.0, 0.0);
            } else {
                listener.onVectorChanged(east, north);
            }
        });
        LinearLayout.LayoutParams joystickParams = new LinearLayout.LayoutParams(dp(106), dp(106));
        joystickParams.gravity = Gravity.CENTER_HORIZONTAL;
        joystickParams.topMargin = dp(2);
        joystickParams.bottomMargin = dp(6);
        root.addView(joystickView, joystickParams);

        speedRow = new LinearLayout(context);
        speedRow.setGravity(Gravity.CENTER);
        walkButton = iconButton(ICON_WALK, t("Walk speed", "Gehgeschwindigkeit"));
        runButton = iconButton(ICON_RUN, t("Run speed", "Laufgeschwindigkeit"));
        bikeButton = iconButton(ICON_BIKE, t("Bike speed", "Fahrradgeschwindigkeit"));
        customButton = iconButton(ICON_GAUGE, t("Custom speed", "Eigene Geschwindigkeit"));
        walkButton.setOnClickListener(view -> setSpeed(SPEED_WALK));
        runButton.setOnClickListener(view -> setSpeed(SPEED_RUN));
        bikeButton.setOnClickListener(view -> setSpeed(SPEED_BIKE));
        customButton.setOnClickListener(view -> setSpeed(SPEED_CUSTOM));
        addIconButton(speedRow, walkButton);
        addIconButton(speedRow, runButton);
        addIconButton(speedRow, bikeButton);
        addIconButton(speedRow, customButton);
        root.addView(speedRow, new LinearLayout.LayoutParams(dp(200), dp(48)));

        controlRow = new LinearLayout(context);
        controlRow.setGravity(Gravity.CENTER);
        pauseButton = iconButton(ICON_PLAY_PAUSE,
                t("Movement active; tap to pause", "Bewegung aktiv; zum Pausieren tippen"));
        holdButton = iconButton(ICON_LOCK, t("Enable hold", "Halten aktivieren"));
        stopButton = iconButton(ICON_STOP,
                t("Stop GeoJoystick service and close overlay", "GeoJoystick-Dienst stoppen und Overlay schließen"));
        pauseButton.setOnClickListener(view -> togglePause());
        holdButton.setOnClickListener(view -> toggleHold());
        stopButton.setOnClickListener(view -> stopMovement());
        addControlButton(controlRow, pauseButton);
        addControlButton(controlRow, holdButton);
        addControlButton(controlRow, stopButton);
        root.addView(controlRow, rowParams(200, 48, 2));

        coordinateText = new TextView(context);
        coordinateText.setTextColor(colorTextDim);
        coordinateText.setTextSize(10);
        coordinateText.setGravity(Gravity.CENTER);
        coordinateText.setSingleLine(true);
        coordinateText.setPadding(dp(3), dp(2), dp(3), 0);
        root.addView(coordinateText, new LinearLayout.LayoutParams(dp(200), dp(28)));

        updateSpeedButtonStates();
        updateToggleStates();
        updateCoordinateText();
        applyOverlayMode();
    }

    void show() {
        if (shown || !Settings.canDrawOverlays(context)) return;
        windowManager.addView(root, params);
        shown = true;
    }

    void hide() {
        joystickView.reset();
        if (shown) {
            windowManager.removeView(root);
            shown = false;
        }
    }

    void destroy() {
        joystickView.reset();
        if (shown) {
            windowManager.removeViewImmediate(root);
            shown = false;
        }
    }

    void updatePosition(double latitude, double longitude, double speed) {
        currentLatitude = latitude;
        currentLongitude = longitude;
        currentSpeed = normalizeSpeed(speed);
        coordinateText.post(() -> {
            if (loadStyleSettings(false)) {
                applyOverlayMode();
                updateSpeedButtonStates();
                updateToggleStates();
            }
            updateCoordinateText();
        });
    }

    private void setSpeed(String kind) {
        selectedSpeedKind = kind;
        currentSpeed = speedForKind(kind);
        preferences.edit()
                .putString(PREF_SELECTED_SPEED_KIND, selectedSpeedKind)
                .putLong(PREF_SELECTED_SPEED, Double.doubleToRawLongBits(currentSpeed))
                .apply();
        listener.onSpeedChanged(currentSpeed);
        updateSpeedButtonStates();
        updateCoordinateText();
    }

    private void toggleHold() {
        holdEnabled = !holdEnabled;
        joystickView.setHoldEnabled(holdEnabled);
        updateToggleStates();
    }

    private void togglePause() {
        paused = !paused;
        if (paused) joystickView.reset();
        updateToggleStates();
    }

    private void stopMovement() {
        joystickView.reset();
        listener.onStopRequested();
    }

    private void toggleOverlayMode() {
        compactMode = !compactMode;
        preferences.edit().putBoolean(PREF_OVERLAY_COMPACT_MODE, compactMode).apply();
        applyOverlayMode();
    }

    private void applyOverlayMode() {
        int expanded = compactMode ? View.GONE : View.VISIBLE;
        dragHandle.setVisibility(expanded);
        speedRow.setVisibility(expanded);
        controlRow.setVisibility(expanded);
        coordinateText.setVisibility(expanded);
        toggleModeButton.setText(compactMode ? "+" : "−");
        toggleModeButton.setContentDescription(compactMode
                ? t("Switch to expanded overlay", "Zum erweiterten Overlay wechseln")
                : t("Switch to compact overlay", "Zum kompakten Overlay wechseln"));
        styleToggleButton();

        LinearLayout.LayoutParams toggleParams = (LinearLayout.LayoutParams) toggleModeButton.getLayoutParams();
        toggleParams.width = dp(48);
        toggleParams.height = dp(48);
        toggleModeButton.setLayoutParams(toggleParams);

        int panelWidth = Math.max(dp(200), scaledDp(216));
        int joystickSize = compactMode
                ? Math.max(dp(88), scaledDp(116))
                : Math.max(dp(92), scaledDp(132));

        LinearLayout.LayoutParams titleParams = (LinearLayout.LayoutParams) titleRow.getLayoutParams();
        titleParams.width = compactMode ? Math.max(dp(104), joystickSize) : panelWidth;
        titleParams.height = dp(48);
        titleRow.setLayoutParams(titleParams);
        titleRow.setGravity(compactMode ? Gravity.END : Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams joystickParams = (LinearLayout.LayoutParams) joystickView.getLayoutParams();
        joystickParams.width = joystickSize;
        joystickParams.height = joystickSize;
        joystickParams.topMargin = compactMode ? 0 : dp(2);
        joystickParams.bottomMargin = compactMode ? dp(2) : dp(6);
        joystickView.setLayoutParams(joystickParams);

        LinearLayout.LayoutParams speedParams = (LinearLayout.LayoutParams) speedRow.getLayoutParams();
        speedParams.width = panelWidth;
        speedParams.height = dp(48);
        speedRow.setLayoutParams(speedParams);

        LinearLayout.LayoutParams controlParams = (LinearLayout.LayoutParams) controlRow.getLayoutParams();
        controlParams.width = panelWidth;
        controlParams.height = dp(48);
        controlParams.topMargin = dp(2);
        controlRow.setLayoutParams(controlParams);

        LinearLayout.LayoutParams coordinateParams = (LinearLayout.LayoutParams) coordinateText.getLayoutParams();
        coordinateParams.width = panelWidth;
        coordinateParams.height = dp(28);
        coordinateText.setLayoutParams(coordinateParams);

        if (compactMode) {
            root.setPadding(0, 0, 0, 0);
            root.setBackground(null);
            root.setElevation(0);
        } else {
            root.setPadding(dp(6), dp(6), dp(6), dp(8));
            root.setBackground(panelBackground());
            root.setElevation(dp(10));
        }
        root.requestLayout();
    }

    private void updateSpeedButtonStates() {
        styleIconButton(walkButton, SPEED_WALK.equals(selectedSpeedKind));
        styleIconButton(runButton, SPEED_RUN.equals(selectedSpeedKind));
        styleIconButton(bikeButton, SPEED_BIKE.equals(selectedSpeedKind));
        styleIconButton(customButton, SPEED_CUSTOM.equals(selectedSpeedKind));
        customButton.setContentDescription(t("Custom speed: ", "Eigene Geschwindigkeit: ") + customSpeedName());
    }

    private void updateToggleStates() {
        pauseButton.setContentDescription(paused
                ? t("Movement paused; tap to resume", "Bewegung pausiert; zum Fortsetzen tippen")
                : t("Movement active; tap to pause", "Bewegung aktiv; zum Pausieren tippen"));
        styleIconButton(pauseButton, !paused);
        holdButton.setContentDescription(holdEnabled
                ? t("Disable hold", "Halten deaktivieren")
                : t("Enable hold", "Halten aktivieren"));
        styleIconButton(holdButton, holdEnabled);
        styleIconButton(stopButton, false);
    }

    private void updateCoordinateText() {
        if (!Double.isFinite(currentLatitude) || !Double.isFinite(currentLongitude)) {
            coordinateText.setText(t("Position unavailable", "Position nicht verfügbar"));
            return;
        }
        coordinateText.setText(String.format(Locale.US,
                "%.5f, %.5f · %.1f m/s", currentLatitude, currentLongitude, currentSpeed));
    }

    private String loadSavedSpeedKind() {
        String kind = preferences.getString(PREF_SELECTED_SPEED_KIND, null);
        if (SPEED_WALK.equals(kind) || SPEED_RUN.equals(kind)
                || SPEED_BIKE.equals(kind) || SPEED_CUSTOM.equals(kind)) return kind;
        double saved = Double.longBitsToDouble(preferences.getLong(
                PREF_SELECTED_SPEED, Double.doubleToLongBits(WALK_SPEED)));
        if (Math.abs(saved - RUN_SPEED) < 0.05) return SPEED_RUN;
        if (Math.abs(saved - BIKE_SPEED) < 0.05) return SPEED_BIKE;
        if (Math.abs(saved - customSpeed()) < 0.05) return SPEED_CUSTOM;
        return SPEED_WALK;
    }

    private double speedForKind(String kind) {
        if (SPEED_RUN.equals(kind)) return RUN_SPEED;
        if (SPEED_BIKE.equals(kind)) return BIKE_SPEED;
        if (SPEED_CUSTOM.equals(kind)) return customSpeed();
        return WALK_SPEED;
    }

    private double normalizeSpeed(double speed) {
        if (!Double.isFinite(speed)) return WALK_SPEED;
        if (Math.abs(speed - RUN_SPEED) < 0.05) return RUN_SPEED;
        if (Math.abs(speed - BIKE_SPEED) < 0.05) return BIKE_SPEED;
        double custom = customSpeed();
        if (Math.abs(speed - custom) < 0.05) return custom;
        if (Math.abs(speed - WALK_SPEED) < 0.05) return WALK_SPEED;
        return Math.max(0.1, Math.min(50.0, speed));
    }

    private double customSpeed() {
        double saved = Double.longBitsToDouble(preferences.getLong(
                PREF_CUSTOM_SPEED, Double.doubleToLongBits(5.0)));
        return Double.isFinite(saved) ? Math.max(0.1, Math.min(50.0, saved)) : 5.0;
    }

    private String customSpeedName() {
        String name = preferences.getString(PREF_CUSTOM_SPEED_NAME, "Custom");
        return name == null || name.trim().isEmpty() ? "Custom" : name.trim();
    }

    private String t(String english, String germanText) {
        String language = preferences.getString(PREF_LANGUAGE, LANGUAGE_SYSTEM);
        boolean german = LANGUAGE_GERMAN.equals(language)
                || (LANGUAGE_SYSTEM.equals(language) && Locale.getDefault().getLanguage().equals("de"));
        return german ? germanText : english;
    }

    private boolean loadStyleSettings(boolean force) {
        int newOpacity = Math.max(30, Math.min(100, preferences.getInt(PREF_OVERLAY_OPACITY, 85)));
        int newSize = Math.max(70, Math.min(120, preferences.getInt(PREF_OVERLAY_SIZE, 80)));
        boolean newContrast = preferences.getBoolean(PREF_OVERLAY_HIGH_CONTRAST, false);
        if (!force
                && newOpacity == overlayOpacityPercent
                && newSize == overlaySizePercent
                && newContrast == highContrast) return false;
        overlayOpacityPercent = newOpacity;
        overlaySizePercent = newSize;
        highContrast = newContrast;
        int panelAlpha = Math.round(255.0f * overlayOpacityPercent / 100.0f);
        colorPanel = argb(panelAlpha, 0x0B, 0x16, 0x22);
        colorBorder = highContrast ? 0xFFF3F7FB : 0xB04A6176;
        colorButton = highContrast ? 0xCC17293A : 0xA817293A;
        colorButtonActive = highContrast ? 0xFF2F8CFF : 0xE62F8CFF;
        colorText = 0xFFF3F7FB;
        colorTextDim = highContrast ? 0xFFF3F7FB : 0xD9A8B6C5;
        colorAccent = highContrast ? 0xFFFFFFFF : 0xFF58A6FF;
        if (root != null) {
            dragHandle.setTextColor(colorText);
            coordinateText.setTextColor(colorTextDim);
            joystickView.setHighContrast(highContrast);
            joystickView.setOverlayOpacity(overlayOpacityPercent);
        }
        return true;
    }

    private int argb(int alpha, int red, int green, int blue) {
        return ((alpha & 0xFF) << 24) | ((red & 0xFF) << 16)
                | ((green & 0xFF) << 8) | (blue & 0xFF);
    }

    private DragButton controlButton(String text, int textSize) {
        DragButton button = new DragButton(context);
        button.setText(text);
        button.setTextColor(colorTextDim);
        button.setTextSize(textSize);
        button.setAllCaps(false);
        button.setMinHeight(dp(48));
        button.setMinWidth(dp(48));
        button.setMinimumHeight(dp(48));
        button.setMinimumWidth(dp(48));
        button.setPadding(0, 0, 0, 0);
        button.setBackground(buttonBackground(false));
        button.setStateListAnimator(null);
        return button;
    }

    private IconButton iconButton(int iconType, String description) {
        IconButton button = new IconButton(context, iconType);
        button.setContentDescription(description);
        button.setText("");
        button.setAllCaps(false);
        button.setMinHeight(dp(48));
        button.setMinWidth(dp(48));
        button.setMinimumHeight(dp(48));
        button.setMinimumWidth(dp(48));
        button.setPadding(0, 0, 0, 0);
        button.setBackground(buttonBackground(false));
        button.setStateListAnimator(null);
        return button;
    }

    private void addIconButton(LinearLayout row, Button button) {
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        buttonParams.leftMargin = dp(1);
        buttonParams.rightMargin = dp(1);
        row.addView(button, buttonParams);
    }

    private void addControlButton(LinearLayout row, Button button) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(48), dp(48));
        params.leftMargin = dp(4);
        params.rightMargin = dp(4);
        row.addView(button, params);
    }

    private LinearLayout.LayoutParams rowParams(int widthDp, int heightDp, int topMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(widthDp), dp(heightDp));
        params.topMargin = dp(topMarginDp);
        return params;
    }

    private void styleToggleButton() {
        toggleModeButton.setTextColor(highContrast ? colorText : colorTextDim);
        toggleModeButton.setBackground(null);
    }

    private void styleIconButton(IconButton button, boolean active) {
        button.setIconActive(active, highContrast);
        button.setBackground(buttonBackground(active));
    }

    private GradientDrawable panelBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(colorPanel);
        drawable.setCornerRadius(dp(16));
        drawable.setStroke(dp(highContrast ? 2 : 1), colorBorder);
        return drawable;
    }

    private Drawable buttonBackground(boolean active) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(active ? colorButtonActive : colorButton);
        drawable.setCornerRadius(dp(8));
        drawable.setStroke(dp(active || highContrast ? 2 : 1), active ? colorAccent : colorBorder);
        return new InsetDrawable(drawable, dp(8));
    }

    private int scaledDp(int baseDp) {
        return dp(Math.round(baseDp * overlaySizePercent / 100.0f));
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static final class DragTextView extends TextView {
        DragTextView(Context context) { super(context); }
        @Override public boolean performClick() { super.performClick(); return true; }
    }

    private static final class DragButton extends Button {
        DragButton(Context context) { super(context); }
        @Override public boolean performClick() { super.performClick(); return true; }
    }

    private final class DragListener implements View.OnTouchListener {
        private int startX;
        private int startY;
        private float startRawX;
        private float startRawY;
        private boolean moved;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    startX = params.x;
                    startY = params.y;
                    startRawX = event.getRawX();
                    startRawY = event.getRawY();
                    moved = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    int dx = Math.round(event.getRawX() - startRawX);
                    int dy = Math.round(event.getRawY() - startRawY);
                    if (Math.abs(dx) > dp(3) || Math.abs(dy) > dp(3)) moved = true;
                    params.x = startX + dx;
                    params.y = startY + dy;
                    if (shown) windowManager.updateViewLayout(root, params);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    preferences.edit()
                            .putInt(PREF_OVERLAY_X, params.x)
                            .putInt(PREF_OVERLAY_Y, params.y)
                            .apply();
                    if (!moved) view.performClick();
                    return true;
                default:
                    return false;
            }
        }
    }

    private final class IconButton extends Button {
        private final int iconType;
        private boolean active;
        private boolean contrast;
        private Drawable iconDrawable;
        private int iconResId;

        IconButton(Context context, int iconType) {
            super(context);
            this.iconType = iconType;
            setWillNotDraw(false);
        }

        void setIconActive(boolean active, boolean contrast) {
            this.active = active;
            this.contrast = contrast;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int resId = iconResource();
            if (resId == 0) return;
            if (iconDrawable == null || iconResId != resId) {
                iconDrawable = context.getDrawable(resId);
                if (iconDrawable != null) iconDrawable = iconDrawable.mutate();
                iconResId = resId;
            }
            if (iconDrawable == null) return;
            int side = Math.min(dp(iconSizeDp()), Math.min(getWidth(), getHeight()));
            int left = (getWidth() - side) / 2;
            int top = (getHeight() - side) / 2;
            iconDrawable.setBounds(left, top, left + side, top + side);
            iconDrawable.setTint(active ? colorText : (contrast ? colorText : colorTextDim));
            iconDrawable.setAlpha(active || contrast ? 255 : 230);
            iconDrawable.draw(canvas);
        }

        private int iconResource() {
            switch (iconType) {
                case ICON_WALK: return active ? R.drawable.overlay_ic_walk_filled : R.drawable.overlay_ic_walk_outline;
                case ICON_RUN: return active ? R.drawable.overlay_ic_run_filled : R.drawable.overlay_ic_run_outline;
                case ICON_BIKE: return active ? R.drawable.overlay_ic_bike_filled : R.drawable.overlay_ic_bike_outline;
                case ICON_GAUGE: return active ? R.drawable.overlay_ic_gauge_filled : R.drawable.overlay_ic_gauge_outline;
                case ICON_LOCK: return active ? R.drawable.overlay_ic_lock_filled : R.drawable.overlay_ic_lock_outline;
                case ICON_PLAY_PAUSE: return active ? R.drawable.overlay_ic_play_filled : R.drawable.overlay_ic_pause_outline;
                case ICON_STOP: return R.drawable.overlay_ic_x_outline;
                default: return 0;
            }
        }

        private int iconSizeDp() {
            if (iconType == ICON_STOP) return 10;
            if (iconType == ICON_BIKE || iconType == ICON_GAUGE) return 12;
            return 11;
        }
    }
}
