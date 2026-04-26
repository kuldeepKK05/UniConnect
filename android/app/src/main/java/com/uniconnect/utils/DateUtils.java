package com.uniconnect.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class DateUtils {

    private DateUtils() {}

    private static final SimpleDateFormat ISO_FORMAT;
    static {
        ISO_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        ISO_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    // ── Relative time ("2 hours ago", "3 days ago") ───────────

    public static String timeAgo(String isoDateString) {
        if (isoDateString == null || isoDateString.isEmpty()) return "";
        try {
            Date date = parseIso(isoDateString);
            if (date == null) return isoDateString;
            long diff = System.currentTimeMillis() - date.getTime();

            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours   = minutes / 60;
            long days    = hours   / 24;
            long weeks   = days    / 7;
            long months  = days    / 30;
            long years   = days    / 365;

            if (seconds < 60)   return "just now";
            if (minutes < 60)   return minutes + "m ago";
            if (hours   < 24)   return hours   + "h ago";
            if (days    < 7)    return days    + "d ago";
            if (weeks   < 5)    return weeks   + "w ago";
            if (months  < 12)   return months  + " month" + (months  == 1 ? "" : "s") + " ago";
            return years + " year" + (years == 1 ? "" : "s") + " ago";
        } catch (Exception e) {
            return isoDateString;
        }
    }

    // ── Deadline formatting ("Dec 31, 2025", "Closes in 5 days") ─

    public static String formatDeadline(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "";
        try {
            SimpleDateFormat inputFmt  = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat outputFmt = new SimpleDateFormat("MMM d, yyyy", Locale.US);
            Date date = inputFmt.parse(isoDate);
            if (date == null) return isoDate;

            long daysLeft = (date.getTime() - System.currentTimeMillis()) / 86_400_000L;

            if (daysLeft < 0)   return "Closed";
            if (daysLeft == 0)  return "Closes today!";
            if (daysLeft == 1)  return "Closes tomorrow";
            if (daysLeft <= 7)  return "Closes in " + daysLeft + " days";
            return "Deadline: " + outputFmt.format(date);
        } catch (ParseException e) {
            return isoDate;
        }
    }

    // ── Short date display ("Jan 10, 2025") ───────────────────

    public static String formatShort(String isoDateString) {
        if (isoDateString == null || isoDateString.isEmpty()) return "";
        try {
            Date date = parseIso(isoDateString);
            if (date == null) return isoDateString;
            return new SimpleDateFormat("MMM d, yyyy", Locale.US).format(date);
        } catch (Exception e) {
            return isoDateString;
        }
    }

    // ── Is deadline approaching (within 7 days) ───────────────

    public static boolean isUrgent(String isoDeadline) {
        if (isoDeadline == null) return false;
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date date = fmt.parse(isoDeadline);
            if (date == null) return false;
            long daysLeft = (date.getTime() - System.currentTimeMillis()) / 86_400_000L;
            return daysLeft >= 0 && daysLeft <= 7;
        } catch (ParseException e) {
            return false;
        }
    }

    // ── Parse ISO 8601 string → Date ──────────────────────────

    private static Date parseIso(String s) {
        // Trim to seconds to normalize "2025-01-10T08:00:00Z" etc.
        String normalized = s.length() > 19 ? s.substring(0, 19) : s;
        try {
            return ISO_FORMAT.parse(normalized);
        } catch (ParseException e) {
            return null;
        }
    }
}
