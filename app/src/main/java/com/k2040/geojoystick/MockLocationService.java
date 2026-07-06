package com.k2040.geojoystick;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.provider.ProviderProperties;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;

import java.util.Locale;

public final class MockLocationService extends Service {
    static final String ACTION_START = "com.k2040.geojoystick.action.START";
    static final String ACTION_SET_POSITION = "com.k2040.geojoystick.action.SET_POSITION";
    static final String ACTION_SHOW_OVERLAY = "com.k2040.geojoystick.action.SHOW_OVERLAY";
    static final String ACTION_HIDE_OVERLAY = "com.k2040.geojoystick.action.HIDE_OVERLAY";
    static final String ACTION_STOP = "com.k2040.geojoystick.action.STOP";
    static final String EXTRA_LATITUDE = "latitude";
    static final String EXTRA_LONGITUDE = "longitude";
    static final String EXTRA_ALTITUDE = "altitude";

    private static final String PREFS = "geojoystick";
    private static final String PREF_LATITUDE = "last_latitude";
    private static final String PREF_LONGITUDE = "last_longitude";
    private static final String PREF_ALTITUDE = "last_altitude";
    private static final String PREF_MANUAL_LATITUDE = "manual_latitude";
    private static final String PREF_MANUAL_LONGITUDE = "manual_longitude";
    private static final String PREF_MANUAL_ALTITUDE = "manual_altitude";
    private static final String PREF_RESTORE_LAST_POSITION = "restore_last_position";
    private static final String PREF_SELECTED_SPEED = "overlay_selected_speed";
    private static final String PREF_SELECTED_SPEED_KIND = "overlay_selected_speed_kind";
    private static final String PREF_CUSTOM_SPEED = "overlay_custom_speed";
    private static final String CHANNEL_ID = "geojoystick_mock_location";
    private static final int NOTIFICATION_ID = 2040;
    private static final long UPDATE_INTERVAL_MS = 200L;
    private static final double WALK_SPEED = 1.2;
    private static final double RUN_SPEED = 3.6;
    private static final double BIKE_SPEED = 10.0;
    private static final String SPEED_RUN = "run";
    private static final String SPEED_BIKE = "bike";
    private static final String SPEED_CUSTOM = "custom";

    private final Object positionLock = new Object();
    private LocationManager locationManager;
    private SharedPreferences preferences;
    private HandlerThread workerThread;
    private Handler workerHandler;
    private JoystickOverlay overlay;
    private volatile double eastFactor;
    private volatile double northFactor;
    private volatile double speedMetersPerSecond = 1.2;
    private double latitude;
    private double longitude;
    private double altitude;
    private float bearing;
    private long lastTickNanos;
    private long lastPersistMillis;
    private long lastProviderRetryMillis;
    private volatile boolean gpsProviderReady;
    private volatile boolean networkProviderReady;

    @Override
    public void onCreate() {
        super.onCreate();
        preferences = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        latitude = Double.longBitsToDouble(preferences.getLong(PREF_LATITUDE, Double.doubleToLongBits(52.520008)));
        longitude = Double.longBitsToDouble(preferences.getLong(PREF_LONGITUDE, Double.doubleToLongBits(13.404954)));
        altitude = Double.longBitsToDouble(preferences.getLong(PREF_ALTITUDE, Double.doubleToLongBits(55.0)));
        speedMetersPerSecond = loadSavedSpeed();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());

        workerThread = new HandlerThread("GeoJoystickLocationWorker");
        workerThread.start();
        workerHandler = new Handler(workerThread.getLooper());

        overlay = new JoystickOverlay(this, new JoystickOverlay.Listener() {
            @Override
            public void onVectorChanged(double east, double north) {
                eastFactor = east;
                northFactor = north;
            }

            @Override
            public void onSpeedChanged(double metersPerSecond) {
                speedMetersPerSecond = normalizeSpeed(metersPerSecond);
            }

            @Override
            public void onStopRequested() {
                stopSelf();
            }
        });

