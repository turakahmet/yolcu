package com.islamyolu.app.Service;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;

/**
 * Pil optimizasyonu durumu ve ayar ekranına yönlendirme (tek yer).
 * <p>
 * Doğrudan muafiyet isteyen REQUEST_IGNORE_BATTERY_OPTIMIZATIONS izni Play politikasında kısıtlı
 * olduğu için kullanılmaz; kullanıcı sistemin kendi listesine yönlendirilir. Honor/Huawei gibi
 * üreticilerde ayrıca "Uygulama başlatma" ayarı vardır; sistem bunu bildirmez, metinle anlatılır.
 */
public final class BatteryOptimization {

    private static final String TAG = "BatteryOptimization";

    private BatteryOptimization() {
    }

    /** Uygulama pil optimizasyonundan muafsa true (alarmlar ve arka plan işleri kısıtlanmaz). */
    public static boolean isIgnoring(@NonNull Context context) {
        PowerManager pm = context.getSystemService(PowerManager.class);
        return pm != null && pm.isIgnoringBatteryOptimizations(context.getPackageName());
    }

    /** Pil optimizasyonu listesini, açılamazsa uygulamanın sistem ayarlarını açar. */
    public static void openSettings(@NonNull Activity activity) {
        try {
            activity.startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "Pil optimizasyonu ekranı yok; uygulama ayarları açılıyor", e);
            activity.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", activity.getPackageName(), null)));
        }
    }
}
