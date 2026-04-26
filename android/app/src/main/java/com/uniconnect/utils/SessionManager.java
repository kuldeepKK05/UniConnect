package com.uniconnect.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.uniconnect.models.User;

public class SessionManager {

    private static final String PREF_NAME = "UniConnectSession";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_USER = "user_json";
    private static final String KEY_USER_LAT = "user_lat";
    private static final String KEY_USER_LNG = "user_lng";
    private static final String KEY_USER_CITY = "user_city";
    private static final String KEY_SORT_PREF = "sort_preference";
    private static final String KEY_SCOPE_PREF = "scope_preference";
    private static final String KEY_RADIUS_KM = "radius_km";
    private static final String KEY_DIFFICULTY = "difficulty_level";
    private static final String KEY_LOCATION_SET = "location_set";

    private static SessionManager instance;
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    private SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context.getApplicationContext());
        }
        return instance;
    }

    // ---- Token ----

    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    // ---- User ----

    public void saveUser(User user) {
        prefs.edit().putString(KEY_USER, gson.toJson(user)).apply();
    }

    public User getUser() {
        String json = prefs.getString(KEY_USER, null);
        if (json == null) return null;
        return gson.fromJson(json, User.class);
    }

    public boolean isAdmin() {
        User user = getUser();
        return user != null && user.isAdmin();
    }

    // ---- Location ----

    public void saveLocation(double lat, double lng, String city) {
        prefs.edit()
                .putFloat(KEY_USER_LAT, (float) lat)
                .putFloat(KEY_USER_LNG, (float) lng)
                .putString(KEY_USER_CITY, city)
                .putBoolean(KEY_LOCATION_SET, true)
                .apply();
    }

    public double getUserLat() {
        return prefs.getFloat(KEY_USER_LAT, 0f);
    }

    public double getUserLng() {
        return prefs.getFloat(KEY_USER_LNG, 0f);
    }

    public String getUserCity() {
        return prefs.getString(KEY_USER_CITY, "");
    }

    public boolean isLocationSet() {
        return prefs.getBoolean(KEY_LOCATION_SET, false);
    }

    // ---- Feed preferences ----

    public void saveSortPreference(String sort) {
        prefs.edit().putString(KEY_SORT_PREF, sort).apply();
    }

    public String getSortPreference() {
        return prefs.getString(KEY_SORT_PREF, "LATEST");
    }

    public void saveScopePreference(String scope) {
        prefs.edit().putString(KEY_SCOPE_PREF, scope).apply();
    }

    public String getScopePreference() {
        return prefs.getString(KEY_SCOPE_PREF, "NEAR_ME");
    }

    public void saveRadius(int radiusKm) {
        prefs.edit().putInt(KEY_RADIUS_KM, radiusKm).apply();
    }

    public int getRadius() {
        return prefs.getInt(KEY_RADIUS_KM, 50);
    }

    // ---- Difficulty (optional feature) ----

    public void saveDifficulty(String difficulty) {
        prefs.edit().putString(KEY_DIFFICULTY, difficulty).apply();
    }

    public String getDifficulty() {
        return prefs.getString(KEY_DIFFICULTY, "Medium");
    }

    // ---- Clear session ----

    public void clearSession() {
        prefs.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_USER)
                .apply();
    }

    public void clearAll() {
        prefs.edit().clear().apply();
    }
}
