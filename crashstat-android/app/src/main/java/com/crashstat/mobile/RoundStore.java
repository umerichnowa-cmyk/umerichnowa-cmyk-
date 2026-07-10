package com.crashstat.mobile;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class RoundStore {
    private static final String PREFS = "crashstat";
    private static final String KEY_VALUES = "values";
    private static final String KEY_AUTO_COUNT = "auto_count";
    private static final String KEY_ACTIVE = "screen_reader_active";
    private static final String KEY_LAST_AUTO_VALUE = "last_auto_value";
    private static final String KEY_LAST_AUTO_TIME = "last_auto_time";
    private static final String CSV_FILE = "captured_rounds.csv";

    private RoundStore() {}

    static synchronized ArrayList<Double> load(Context context) {
        ArrayList<Double> values = new ArrayList<>();
        String saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_VALUES, "");
        if (saved == null || saved.trim().isEmpty()) return values;
        for (String part : saved.split(",")) {
            try {
                if (!part.trim().isEmpty()) values.add(Double.parseDouble(part.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return values;
    }

    static synchronized void save(Context context, List<Double> values) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) out.append(',');
            out.append(values.get(i));
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_VALUES, out.toString())
                .apply();
    }

    static synchronized int appendAutomatic(Context context, double value, long timestamp) {
        ArrayList<Double> values = load(context);
        values.add(value);
        save(context, values);

        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int count = prefs.getInt(KEY_AUTO_COUNT, 0) + 1;
        prefs.edit()
                .putInt(KEY_AUTO_COUNT, count)
                .putFloat(KEY_LAST_AUTO_VALUE, (float) value)
                .putLong(KEY_LAST_AUTO_TIME, timestamp)
                .apply();

        String row = timestamp + "," + value + ",screen_ocr\n";
        try (FileOutputStream output = context.openFileOutput(CSV_FILE, Context.MODE_APPEND)) {
            output.write(row.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
        return count;
    }

    static int automaticCount(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_AUTO_COUNT, 0);
    }

    static double lastAutomaticValue(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getFloat(KEY_LAST_AUTO_VALUE, 0f);
    }

    static long lastAutomaticTime(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getLong(KEY_LAST_AUTO_TIME, 0L);
    }

    static void setReaderActive(Context context, boolean active) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ACTIVE, active).apply();
    }

    static boolean isReaderActive(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_ACTIVE, false);
    }
}
