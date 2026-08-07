package com.k2040.geojoystick;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

final class JoystickView extends View {
    interface Listener {
        void onVectorChanged(double east, double north);
    }

    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Listener listener;
    private float centerX;
    private float centerY;
    private float knobX;
    private float knobY;
    private float travelRadius;
    private boolean holdEnabled;
    private boolean highContrast;
    private int overlayOpacityPercent = 85;

    JoystickView(Context context) {
        super(context);
        ringPaint.setStyle(Paint.Style.STROKE);
        innerPaint.setStyle(Paint.Style.FILL);
        guidePaint.setStyle(Paint.Style.STROKE);
        guidePaint.setStrokeCap(Paint.Cap.ROUND);
        knobPaint.setStyle(Paint.Style.FILL);
        updatePaintColors();
        setContentDescription("Movement joystick");
        setMinimumWidth(dp(96));
        setMinimumHeight(dp(96));
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    void setHoldEnabled(boolean enabled) {
        holdEnabled = enabled;
        if (!enabled) reset();
    }

    void setHighContrast(boolean enabled) {
        if (highContrast == enabled) return;
        highContrast = enabled;
        updatePaintColors();
        invalidate();
    }

    void setOverlayOpacity(int opacityPercent) {
        int clamped = Math.max(30, Math.min(100, opacityPercent));
        if (overlayOpacityPercent == clamped) return;
        overlayOpacityPercent = clamped;
        updatePaintColors();
        invalidate();
    }

    private void updatePaintColors() {
        int ringAlpha = Math.round((highContrast ? 255f : 220f) * overlayOpacityPercent / 100f);
        int fillAlpha = Math.round((highContrast ? 92f : 58f) * overlayOpacityPercent / 100f);
        int guideAlpha = Math.round((highContrast ? 255f : 235f) * overlayOpacityPercent / 100f);
        int knobAlpha = Math.round((highContrast ? 255f : 245f) * overlayOpacityPercent / 100f);
        ringPaint.setStrokeWidth(dp(highContrast ? 2 : 1));
        ringPaint.setColor(argb(ringAlpha, highContrast ? 0xF3 : 0x69, highContrast ? 0xF7 : 0x86, highContrast ? 0xFB : 0xA2));
        innerPaint.setColor(argb(fillAlpha, 0x2F, 0x8C, 0xFF));
        guidePaint.setStrokeWidth(dp(highContrast ? 3 : 2));
        guidePaint.setColor(argb(guideAlpha, 0x58, 0xA6, 0xFF));
        knobPaint.setColor(argb(knobAlpha, 0x2F, 0x8C, 0xFF));
    }

    private int argb(int alpha, int red, int green, int blue) {
        return ((alpha & 0xFF) << 24)
                | ((red & 0xFF) << 16)
                | ((green & 0xFF) << 8)
                | (blue & 0xFF);
    }

    void reset() {
        knobX = centerX;
        knobY = centerY;
        notifyVector(0.0, 0.0);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int preferred = dp(132);
        int width = resolveSize(preferred, widthMeasureSpec);
        int height = resolveSize(preferred, heightMeasureSpec);
        int size = Math.min(width, height);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        centerX = width / 2.0f;
        centerY = height / 2.0f;
        travelRadius = Math.min(width, height) * 0.31f;
        if (oldWidth == 0 || oldHeight == 0) {
            knobX = centerX;
            knobY = centerY;
        } else if (!holdEnabled) {
            reset();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float size = Math.min(getWidth(), getHeight());
        float outerRadius = size * 0.46f;
        float innerRadius = size * 0.34f;
        float knobRadius = size * 0.13f;
        canvas.drawCircle(centerX, centerY, outerRadius, ringPaint);
        canvas.drawCircle(centerX, centerY, innerRadius, innerPaint);
        drawDirectionGuides(canvas, outerRadius);
        canvas.drawCircle(knobX, knobY, knobRadius, knobPaint);
    }

    private void drawDirectionGuides(Canvas canvas, float radius) {
        float length = Math.max(dp(5), radius * 0.11f);
        float offset = radius * 0.78f;
        drawChevron(canvas, centerX, centerY - offset, 0f, length);
        drawChevron(canvas, centerX + offset, centerY, 90f, length);
        drawChevron(canvas, centerX, centerY + offset, 180f, length);
        drawChevron(canvas, centerX - offset, centerY, 270f, length);
    }

    private void drawChevron(Canvas canvas, float x, float y, float degrees, float length) {
        double angle = Math.toRadians(degrees);
        double left = angle + Math.toRadians(135);
        double right = angle - Math.toRadians(135);
        canvas.drawLine(x, y,
                x + (float) Math.cos(left) * length,
                y + (float) Math.sin(left) * length,
                guidePaint);
        canvas.drawLine(x, y,
                x + (float) Math.cos(right) * length,
                y + (float) Math.sin(right) * length,
                guidePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                updateFromTouch(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!holdEnabled) reset();
                performClick();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void updateFromTouch(float touchX, float touchY) {
        float dx = touchX - centerX;
        float dy = touchY - centerY;
        double distance = Math.hypot(dx, dy);
        if (distance > travelRadius && distance > 0.0) {
            double scale = travelRadius / distance;
            dx *= scale;
            dy *= scale;
        }
        knobX = centerX + dx;
        knobY = centerY + dy;
        double east = dx / travelRadius;
        double north = -dy / travelRadius;
        if (Math.hypot(east, north) < 0.08) {
            east = 0.0;
            north = 0.0;
        }
        notifyVector(east, north);
        invalidate();
    }

    private void notifyVector(double east, double north) {
        if (listener != null) listener.onVectorChanged(east, north);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
