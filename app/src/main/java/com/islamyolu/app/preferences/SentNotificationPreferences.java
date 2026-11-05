package com.islamyolu.app.preferences;

import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Calendar;
import java.util.Locale;

/**
 * Gönderilen vakit bildirimlerinin kaydı ("SentNotifications"); aynı bildirim iki kez gösterilmez.
 * Kayıt adı "Vakit_yyyy_MM_dd" biçimindedir (ör. "Öğle_2026_11_04").
 */
public final class SentNotificationPreferences {

    private static final String TAG = "SentNotifications";

    private final SharedPreferences prefs;

    SentNotificationPreferences(@NonNull SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public boolean isSent(@NonNull String notificationId) {
        return prefs.getBoolean(notificationId, false);
    }

    public void markSent(@NonNull String notificationId) {
        prefs.edit().putBoolean(notificationId, true).apply();
    }

    /** Cihaz tarihine göre {@code days} günden eski ve biçim dışı kayıtları siler. */
    public void pruneOlderThan(int days) {
        Calendar limit = Calendar.getInstance();
        limit.add(Calendar.DAY_OF_YEAR, -days);
        String oldestKept = String.format(Locale.ROOT, "%d_%02d_%02d",
                limit.get(Calendar.YEAR), limit.get(Calendar.MONTH) + 1, limit.get(Calendar.DAY_OF_MONTH));
        SharedPreferences.Editor editor = prefs.edit();
        int removed = 0;
        for (String key : prefs.getAll().keySet()) {
            int split = key.indexOf('_');
            String datePart = split >= 0 ? key.substring(split + 1) : "";
            // "yyyy_MM_dd" metin olarak sıralanabilir
            if (datePart.length() != oldestKept.length() || datePart.compareTo(oldestKept) < 0) {
                editor.remove(key);
                removed++;
            }
        }
        if (removed > 0) {
            editor.apply();
            Log.d(TAG, removed + " eski bildirim kaydı temizlendi");
        }
    }
}
