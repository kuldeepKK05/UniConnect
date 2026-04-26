package com.uniconnect;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.uniconnect.utils.SessionManager;

public class UniConnectApp extends Application {

    private static UniConnectApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Respect system dark/light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public static UniConnectApp getInstance() { return instance; }
}