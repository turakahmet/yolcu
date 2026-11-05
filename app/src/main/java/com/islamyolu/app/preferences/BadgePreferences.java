package com.islamyolu.app.preferences;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/** Kazanılan rozetler ("BadgePreferences"); anahtar rozet türünün enum adıdır. */
public final class BadgePreferences {

    private final SharedPreferences prefs;

    BadgePreferences(@NonNull SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public boolean isEarned(@NonNull String badgeName) {
        return prefs.getBoolean(badgeName, false);
    }

    public void setEarned(@NonNull String badgeName) {
        prefs.edit().putBoolean(badgeName, true).apply();
    }
}
