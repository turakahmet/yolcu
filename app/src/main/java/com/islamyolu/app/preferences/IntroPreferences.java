package com.islamyolu.app.preferences;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/** Tanıtım ekranı ("IntroPrefs"). */
public final class IntroPreferences {

    private static final String KEY_COMPLETED = "isIntroCompleted";

    private final SharedPreferences prefs;

    IntroPreferences(@NonNull SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public boolean isCompleted() {
        return prefs.getBoolean(KEY_COMPLETED, false);
    }

    public void setCompleted() {
        prefs.edit().putBoolean(KEY_COMPLETED, true).apply();
    }
}
