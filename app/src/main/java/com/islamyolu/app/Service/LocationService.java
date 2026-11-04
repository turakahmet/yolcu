package com.islamyolu.app.Service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.islamyolu.app.Utils.AppLogger;

/**
 * Merkezi konum servisi: izin ve konum servisi durumu ile tek seferlik konum alma.
 * Ekranlar bunu doğrudan değil, izin isteme ve ayarlara yönlendirmeyi de yapan
 * {@link LocationAccess} üzerinden kullanır. Adres çözümleme {@link AddressResolver}'dadır.
 */
public class LocationService {

    private static final String TAG = "LocationService";
    private static LocationService instance;
    private final Context context;
    private final FusedLocationProviderClient fusedLocationClient;

    /** Konumun alınamama nedeni; kullanıcıya doğru mesajı göstermek için ayrılır. */
    public enum Unavailable {
        /** Konum izni yok */
        PERMISSION_DENIED,
        /** Cihazın konum servisi (GPS/ağ) kapalı */
        SERVICES_OFF,
        /** Servis açık ama konum belirlenemedi (sinyal yok, kapalı alan, emülatör vb.) */
        NO_FIX
    }

    public interface SingleLocationCallback {
        void onLocation(@NonNull Location location);

        void onUnavailable(@NonNull Unavailable reason);
    }

    private LocationService(Context context) {
        this.context = context.getApplicationContext();
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.context);
    }

    public static synchronized LocationService getInstance(Context context) {
        if (instance == null) {
            instance = new LocationService(context);
        }
        return instance;
    }

    /** Cihazın konum servisi (GPS veya ağ) açık mı. */
    public boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null
                && (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    /** Kaba (coarse) veya hassas (fine) konum izinlerinden biri verilmiş mi. */
    public boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /** fresh=false iken bu kadar yeni hazır konum beklemeden kullanılır (açılış, kıble gibi). */
    private static final long MAX_LOCATION_AGE_MS = 10 * 60 * 1000L;
    /** Pil dostu (ağ) deneme süresi */
    private static final long BALANCED_TIMEOUT_MS = 8_000L;
    /** GPS deneme süresi */
    private static final long HIGH_ACCURACY_TIMEOUT_MS = 15_000L;

    /** Hazır konum kabul edilir (hızlı). */
    public void getSingleLocation(@NonNull SingleLocationCallback callback) {
        getSingleLocation(false, callback);
    }

    /**
     * Tek bir konum alır; sürekli güncelleme başlatmaz. Ağ (pil dostu) ve GPS aynı anda denenir,
     * önce gelen kullanılır, diğeri iptal edilir; ikisi de boş dönerse son bilinen konuma düşülür.
     * Sonuç ana thread'de döner.
     *
     * @param fresh true ise önbellekteki eski konum kullanılmaz (kullanıcı "konumumu bul" dediğinde;
     *              kullanıcı yer değiştirdiyse eski konum dönmesin)
     */
    @SuppressLint("MissingPermission") // izin hemen aşağıda kontrol ediliyor
    public void getSingleLocation(boolean fresh, @NonNull SingleLocationCallback callback) {
        if (!hasLocationPermission()) {
            callback.onUnavailable(Unavailable.PERMISSION_DENIED);
            return;
        }
        if (!isLocationEnabled()) {
            callback.onUnavailable(Unavailable.SERVICES_OFF);
            return;
        }
        long maxAge = fresh ? 0 : MAX_LOCATION_AGE_MS;
        CancellationTokenSource cancellation = new CancellationTokenSource();
        boolean[] delivered = {false};
        int[] pending = {2};
        OnCurrentResult onResult = location -> {
            if (delivered[0]) {
                return;
            }
            if (location != null) {
                delivered[0] = true;
                cancellation.cancel(); // diğer deneme gereksiz
                callback.onLocation(location);
            } else if (--pending[0] == 0) {
                requestLastKnown(callback);
            }
        };
        requestCurrent(Priority.PRIORITY_BALANCED_POWER_ACCURACY, BALANCED_TIMEOUT_MS, maxAge, cancellation, onResult);
        requestCurrent(Priority.PRIORITY_HIGH_ACCURACY, HIGH_ACCURACY_TIMEOUT_MS, maxAge, cancellation, onResult);
    }

    private interface OnCurrentResult {
        void onResult(Location location);
    }

    @SuppressLint("MissingPermission") // getSingleLocation() izni kontrol ediyor
    private void requestCurrent(int priority, long timeoutMs, long maxAgeMs,
                                CancellationTokenSource cancellation, OnCurrentResult onResult) {
        CurrentLocationRequest request = new CurrentLocationRequest.Builder()
                .setPriority(priority)
                .setMaxUpdateAgeMillis(maxAgeMs)
                .setDurationMillis(timeoutMs)
                .build();
        fusedLocationClient.getCurrentLocation(request, cancellation.getToken())
                .addOnSuccessListener(onResult::onResult)
                .addOnFailureListener(e -> {
                    if (!cancellation.getToken().isCancellationRequested()) {
                        AppLogger.error(TAG, "Güncel konum alınamadı (öncelik " + priority + ")", e);
                    }
                    onResult.onResult(null);
                });
    }

    @SuppressLint("MissingPermission") // getSingleLocation() izni kontrol ediyor
    private void requestLastKnown(SingleLocationCallback callback) {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        callback.onLocation(location);
                    } else {
                        AppLogger.warn(TAG, "Konum belirlenemedi; son bilinen konum da yok");
                        callback.onUnavailable(Unavailable.NO_FIX);
                    }
                })
                .addOnFailureListener(e -> {
                    AppLogger.error(TAG, "Son bilinen konum alınamadı", e);
                    callback.onUnavailable(Unavailable.NO_FIX);
                });
    }
}
