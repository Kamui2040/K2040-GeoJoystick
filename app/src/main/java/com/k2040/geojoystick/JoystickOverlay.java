package com.k2040.geojoystick;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
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
    private static final String PREF_OVERLAY_HIGH_CONTRAST = "overlay_high_contrast";
    private static final String PREF_CUSTOM_SPEED = "overlay_custom_speed";
    private static final String PREF_CUSTOM_SPEED_NAME = "overlay_custom_speed_name";

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
    private final TextView dragHandle;
    private final JoystickView joystickView;
    private final LinearLayout speedRow;
    private final LinearLayout controlRow;
    private final TextView coordinateText;
    private final Button toggleModeButton;
    private final IconButton walkButton;
    private final IconButton runButton;
    private final IconButton bikeButton;
    private final IconButton customButton;
    private final IconButton holdButton;
    private final IconButton pauseButton;
    private final IconButton stopButton;
    private boolean shown;
    private boolean compactMode;
    private boolean holdEnabled;
    private boolean paused;
    private boolean highContrast;
    private int overlayOpacityPercent;
    private int colorPanel;
    private int colorBorder;
    private int colorButton;
    private int colorButtonActive;
    private int colorButtonCompact;
    private int colorText;
    private int colorTextDim;
    private int colorAccent;
    private String selectedSpeedKind;
    private double currentLatitude = 52.520008;
    private double currentLongitude = 13.404954;
    private double currentSpeed = WALK_SPEED;

    JoystickOverlay(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
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
        root.setPadding(dp(4), dp(3), dp(4), dp(4));
        root.setBackground(panelBackground());

        titleRow = new LinearLayout(context);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        dragHandle = new TextView(context);
        dragHandle.setText("GeoJoystick");
        dragHandle.setTextColor(colorText);
        dragHandle.setTextSize(7);
        dragHandle.setPadding(dp(2), dp(1), dp(4), dp(1));
        dragHandle.setOnTouchListener(new DragListener());
        titleRow.addView(dragHandle, new LinearLayout.LayoutParams(0, dp(18), 1f));

        toggleModeButton = controlButton("−", 7);
        toggleModeButton.setContentDescription("Switch to compact overlay");
        toggleModeButton.setOnClickListener(view -> toggleOverlayMode());
        toggleModeButton.setOnTouchListener(new DragListener());
        titleRow.addView(toggleModeButton, new LinearLayout.LayoutParams(dp(18), dp(16)));
        root.addView(titleRow, new LinearLayout.LayoutParams(dp(118), dp(19)));

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
        LinearLayout.LayoutParams joystickParams = new LinearLayout.LayoutParams(dp(88), dp(88));
        joystickParams.gravity = Gravity.CENTER_HORIZONTAL;
        joystickParams.topMargin = dp(1);
        joystickParams.bottomMargin = dp(4);
        root.addView(joystickView, joystickParams);

        speedRow = new LinearLayout(context);
        speedRow.setGravity(Gravity.CENTER);
        walkButton = iconButton(ICON_WALK, "Walk speed");
        runButton = iconButton(ICON_RUN, "Run speed");
        bikeButton = iconButton(ICON_BIKE, "Bike speed");
        customButton = iconButton(ICON_GAUGE, "Custom speed");
        walkButton.setOnClickListener(view -> setSpeed(SPEED_WALK));
        runButton.setOnClickListener(view -> setSpeed(SPEED_RUN));
        bikeButton.setOnClickListener(view -> setSpeed(SPEED_BIKE));
        customButton.setOnClickListener(view -> setSpeed(SPEED_CUSTOM));
        addSpeedButtonToRow(speedRow, walkButton);
        addSpeedButtonToRow(speedRow, runButton);
        addSpeedGroupSpacer(speedRow);
        addSpeedButtonToRow(speedRow, bikeButton);
        addSpeedButtonToRow(speedRow, customButton);
        root.addView(speedRow, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(23)));

        controlRow = new LinearLayout(context);
        controlRow.setGravity(Gravity.CENTER_VERTICAL);
        pauseButton = iconButton(ICON_PLAY_PAUSE, "Movement active; tap to pause");
        holdButton = iconButton(ICON_LOCK, "Enable hold");
        stopButton = iconButton(ICON_STOP, "Stop GeoJoystick service and close overlay");
        pauseButton.setContentDescription("Movement active; tap to pause");
        stopButton.setContentDescription("Stop GeoJoystick service and close overlay");
        pauseButton.setOnClickListener(view -> togglePause());
        holdButton.setOnClickListener(view -> toggleHold());
        stopButton.setOnClickListener(view -> stopMovement());
        addControlButtonLeft(controlRow, pauseButton);
        addControlSpacer(controlRow);
        addControlButtonCenter(controlRow, holdButton);
        addControlSpacer(controlRow);
        addControlButtonRight(controlRow, stopButton);
        root.addView(controlRow, new LinearLayout.LayoutParams(dp(118), dp(23)));

        coordinateText = new TextView(context);
        coordinateText.setTextColor(colorTextDim);
        coordinateText.setTextSize(6);
        coordinateText.setGravity(Gravity.CENTER_VERTICAL);
        coordinateText.setSingleLine(true);
        coordinateText.setPadding(dp(2), dp(1), dp(2), 0);
        root.addView(coordinateText, new LinearLayout.LayoutParams(dp(118), dp(12)));

        updateSpeedButtonStates();
        updateToggleStates();
        updateCoordinateText();
        applyOverlayMode();
    }

    void show() {
        if (shown || !Settings.canDrawOverlays(context)) {
            return;
        }
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
        if (paused) {
            joystickView.reset();
        }
        updateToggleStates();
    }

    private void stopMovement() {
        joystickView.reset();
        listener.onStopRequested();
    }

    private void toggleOverlayMode() {
        compactMode = !compactMode;
        preferences.edit()
                .putBoolean(PREF_OVERLAY_COMPACT_MODE, compactMode)
                .apply();
        applyOverlayMode();
    }

    private void applyOverlayMode() {
        int expandedVisibility = compactMode ? View.GONE : View.VISIBLE;
        dragHandle.setVisibility(expandedVisibility);
        speedRow.setVisibility(expandedVisibility);
        controlRow.setVisibility(expandedVisibility);
        coordinateText.setVisibility(expandedVisibility);

        toggleModeButton.setText(compactMode ? "+" : "−");
        toggleModeButton.setContentDescription(compactMode ? "Switch to expanded overlay" : "Switch to compact overlay");
        styleToggleButton();

        LinearLayout.LayoutParams toggleParams = (LinearLayout.LayoutParams) toggleModeButton.getLayoutParams();
        toggleParams.width = compactMode ? dp(16) : dp(18);
        toggleParams.height = compactMode ? dp(14) : dp(16);
        toggleModeButton.setLayoutParams(toggleParams);

        LinearLayout.LayoutParams titleParams = (LinearLayout.LayoutParams) titleRow.getLayoutParams();
        titleParams.width = compactMode ? dp(88) : dp(118);
        titleParams.height = compactMode ? dp(14) : dp(19);
        titleRow.setLayoutParams(titleParams);
        titleRow.setGravity(compactMode ? Gravity.RIGHT : Gravity.CENTER_VERTICAL);

        root.setBackground(compactMode ? null : panelBackground());
        root.setPadding(
                compactMode ? 0 : dp(4),
                compactMode ? 0 : dp(3),
                compactMode ? 0 : dp(4),
                compactMode ? 0 : dp(4));
        root.requestLayout();
    }

    private void updateSpeedButtonStates() {
        styleIconButton(walkButton, SPEED_WALK.equals(selectedSpeedKind));
        styleIconButton(runButton, SPEED_RUN.equals(selectedSpeedKind));
        styleIconButton(bikeButton, SPEED_BIKE.equals(selectedSpeedKind));
        styleIconButton(customButton, SPEED_CUSTOM.equals(selectedSpeedKind));
        customButton.setContentDescription(customSpeedName() + " speed");
    }

    private String loadSavedSpeedKind() {
        String kind = preferences.getString(PREF_SELECTED_SPEED_KIND, null);
        if (SPEED_WALK.equals(kind) || SPEED_RUN.equals(kind) || SPEED_BIKE.equals(kind) || SPEED_CUSTOM.equals(kind)) {
            return kind;
        }
        double savedSpeed = Double.longBitsToDouble(
                preferences.getLong(PREF_SELECTED_SPEED, Double.doubleToLongBits(WALK_SPEED)));
        if (Math.abs(savedSpeed - RUN_SPEED) < 0.05) {
            return SPEED_RUN;
        }
        if (Math.abs(savedSpeed - BIKE_SPEED) < 0.05) {
            return SPEED_BIKE;
        }
        if (Math.abs(savedSpeed - customSpeed()) < 0.05) {
            return SPEED_CUSTOM;
        }
        return SPEED_WALK;
    }

    private double speedForKind(String kind) {
        if (SPEED_RUN.equals(kind)) {
            return RUN_SPEED;
        }
        if (SPEED_BIKE.equals(kind)) {
            return BIKE_SPEED;
        }
        if (SPEED_CUSTOM.equals(kind)) {
            return customSpeed();
        }
        return WALK_SPEED;
    }

    private double normalizeSpeed(double speed) {
        if (!Double.isFinite(speed)) {
            return WALK_SPEED;
        }
        if (Math.abs(speed - RUN_SPEED) < 0.05) {
            return RUN_SPEED;
        }
        if (Math.abs(speed - BIKE_SPEED) < 0.05) {
            return BIKE_SPEED;
        }
        double custom = customSpeed();
        if (Math.abs(speed - custom) < 0.05) {
            return custom;
        }
        if (Math.abs(speed - WALK_SPEED) < 0.05) {
            return WALK_SPEED;
        }
        return Math.max(0.1, Math.min(50.0, speed));
    }

    private double customSpeed() {
        double saved = Double.longBitsToDouble(
                preferences.getLong(PREF_CUSTOM_SPEED, Double.doubleToLongBits(5.0)));
        if (!Double.isFinite(saved)) {
            return 5.0;
        }
        return Math.max(0.1, Math.min(50.0, saved));
    }

    private String customSpeedName() {
        String name = preferences.getString(PREF_CUSTOM_SPEED_NAME, "Custom");
        if (name == null || name.trim().isEmpty()) {
            return "Custom";
        }
        return name.trim();
    }

    private void updateToggleStates() {
        pauseButton.setContentDescription(paused ? "Movement paused; tap to resume" : "Movement active; tap to pause");
        styleIconButton(pauseButton, !paused);

        holdButton.setContentDescription(holdEnabled ? "Disable hold" : "Enable hold");
        styleIconButton(holdButton, holdEnabled);
        styleIconButton(stopButton, false);
    }

    private void updateCoordinateText() {
        coordinateText.setText(String.format(
                Locale.US,
                "%.5f, %.5f · %.1f m/s",
                currentLatitude,
                currentLongitude,
                currentSpeed));
    }

    private boolean loadStyleSettings(boolean force) {
        int newOpacity = Math.max(30, Math.min(100, preferences.getInt(PREF_OVERLAY_OPACITY, 85)));
        boolean newHighContrast = preferences.getBoolean(PREF_OVERLAY_HIGH_CONTRAST, false);
        if (!force && newOpacity == overlayOpacityPercent && newHighContrast == highContrast) {
            return false;
        }
        overlayOpacityPercent = newOpacity;
        highContrast = newHighContrast;
        int panelAlpha = Math.round(255.0f * overlayOpacityPercent / 100.0f);
        colorPanel = argb(panelAlpha, 0x1A, 0x23, 0x29);
        colorBorder = highContrast ? 0xFFECEFF1 : 0x8090A4AE;
        colorButton = highContrast ? 0x66263238 : 0x33263238;
        colorButtonActive = highContrast ? 0xCC29A8FF : 0x80338FCE;
        int compactAlpha = Math.max(32, Math.round((highContrast ? 120.0f : 70.0f) * overlayOpacityPercent / 100.0f));
        colorButtonCompact = argb(compactAlpha, 0x26, 0x32, 0x38);
        colorText = 0xFFECEFF1;
        colorTextDim = highContrast ? 0xFFECEFF1 : 0xCCCFD8DC;
        colorAccent = highContrast ? 0xFFFFFFFF : 0xFF29A8FF;
        if (root != null) {
            dragHandle.setTextColor(colorText);
            coordinateText.setTextColor(colorTextDim);
            joystickView.setHighContrast(highContrast);
            joystickView.setOverlayOpacity(overlayOpacityPercent);
        }
        return true;
    }

    private int argb(int alpha, int red, int green, int blue) {
        return ((alpha & 0xFF) << 24)
                | ((red & 0xFF) << 16)
                | ((green & 0xFF) << 8)
                | (blue & 0xFF);
    }

    private Button controlButton(String text, int textSize) {
        Button button = new Button(context);
        button.setText(text);
        button.setTextColor(colorTextDim);
        button.setTextSize(textSize);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setMinimumHeight(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(2), 0, dp(2), 0);
        button.setBackground(buttonBackground(false));
        return button;
    }

    private IconButton iconButton(int iconType, String description) {
        IconButton button = new IconButton(context, iconType);
        button.setContentDescription(description);
        button.setText("");
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setMinimumHeight(0);
        button.setMinimumWidth(0);
        button.setPadding(0, 0, 0, 0);
        button.setBackground(buttonBackground(false));
        return button;
    }

    private void addSpeedButtonToRow(LinearLayout row, Button button) {
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(dp(26), dp(20));
        buttonParams.leftMargin = dp(1);
        buttonParams.rightMargin = dp(1);
        row.addView(button, buttonParams);
    }

    private void addSpeedGroupSpacer(LinearLayout row) {
        View spacer = new View(context);
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(dp(6), 0);
        row.addView(spacer, spacerParams);
    }

    private void addControlButtonLeft(LinearLayout row, Button button) {
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(dp(18), dp(20));
        buttonParams.leftMargin = dp(2);
        buttonParams.rightMargin = dp(1);
        row.addView(button, buttonParams);
    }

    private void addControlButtonCenter(LinearLayout row, Button button) {
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(dp(18), dp(20));
        buttonParams.leftMargin = dp(1);
        buttonParams.rightMargin = dp(1);
        row.addView(button, buttonParams);
    }

    private void addControlButtonRight(LinearLayout row, Button button) {
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(dp(18), dp(20));
        buttonParams.leftMargin = dp(1);
        buttonParams.rightMargin = dp(2);
        row.addView(button, buttonParams);
    }

    private void addControlSpacer(LinearLayout row) {
        View spacer = new View(context);
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(0, 0, 1f);
        row.addView(spacer, spacerParams);
    }

    private void styleToggleButton() {
        toggleModeButton.setTextColor(compactMode ? colorText : colorTextDim);
        toggleModeButton.setBackground(toggleButtonBackground());
    }

    private void styleButton(Button button, boolean active) {
        button.setTextColor(active ? colorText : colorTextDim);
        button.setBackground(buttonBackground(active));
    }

    private void styleIconButton(IconButton button, boolean active) {
        button.setIconActive(active, highContrast);
        button.setBackground(buttonBackground(active));
    }

    private GradientDrawable panelBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(colorPanel);
        drawable.setCornerRadius(dp(7));
        drawable.setStroke(dp(highContrast ? 2 : 1), colorBorder);
        return drawable;
    }

    private GradientDrawable buttonBackground(boolean active) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(active ? colorButtonActive : colorButton);
        drawable.setCornerRadius(dp(5));
        drawable.setStroke(dp(active || highContrast ? 2 : 1), active ? colorAccent : colorBorder);
        return drawable;
    }

    private GradientDrawable toggleButtonBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(compactMode ? colorButtonCompact : colorButton);
        drawable.setCornerRadius(dp(5));
        drawable.setStroke(dp(highContrast ? 2 : 1), compactMode ? colorAccent : colorBorder);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
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
                    if (Math.abs(dx) > dp(2) || Math.abs(dy) > dp(2)) {
                        moved = true;
                    }
                    params.x = startX + dx;
                    params.y = startY + dy;
                    if (shown) {
                        windowManager.updateViewLayout(root, params);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    preferences.edit()
                            .putInt(PREF_OVERLAY_X, params.x)
                            .putInt(PREF_OVERLAY_Y, params.y)
                            .apply();
                    if (!moved) {
                        view.performClick();
                    }
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
            if (resId == 0) {
                return;
            }
            if (iconDrawable == null || iconResId != resId) {
                iconDrawable = context.getDrawable(resId);
                if (iconDrawable != null) {
                    iconDrawable = iconDrawable.mutate();
                }
                iconResId = resId;
            }
            if (iconDrawable == null) {
                return;
            }
            int inset = iconInset();
            int side = Math.max(1, Math.min(getWidth(), getHeight()) - inset * 2);
            int left = (getWidth() - side) / 2;
            int top = (getHeight() - side) / 2;
            iconDrawable.setBounds(left, top, left + side, top + side);
            iconDrawable.setTint(active ? colorText : (contrast ? colorText : colorTextDim));
            iconDrawable.setAlpha(active || contrast ? 255 : 225);
            iconDrawable.draw(canvas);
        }

        private int iconResource() {
            switch (iconType) {
                case ICON_WALK:
                    return active ? R.drawable.overlay_ic_walk_filled : R.drawable.overlay_ic_walk_outline;
                case ICON_RUN:
                    return active ? R.drawable.overlay_ic_run_filled : R.drawable.overlay_ic_run_outline;
                case ICON_BIKE:
                    return active ? R.drawable.overlay_ic_bike_filled : R.drawable.overlay_ic_bike_outline;
                case ICON_GAUGE:
                    return active ? R.drawable.overlay_ic_gauge_filled : R.drawable.overlay_ic_gauge_outline;
                case ICON_LOCK:
                    return active ? R.drawable.overlay_ic_lock_filled : R.drawable.overlay_ic_lock_outline;
                case ICON_PLAY_PAUSE:
                    return active ? R.drawable.overlay_ic_play_filled : R.drawable.overlay_ic_pause_outline;
                case ICON_STOP:
                    return R.drawable.overlay_ic_x_outline;
                default:
                    return 0;
            }
        }

        private int iconInset() {
            switch (iconType) {
                case ICON_BIKE:
                case ICON_GAUGE:
                    return dp(1);
                case ICON_STOP:
                    return dp(2);
                default:
                    return dp(1);
            }
        }
    }
}
