package com.islamyolu.app.Service;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.islamyolu.app.Utils.AppLogger;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Koordinatı adrese çeviren tek yer (Geocoder). Ana thread'de ağ/disk işi yapılmaz:
 * {@link #resolve} arka plan thread'inden, {@link #resolveAsync} her yerden çağrılır.
 * Koordinat ve adres loglanmaz (kişisel veri).
 */
public final class AddressResolver {

    public interface Callback {
        /** Ana thread'de çağrılır; adres bulunamazsa null. */
        @MainThread
        void onResult(@Nullable Address address);
    }

    private static final String TAG = "AddressResolver";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private final Context context;

    public AddressResolver(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * @param locale namaz vakti eşleştirmesinde Locale.ENGLISH (§8); ekranda gösterimde cihaz dili
     */
    @WorkerThread
    @Nullable
    @SuppressWarnings("deprecation") // Senkron sürüm tüm API seviyelerinde aynı çalışır; ana thread'de çağrılmaz
    public Address resolve(double latitude, double longitude, @NonNull Locale locale) {
        if (!Geocoder.isPresent()) {
            AppLogger.warn(TAG, "Cihazda Geocoder yok");
            return null;
        }
        try {
            List<Address> addresses = new Geocoder(context, locale).getFromLocation(latitude, longitude, 1);
            return addresses == null || addresses.isEmpty() ? null : addresses.get(0);
        } catch (IOException | IllegalArgumentException e) {
            AppLogger.error(TAG, "Adres çözümlenemedi", e);
            return null;
        }
    }

    /** Arka planda çözümler, sonucu ana thread'e döndürür. */
    public void resolveAsync(double latitude, double longitude, @NonNull Locale locale, @NonNull Callback callback) {
        EXECUTOR.execute(() -> {
            Address address = resolve(latitude, longitude, locale);
            MAIN_HANDLER.post(() -> callback.onResult(address));
        });
    }
}
