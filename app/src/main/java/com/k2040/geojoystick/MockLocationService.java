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
import android.content.pm.ServiceInfo;
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
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.widget.Toast;

public final class MockLocationService extends Service {
    static final String ACTION_START = "com.k2040.geojoystick.action.START";
    static final String ACTION_SET_POSITION = "com.k2040.geojoystick.action.SET_POSITION";
    static final String ACTION_SHOW_OVERLAY = "com.k2040.geojoystick.action.SHOW_OVERLAY";
    static final String ACTION_HIDE_OVERLAY = "com.k2040.geojoystick.action.HIDE_OVERLAY";
    static final String ACTION_STOP = "com.k2040.geojoystick.action.STOP";
    static final String ACTION_STATE_CHANGED = "com.k2040.geojoystick.action.STATE_CHANGED";
    static final String PERMISSION_INTERNAL_STATE =
            "com.k2040.geojoystick.permission.INTERNAL_STATE";
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
    private static volatile boolean simulationActive;
    private static volatile boolean simulationStarting;
    private boolean hasPosition;
    private boolean hasPublishedPosition;
    private double lastPublishedLatitude;
    private double lastPublishedLongitude;
    private double lastPublishedAltitude;
    private boolean foregroundStarted;

    static boolean isSimulationActive() {
        return simulationActive;
    }

    static boolean isSimulationStarting() {
        return simulationStarting;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        preferences = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        setSimulationState(false, false);
        speedMetersPerSecond = loadSavedSpeed();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            setSimulationState(false, false);
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopSimulation();
            return START_NOT_STICKY;
        }

        if (ACTION_START.equals(action)) {
            if (!setPositionFromIntent(intent)) {
                reportRuntimeError(R.string.service_invalid_start);
                setSimulationState(false, false);
                stopSelf(startId);
                return START_NOT_STICKY;
            }
            setSimulationState(true, false);
            startOrRefreshSimulation();
            return START_NOT_STICKY;
        }

        if (ACTION_SET_POSITION.equals(action)) {
            if (!simulationActive) {
                reportRuntimeError(R.string.service_not_active);
                stopSelf(startId);
                return START_NOT_STICKY;
            }
            if (!setPositionFromIntent(intent)) {
                reportRuntimeError(R.string.service_coordinates_rejected);
                return START_NOT_STICKY;
            }
            prepareProviders();
            if (!publishReadyProviders()) {
                failSimulation(R.string.service_no_provider);
                return START_NOT_STICKY;
            }
            persistPosition();
            updateNotification();
            return START_NOT_STICKY;
        }

        if (ACTION_SHOW_OVERLAY.equals(action)) {
            if (simulationActive) {
                if (overlay != null) {
                    overlay.show();
                }
            } else {
                stopSelf(startId);
            }
            return START_NOT_STICKY;
        }

        if (ACTION_HIDE_OVERLAY.equals(action)) {
            if (simulationActive) {
                if (overlay != null) {
                    overlay.hide();
                }
                eastFactor = 0.0;
                northFactor = 0.0;
            } else {
                stopSelf(startId);
            }
            return START_NOT_STICKY;
        }

