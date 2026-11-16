package com.islamyolu.app.theme;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatDelegate;

import com.islamyolu.app.R;
import com.islamyolu.app.preferences.AppPreferences;

/**
 * Uygulama temasının (Sistem / Açık / Koyu) TEK yönetim noktası.
 * <ul>
 *   <li>Seçim {@link com.islamyolu.app.preferences.SettingsPreferences}'ta kalıcı saklanır.</li>
 *   <li>Uygulama açılışında {@link #applySaved} ile uygulanır; "Sistem" seçiliyken Android'in açık/koyu
 *       tema değişimine kendiliğinden uyulur.</li>
 *   <li>{@link #setMode} AppCompatDelegate üzerinden açık ekranları kendisi yeniler.</li>
 * </ul>
 */
public final class AppThemeManager {

    /** Kayıtlı değerler (sıra değiştirilmez: tercih dosyasında sayı olarak tutulur). */
    public enum Mode {
        SYSTEM(0, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, R.string.theme_system),
        LIGHT(1, AppCompatDelegate.MODE_NIGHT_NO, R.string.theme_light),
        DARK(2, AppCompatDelegate.MODE_NIGHT_YES, R.string.theme_dark);

        final int storedValue;
        final int nightMode;
        @StringRes public final int labelRes;

        Mode(int storedValue, int nightMode, @StringRes int labelRes) {
            this.storedValue = storedValue;
            this.nightMode = nightMode;
            this.labelRes = labelRes;
        }

        @NonNull
        static Mode fromStored(int value) {
            for (Mode mode : values()) {
                if (mode.storedValue == value) return mode;
            }
            return SYSTEM;
        }
    }

    private AppThemeManager() {
    }

    /** Uygulama açılışında (Application.onCreate) çağrılır. */
    public static void applySaved(@NonNull Context context) {
        AppCompatDelegate.setDefaultNightMode(getMode(context).nightMode);
    }

    @NonNull
    public static Mode getMode(@NonNull Context context) {
        return Mode.fromStored(AppPreferences.from(context).settings().getThemeMode());
    }

    /** Seçimi kaydeder ve uygular; açık ekranlar AppCompat tarafından yeniden oluşturulur. */
    public static void setMode(@NonNull Context context, @NonNull Mode mode) {
        if (mode == getMode(context)) return;
        AppPreferences.from(context).settings().setThemeMode(mode.storedValue);
        AppCompatDelegate.setDefaultNightMode(mode.nightMode);
    }
}
