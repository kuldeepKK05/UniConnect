package com.uniconnect.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;

import org.json.JSONObject;

import java.io.IOException;

import retrofit2.Response;

public final class NetworkUtils {

    private NetworkUtils() {}

    // ── Connectivity ──────────────────────────────────────────

    public static boolean isConnected(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.net.Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            return caps != null && (
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            android.net.NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }
    }

    // ── Error message extraction from Retrofit response ───────

    public static String extractErrorMessage(Response<?> response) {
        if (response == null) return "Unknown error";
        try {
            if (response.errorBody() != null) {
                String raw = response.errorBody().string();
                JSONObject json = new JSONObject(raw);
                if (json.has("error"))   return json.getString("error");
                if (json.has("message")) return json.getString("message");
            }
        } catch (Exception ignored) {}
        return httpCodeToMessage(response.code());
    }

    public static String httpCodeToMessage(int code) {
        switch (code) {
            case 400: return "Bad request. Please check your input.";
            case 401: return "You are not logged in. Please sign in.";
            case 403: return "You don't have permission to do this.";
            case 404: return "Content not found.";
            case 409: return "A conflict occurred. This may already exist.";
            case 422: return "Invalid data submitted.";
            case 429: return "Too many requests. Please wait a moment.";
            case 500: return "Server error. Please try again later.";
            case 503: return "Service unavailable. Please try again later.";
            default:  return "Something went wrong (code " + code + ").";
        }
    }

    public static boolean isNetworkError(Throwable t) {
        return t instanceof IOException;
    }

    public static String throwableToMessage(Throwable t) {
        if (t == null) return "Unknown network error";
        if (isNetworkError(t)) return "No internet connection. Please check your network.";
        return t.getMessage() != null ? t.getMessage() : "Network error occurred";
    }
}
