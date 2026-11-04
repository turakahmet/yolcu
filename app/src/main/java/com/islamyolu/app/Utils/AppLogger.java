package com.islamyolu.app.Utils;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Merkezi hata/olay kaydı. Hatalar sessizce yutulmaz; buradan kaydedilir.
 * İleride uzak hata raporlama eklenirse tek değişiklik noktası burasıdır.
 * Kişisel veri (konum, ad, cihaz kimliği vb.) ve gizli bilgi loglanmaz.
 */
public final class AppLogger {

    private AppLogger() {
    }

    public static void error(@NonNull String tag, @NonNull String message, @Nullable Throwable error) {
        Log.e(tag, message, error);
    }

    public static void warn(@NonNull String tag, @NonNull String message) {
        Log.w(tag, message);
    }

    public static void info(@NonNull String tag, @NonNull String message) {
        Log.i(tag, message);
    }
}
