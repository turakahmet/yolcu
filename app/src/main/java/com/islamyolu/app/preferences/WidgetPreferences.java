package com.islamyolu.app.preferences;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/** Widget başına görünüm ayarları. */
public final class WidgetPreferences {

    private static final String KEY_TRANSPARENCY_PREFIX = "widget_transparency_";

    private final SharedPreferences prefs;

    WidgetPreferences(@NonNull SharedPreferences prefs) {
        this.prefs = prefs;
    }

    /** Şeffaflık; kayıt yoksa 0 (şeffaf değil). */
    public int getTransparency(int appWidgetId) {
        return prefs.getInt(KEY_TRANSPARENCY_PREFIX + appWidgetId, 0);
    }

    public void setTransparency(int appWidgetId, int transparency) {
        prefs.edit().putInt(KEY_TRANSPARENCY_PREFIX + appWidgetId, transparency).apply();
    }
}
