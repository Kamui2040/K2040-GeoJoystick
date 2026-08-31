package com.k2040.geojoystick;

import android.annotation.TargetApi;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

final class PlaceSearchGeocoder {
    private static final int MAX_RESULTS = 2;

    interface Callback {
        void onResults(List<Address> addresses);

        void onFailure();
    }

    private PlaceSearchGeocoder() {
    }

    static boolean isAvailable() {
        return Geocoder.isPresent();
    }

    static void search(
            Context context,
            Locale locale,
            String query,
            Callback callback) {
        if (!isAvailable()) {
            callback.onFailure();
            return;
        }

        Geocoder geocoder = new Geocoder(context, locale);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Api33.search(geocoder, query, callback);
            return;
        }

        Thread worker = new Thread(() -> {
            try {
                List<Address> results = geocoder.getFromLocationName(query, MAX_RESULTS);
                callback.onResults(results == null
                        ? Collections.emptyList()
                        : results);
            } catch (IOException | RuntimeException exception) {
                callback.onFailure();
            }
        }, "GeoJoystick-place-search");
        worker.setDaemon(true);
        worker.start();
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    private static final class Api33 {
        private Api33() {
        }

        static void search(
                Geocoder geocoder,
                String query,
                Callback callback) {
            try {
                geocoder.getFromLocationName(
                        query,
                        MAX_RESULTS,
                        new Geocoder.GeocodeListener() {
                            @Override
                            public void onGeocode(List<Address> addresses) {
                                callback.onResults(addresses == null
                                        ? Collections.emptyList()
                                        : addresses);
                            }

                            @Override
                            public void onError(String errorMessage) {
                                callback.onFailure();
                            }
                        });
            } catch (RuntimeException exception) {
                callback.onFailure();
            }
        }
    }
}