        prepareProviders();
        if (Settings.canDrawOverlays(this)) {
            overlay.show();
        }
        lastTickNanos = SystemClock.elapsedRealtimeNanos();
        workerHandler.post(locationLoop);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_STICKY;
        }

        String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (ACTION_SHOW_OVERLAY.equals(action)) {
            overlay.show();
            return START_STICKY;
        }
        if (ACTION_HIDE_OVERLAY.equals(action)) {
            overlay.hide();
            eastFactor = 0.0;
            northFactor = 0.0;
            return START_STICKY;
        }
        if (ACTION_START.equals(action) || ACTION_SET_POSITION.equals(action)) {
            setPositionFromIntent(intent);
            prepareProviders();
            overlay.show();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        eastFactor = 0.0;
        northFactor = 0.0;
        if (workerHandler != null) {
            workerHandler.removeCallbacksAndMessages(null);
        }
        if (workerThread != null) {
            workerThread.quitSafely();
        }
        if (overlay != null) {
            overlay.destroy();
        }
        persistPosition();
        removeTestProvider(LocationManager.NETWORK_PROVIDER);
        removeTestProvider(LocationManager.GPS_PROVIDER);
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final Runnable locationLoop = new Runnable() {
        @Override
        public void run() {
            long nowNanos = SystemClock.elapsedRealtimeNanos();
            double elapsedSeconds = Math.min(1.0, Math.max(0.0, (nowNanos - lastTickNanos) / 1_000_000_000.0));
            lastTickNanos = nowNanos;

            updatePosition(elapsedSeconds);
            long nowMillis = SystemClock.elapsedRealtime();
            if ((!gpsProviderReady || !networkProviderReady)
                    && nowMillis - lastProviderRetryMillis >= 2_000L) {
                prepareProviders();
                lastProviderRetryMillis = nowMillis;
            }
            if (gpsProviderReady) {
                gpsProviderReady = publishLocation(LocationManager.GPS_PROVIDER, 3.0f);
            }
            if (networkProviderReady) {
                networkProviderReady = publishLocation(LocationManager.NETWORK_PROVIDER, 12.0f);
            }

            double lat;
            double lng;
            synchronized (positionLock) {
                lat = latitude;
                lng = longitude;
            }
            overlay.updatePosition(lat, lng, speedMetersPerSecond);

            if (nowMillis - lastPersistMillis >= 1_000L) {
                persistPosition();
                updateNotification();
                lastPersistMillis = nowMillis;
            }
            workerHandler.postDelayed(this, UPDATE_INTERVAL_MS);
        }
    };

    private void updatePosition(double elapsedSeconds) {
        double east = eastFactor;
        double north = northFactor;
        double magnitude = Math.hypot(east, north);
        if (magnitude <= 0.0 || elapsedSeconds <= 0.0) {
            return;
        }

        double eastMeters = east * speedMetersPerSecond * elapsedSeconds;
        double northMeters = north * speedMetersPerSecond * elapsedSeconds;
        synchronized (positionLock) {
            latitude += northMeters / 111_320.0;
            double cosine = Math.cos(Math.toRadians(latitude));
            if (Math.abs(cosine) > 0.000001) {
                longitude += eastMeters / (111_320.0 * cosine);
            }
            longitude = normalizeLongitude(longitude);
            latitude = Math.max(-90.0, Math.min(90.0, latitude));
            bearing = (float) ((Math.toDegrees(Math.atan2(east, north)) + 360.0) % 360.0);
        }
    }

    private void setPositionFromIntent(Intent intent) {
        double requestedLatitude = intent.getDoubleExtra(EXTRA_LATITUDE, latitude);
        double requestedLongitude = intent.getDoubleExtra(EXTRA_LONGITUDE, longitude);
        double requestedAltitude = intent.getDoubleExtra(EXTRA_ALTITUDE, altitude);
        if (!Double.isFinite(requestedLatitude)
                || !Double.isFinite(requestedLongitude)
                || !Double.isFinite(requestedAltitude)) {
            return;
        }
        synchronized (positionLock) {
            latitude = Math.max(-90.0, Math.min(90.0, requestedLatitude));
            longitude = normalizeLongitude(requestedLongitude);
            altitude = requestedAltitude;
        }
        persistPosition();
    }

    private void prepareProviders() {
        if (locationManager == null) {
            gpsProviderReady = false;
            networkProviderReady = false;
            return;
        }
        if (!gpsProviderReady) {
            gpsProviderReady = addTestProvider(LocationManager.GPS_PROVIDER, true);
        }
        if (!networkProviderReady) {
            networkProviderReady = addTestProvider(LocationManager.NETWORK_PROVIDER, false);
        }
    }

    @SuppressLint("WrongConstant")
    private boolean addTestProvider(String provider, boolean gps) {
        try {
            removeTestProvider(provider);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                locationManager.addTestProvider(
                        provider,
                        !gps,
                        gps,
                        !gps,
                        !gps,
                        true,
                        true,
                        true,
                        gps ? ProviderProperties.POWER_USAGE_HIGH : ProviderProperties.POWER_USAGE_LOW,
                        gps ? ProviderProperties.ACCURACY_FINE : ProviderProperties.ACCURACY_COARSE);
            } else {
                locationManager.addTestProvider(
                        provider,
                        !gps,
                        gps,
                        !gps,
                        !gps,
                        true,
                        true,
                        true,
                        gps ? Criteria.POWER_HIGH : Criteria.POWER_LOW,
                        gps ? Criteria.ACCURACY_FINE : Criteria.ACCURACY_COARSE);
            }
            locationManager.setTestProviderEnabled(provider, true);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private void removeTestProvider(String provider) {
        if (locationManager == null) {
            return;
        }
        try {
            locationManager.setTestProviderEnabled(provider, false);
        } catch (RuntimeException ignored) {
            // The provider may not currently be a test provider.
        }
        try {
            locationManager.removeTestProvider(provider);
        } catch (RuntimeException ignored) {
            // The provider may not currently be a test provider.
        }
    }

    private boolean publishLocation(String provider, float accuracyMeters) {
        try {
            Location location = new Location(provider);
            synchronized (positionLock) {
                location.setLatitude(latitude);
                location.setLongitude(longitude);
                location.setAltitude(altitude);
                location.setBearing(bearing);
            }
            location.setAccuracy(accuracyMeters);
            location.setSpeed((float) (Math.hypot(eastFactor, northFactor) * speedMetersPerSecond));
            location.setTime(System.currentTimeMillis());
            location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                location.setVerticalAccuracyMeters(5.0f);
                location.setSpeedAccuracyMetersPerSecond(0.5f);
                location.setBearingAccuracyDegrees(3.0f);
            }
            Bundle extras = new Bundle();
            extras.putInt("satellites", 12);
            location.setExtras(extras);
            locationManager.setTestProviderLocation(provider, location);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private double loadSavedSpeed() {
        String kind = preferences.getString(PREF_SELECTED_SPEED_KIND, "walk");
        if (SPEED_RUN.equals(kind)) {
            return RUN_SPEED;
        }
        if (SPEED_BIKE.equals(kind)) {
            return BIKE_SPEED;
        }
        if (SPEED_CUSTOM.equals(kind)) {
            return customSpeed();
        }
        double savedSpeed = Double.longBitsToDouble(
                preferences.getLong(PREF_SELECTED_SPEED, Double.doubleToLongBits(WALK_SPEED)));
        return normalizeSpeed(savedSpeed);
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

    private void persistPosition() {
        double lat;
        double lng;
        double alt;
        synchronized (positionLock) {
            lat = latitude;
            lng = longitude;
            alt = altitude;
        }
        SharedPreferences.Editor editor = preferences.edit()
                .putLong(PREF_LATITUDE, Double.doubleToRawLongBits(lat))
                .putLong(PREF_LONGITUDE, Double.doubleToRawLongBits(lng))
                .putLong(PREF_ALTITUDE, Double.doubleToRawLongBits(alt));
        if (preferences.getBoolean(PREF_RESTORE_LAST_POSITION, true)) {
            editor.putLong(PREF_MANUAL_LATITUDE, Double.doubleToRawLongBits(lat))
                    .putLong(PREF_MANUAL_LONGITUDE, Double.doubleToRawLongBits(lng))
                    .putLong(PREF_MANUAL_ALTITUDE, Double.doubleToRawLongBits(alt));
        }
        editor.apply();
    }

    private double normalizeLongitude(double value) {
        double normalized = value;
        while (normalized > 180.0) normalized -= 360.0;
        while (normalized < -180.0) normalized += 360.0;
        return normalized;
    }

    private void createNotificationChannel() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Keeps the mock-location joystick active.");
        manager.createNotificationChannel(channel);
    }

    private Notification buildNotification() {
        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                this,
                1,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PendingIntent showIntent = serviceAction(ACTION_SHOW_OVERLAY, 2);
        PendingIntent hideIntent = serviceAction(ACTION_HIDE_OVERLAY, 3);
        PendingIntent stopIntent = serviceAction(ACTION_STOP, 4);

        double lat;
        double lng;
        synchronized (positionLock) {
            lat = latitude;
            lng = longitude;
        }
        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_location)
                .setContentTitle("GeoJoystick is active")
                .setContentText(String.format(Locale.US, "%.5f, %.5f", lat, lng))
                .setContentIntent(openPendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .addAction(new Notification.Action.Builder(notificationIcon(), "Show", showIntent).build())
                .addAction(new Notification.Action.Builder(notificationIcon(), "Hide", hideIntent).build())
                .addAction(new Notification.Action.Builder(notificationIcon(), "Stop", stopIntent).build())
                .build();
    }

    private Icon notificationIcon() {
        return Icon.createWithResource(this, R.drawable.ic_stat_location);
    }

    private PendingIntent serviceAction(String action, int requestCode) {
        Intent intent = new Intent(this, MockLocationService.class).setAction(action);
        return PendingIntent.getService(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void updateNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification());
        }
    }
}