        if (!simulationActive && !simulationStarting) {
            stopSelf(startId);
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        setSimulationState(false, false);
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
        if (hasPublishedPosition) {
            persistPosition();
        }
        removeTestProvider(LocationManager.NETWORK_PROVIDER);
        removeTestProvider(LocationManager.GPS_PROVIDER);
        if (foregroundStarted) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final Runnable locationLoop = new Runnable() {
        @Override
        public void run() {
            if (!simulationActive || workerHandler == null) {
                return;
            }

            long nowNanos = SystemClock.elapsedRealtimeNanos();
            double elapsedSeconds = Math.min(
                    1.0,
                    Math.max(0.0, (nowNanos - lastTickNanos) / 1_000_000_000.0));
            lastTickNanos = nowNanos;

            updatePosition(elapsedSeconds);
            long nowMillis = SystemClock.elapsedRealtime();
            if ((!gpsProviderReady || !networkProviderReady)
                    && nowMillis - lastProviderRetryMillis >= 2_000L) {
                prepareProviders();
                lastProviderRetryMillis = nowMillis;
            }

            if (!publishReadyProviders()) {
                failSimulation(R.string.service_publish_failed);
                return;
            }
            double lat;
            double lng;
            synchronized (positionLock) {
                lat = lastPublishedLatitude;
                lng = lastPublishedLongitude;
            }
            if (overlay != null) {
                overlay.updatePosition(lat, lng, speedMetersPerSecond);
            }

            if (nowMillis - lastPersistMillis >= 1_000L) {
                persistPosition();
                updateNotification();
                lastPersistMillis = nowMillis;
            }
            if (simulationActive && workerHandler != null) {
                workerHandler.postDelayed(this, UPDATE_INTERVAL_MS);
            }
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

    private boolean setPositionFromIntent(Intent intent) {
        if (intent == null
                || !intent.hasExtra(EXTRA_LATITUDE)
                || !intent.hasExtra(EXTRA_LONGITUDE)
                || !intent.hasExtra(EXTRA_ALTITUDE)) {
            return false;
        }

        double requestedLatitude = intent.getDoubleExtra(EXTRA_LATITUDE, Double.NaN);
        double requestedLongitude = intent.getDoubleExtra(EXTRA_LONGITUDE, Double.NaN);
        double requestedAltitude = intent.getDoubleExtra(EXTRA_ALTITUDE, Double.NaN);
        if (!Double.isFinite(requestedLatitude)
                || !Double.isFinite(requestedLongitude)
                || !Double.isFinite(requestedAltitude)
                || requestedLatitude < -90.0
                || requestedLatitude > 90.0
                || requestedLongitude < -180.0
                || requestedLongitude > 180.0) {
            return false;
        }

        synchronized (positionLock) {
            latitude = requestedLatitude;
            longitude = requestedLongitude;
            altitude = requestedAltitude;
        }
        hasPosition = true;
        return true;
    }

    private boolean startOrRefreshSimulation() {
        setSimulationState(true, false);
        try {
            if (!foregroundStarted) {
                startForegroundNotification(false);
            }
            ensureRuntimeComponents();
            prepareProviders();
            if (!publishReadyProviders()) {
                return failSimulation(R.string.service_cannot_publish);
            }

            persistPosition();
            setSimulationState(false, true);
            if (overlay != null) {
                synchronized (positionLock) {
                    overlay.updatePosition(
                            lastPublishedLatitude,
                            lastPublishedLongitude,
                            speedMetersPerSecond);
                }
            }
            if (Settings.canDrawOverlays(this) && overlay != null) {
                overlay.show();
            }
            lastTickNanos = SystemClock.elapsedRealtimeNanos();
            updateNotification();
            if (workerHandler != null) {
                workerHandler.removeCallbacks(locationLoop);
                workerHandler.post(locationLoop);
            }
            return true;
        } catch (RuntimeException exception) {
            return failSimulation(R.string.service_cannot_start);
        }
    }

    private void ensureRuntimeComponents() {
        if (workerThread == null) {
            workerThread = new HandlerThread("GeoJoystickLocationWorker");
            workerThread.start();
            workerHandler = new Handler(workerThread.getLooper());
        }

        if (overlay == null) {
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
                    stopSimulation();
                }
            });
        }
    }

    private void startForegroundNotification(boolean active) {
        Notification notification = buildNotification(active);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
        foregroundStarted = true;
    }

    private boolean publishReadyProviders() {
        PositionSnapshot snapshot = snapshotCurrentPosition();
        if (snapshot == null) {
            return false;
        }

        boolean attempted = false;
        boolean published = false;

        if (gpsProviderReady) {
            attempted = true;
            gpsProviderReady = publishLocation(
                    LocationManager.GPS_PROVIDER,
                    3.0f,
                    snapshot);
            published |= gpsProviderReady;
        }
        if (networkProviderReady) {
            attempted = true;
            networkProviderReady = publishLocation(
                    LocationManager.NETWORK_PROVIDER,
                    12.0f,
                    snapshot);
            published |= networkProviderReady;
        }
        if (attempted && published) {
            recordPublishedPosition(snapshot);
            return true;
        }
        return false;
    }

