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
        ringPaint.setStrokeWidth(dp(1));
        updatePaintColors();
        setContentDescription("Movement joystick");
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    void setHoldEnabled(boolean enabled) {
        holdEnabled = enabled;
        if (!enabled) {
            reset();
        }
    }

    void setHighContrast(boolean enabled) {
        if (highContrast == enabled) {
            return;
        }
        highContrast = enabled;
        ringPaint.setStrokeWidth(dp(highContrast ? 2 : 1));
        updatePaintColors();
        invalidate();
    }

    void setOverlayOpacity(int opacityPercent) {
        int clamped = Math.max(30, Math.min(100, opacityPercent));
        if (overlayOpacityPercent == clamped) {
            return;
        }
        overlayOpacityPercent = clamped;
        updatePaintColors();
        invalidate();
    }

    private void updatePaintColors() {
        int ringAlpha = Math.round((highContrast ? 255.0f : 204.0f) * overlayOpacityPercent / 100.0f);
        int knobAlpha = Math.round((highContrast ? 255.0f : 230.0f) * overlayOpacityPercent / 100.0f);
        ringPaint.setColor(argb(ringAlpha, highContrast ? 0xEC : 0x90, highContrast ? 0xEF : 0xA4, highContrast ? 0xF1 : 0xAE));
        knobPaint.setColor(argb(knobAlpha, 0xEC, 0xEF, 0xF1));
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
        int preferred = dp(88);
        int width = resolveSize(preferred, widthMeasureSpec);
        int height = resolveSize(preferred, heightMeasureSpec);
        int size = Math.min(width, height);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        centerX = width / 2.0f;
        centerY = height / 2.0f;
        travelRadius = Math.min(width, height) * 0.34f;
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
        float outerRadius = Math.min(getWidth(), getHeight()) * 0.46f;
        float knobRadius = Math.min(getWidth(), getHeight()) * 0.11f;
        canvas.drawCircle(centerX, centerY, outerRadius, ringPaint);
        canvas.drawCircle(knobX, knobY, knobRadius, knobPaint);
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
                if (!holdEnabled) {
                    reset();
                }
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
        if (listener != null) {
            listener.onVectorChanged(east, north);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
