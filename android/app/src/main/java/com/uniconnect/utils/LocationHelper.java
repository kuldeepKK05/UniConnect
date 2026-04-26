package com.uniconnect.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Looper;

import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationHelper {

    public interface LocationCallback2 {
        void onLocationReceived(double lat, double lng, String city, String country);
        void onLocationFailed(String reason);
    }

    private final Context context;
    private final FusedLocationProviderClient fusedClient;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private LocationCallback locationCallback;

    public LocationHelper(Context context) {
        this.context = context.getApplicationContext();
        this.fusedClient = LocationServices.getFusedLocationProviderClient(context);
    }

    public boolean hasPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public void getCurrentLocation(LocationCallback2 callback) {
        if (!hasPermission()) {
            callback.onLocationFailed("Permission denied");
            return;
        }

        // Try last known location first (fast)
        fusedClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null && isLocationFresh(location)) {
                reverseGeocode(location.getLatitude(), location.getLongitude(), callback);
            } else {
                // Request fresh location
                requestFreshLocation(callback);
            }
        }).addOnFailureListener(e -> requestFreshLocation(callback));
    }

    @SuppressWarnings("MissingPermission")
    private void requestFreshLocation(LocationCallback2 callback) {
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .setMaxUpdates(1)
                .setWaitForAccurateLocation(false)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                Location location = result.getLastLocation();
                if (location != null) {
                    reverseGeocode(location.getLatitude(), location.getLongitude(), callback);
                } else {
                    callback.onLocationFailed("Location unavailable");
                }
                fusedClient.removeLocationUpdates(this);
            }
        };

        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
                .addOnFailureListener(e -> callback.onLocationFailed(e.getMessage()));
    }

    private void reverseGeocode(double lat, double lng, LocationCallback2 callback) {
        executor.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String city = address.getLocality();
                    if (city == null) city = address.getSubAdminArea();
                    if (city == null) city = address.getAdminArea();
                    String country = address.getCountryName();
                    String finalCity = city;
                    android.os.Handler mainHandler = new android.os.Handler(Looper.getMainLooper());
                    mainHandler.post(() -> callback.onLocationReceived(lat, lng, finalCity, country));
                } else {
                    android.os.Handler mainHandler = new android.os.Handler(Looper.getMainLooper());
                    mainHandler.post(() -> callback.onLocationFailed("Could not determine city"));
                }
            } catch (IOException e) {
                android.os.Handler mainHandler = new android.os.Handler(Looper.getMainLooper());
                mainHandler.post(() -> callback.onLocationFailed(e.getMessage()));
            }
        });
    }

    /**
     * Calculate distance between two lat/lng points using Haversine formula
     */
    public static double calculateDistanceKm(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private boolean isLocationFresh(Location location) {
        long ageMs = System.currentTimeMillis() - location.getTime();
        return ageMs < 10 * 60 * 1000; // 10 minutes
    }

    public void cleanup() {
        if (locationCallback != null) {
            fusedClient.removeLocationUpdates(locationCallback);
        }
        executor.shutdown();
    }
}
