package com.islamyolu.app.preferences;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Kullanıcı profili ("user_prefs"): kullanıcı adı ve profil resmi. */
public final class UserPreferences {

    private static final String KEY_USERNAME = "username";
    private static final String KEY_AVATAR = "profile_picture_name";

    private final SharedPreferences prefs;

    UserPreferences(@NonNull SharedPreferences prefs) {
        this.prefs = prefs;
    }

    @Nullable
    public String getUsername(@Nullable String defaultValue) {
        return prefs.getString(KEY_USERNAME, defaultValue);
    }

    public void setUsername(@NonNull String username) {
        prefs.edit().putString(KEY_USERNAME, username).apply();
    }

    /** Profil resminin Avatar enum adı. */
    @Nullable
    public String getAvatarName(@Nullable String defaultValue) {
        return prefs.getString(KEY_AVATAR, defaultValue);
    }

    public void setAvatarName(@NonNull String avatarName) {
        prefs.edit().putString(KEY_AVATAR, avatarName).apply();
    }
}