    private PositionSnapshot snapshotCurrentPosition() {
        synchronized (positionLock) {
            if (!hasPosition
                    || !Double.isFinite(latitude)
                    || !Double.isFinite(longitude)
                    || !Double.isFinite(altitude)) {
                return null;
            }
            return new PositionSnapshot(latitude, longitude, altitude, bearing);
        }
    }

    private void recordPublishedPosition(PositionSnapshot snapshot) {
        synchronized (positionLock) {
            lastPublishedLatitude = snapshot.latitude;
            lastPublishedLongitude = snapshot.longitude;
            lastPublishedAltitude = snapshot.altitude;
            hasPublishedPosition = true;
        }
    }

    private boolean failSimulation(int resourceId) {
        setSimulationState(false, false);
        eastFactor = 0.0;
        northFactor = 0.0;
        reportRuntimeError(resourceId);
        stopSelf();
        return false;
    }

    private void stopSimulation() {
        setSimulationState(false, false);
        eastFactor = 0.0;
        northFactor = 0.0;
        stopSelf();
    }

    private void reportRuntimeError(int resourceId) {
        String message = t(resourceId);
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(
                        getApplicationContext(),
                        message,
                        Toast.LENGTH_LONG).show());
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

    private boolean publishLocation(
            String provider,
            float accuracyMeters,
            PositionSnapshot snapshot) {
        try {
            Location location = new Location(provider);
            location.setLatitude(snapshot.latitude);
            location.setLongitude(snapshot.longitude);
            location.setAltitude(snapshot.altitude);
            location.setBearing(snapshot.bearing);
            location.setAccuracy(accuracyMeters);
            location.setSpeed((float) (Math.hypot(eastFactor, northFactor) * speedMetersPerSecond));
            location.setTime(System.currentTimeMillis());
            location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            location.setVerticalAccuracyMeters(5.0f);
            location.setSpeedAccuracyMetersPerSecond(0.5f);
            location.setBearingAccuracyDegrees(3.0f);
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
        if (!hasPosition || !hasPublishedPosition) {
            return;
        }

        double lat;
        double lng;
        double alt;
        synchronized (positionLock) {
            lat = lastPublishedLatitude;
            lng = lastPublishedLongitude;
            alt = lastPublishedAltitude;
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
                t(R.string.ui_171),
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(t(R.string.ui_172));
        manager.createNotificationChannel(channel);
    }

    private Notification buildNotification(boolean active) {
        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                this,
                1,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PendingIntent showIntent = serviceAction(ACTION_SHOW_OVERLAY, 2);
        PendingIntent hideIntent = serviceAction(ACTION_HIDE_OVERLAY, 3);
        PendingIntent stopIntent = serviceAction(ACTION_STOP, 4);

        Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_location)
                .setContentTitle("GeoJoystick")
                .setContentText(active
                        ? t(R.string.ui_173)
                        : t(R.string.ui_174))
                .setContentIntent(openPendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true);

        if (active) {
            builder.addAction(new Notification.Action.Builder(
                    notificationIcon(),
                    t(R.string.ui_175),
                    showIntent).build());
            builder.addAction(new Notification.Action.Builder(
                    notificationIcon(),
                    t(R.string.ui_176),
                    hideIntent).build());
        }
        builder.addAction(new Notification.Action.Builder(
                notificationIcon(),
                t(R.string.ui_177),
                stopIntent).build());
        return builder.build();
    }


    private void setSimulationState(boolean starting, boolean active) {
        simulationStarting = starting && !active;
        simulationActive = active;
        Intent update = new Intent(ACTION_STATE_CHANGED)
                .setPackage(getPackageName());
        sendBroadcast(update, PERMISSION_INTERNAL_STATE);
    }

    private String t(int resourceId) {
        return new GeoSettings(this).text(resourceId);
    }

    private static final class PositionSnapshot {
        final double latitude;
        final double longitude;
        final double altitude;
        final float bearing;

        PositionSnapshot(double latitude, double longitude, double altitude, float bearing) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.altitude = altitude;
            this.bearing = bearing;
        }
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
        if (!foregroundStarted) {
            return;
        }
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification(simulationActive));
        }
    }
}
