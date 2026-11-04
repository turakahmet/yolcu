package com.islamyolu.app.Service;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.provider.Settings;

import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import com.islamyolu.app.R;
import com.islamyolu.app.Utils.AppLogger;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Ekranların konuma erişimi için tek giriş noktası. Her ekranda aynı kodu yazmak yerine:
 * <pre>
 * private final LocationAccess locationAccess = LocationAccess.register(this, options, callback);
 * ...
 * locationAccess.requestSingleLocation();
 * </pre>
 * Sırasıyla: izin (yoksa ister) → konum servisi (kapalıysa "Konum kapalı" penceresi ve ayarlara
 * yönlendirme; dönüşte kendiliğinden devam) → {@link LocationService} ile tek seferlik konum.
 * <p>
 * {@link #register} Activity/Fragment oluşturulurken (alan tanımında veya onCreate'te) çağrılmalıdır;
 * Activity Result API bunu gerektirir. Ekran dönse de izin ve ayar sonuçları yeni ekrana ulaşır.
 * Geri çağrı ekran döndükten sonra da gelebileceği için sonuç ViewModel'e iletilmelidir.
 */
public final class LocationAccess {

    private static final String TAG = "LocationAccess";

    public interface Callback {
        void onLocation(@NonNull Location location);

        void onUnavailable(@NonNull LocationService.Unavailable reason);
    }

    /** Ekrana özel ayarlar; varsayılanlar çoğu ekran için yeterlidir. */
    public static final class Options {
        boolean precise;
        boolean fresh;
        @StringRes int servicesOffMessage = R.string.location_off_message;
        @StringRes int declineLabel = R.string.location_off_decline;

        /** Hassas konum (ör. kıble pusulası). Varsayılan: şehir düzeyi kaba konum. */
        public Options precise(boolean value) {
            precise = value;
            return this;
        }

        /**
         * Önbellekteki eski konum kullanılmaz; her istekte taze konum alınır.
         * Kullanıcı açıkça "konumumu bul" dediğinde (yer değiştirmiş olabilir). Varsayılan: hızlı, hazır konum.
         */
        public Options fresh(boolean value) {
            fresh = value;
            return this;
        }

        /** "Konum kapalı" penceresindeki açıklama. */
        public Options servicesOffMessage(@StringRes int value) {
            servicesOffMessage = value;
            return this;
        }

        /** "Konum kapalı" penceresindeki vazgeç butonu. */
        public Options declineLabel(@StringRes int value) {
            declineLabel = value;
            return this;
        }
    }

    private final Supplier<Context> context;
    private final Supplier<FragmentManager> fragmentManager;
    private final Options options;
    private final Callback callback;
    private final ActivityResultLauncher<String[]> permissionLauncher;
    private final ActivityResultLauncher<Intent> settingsLauncher;
    private boolean askToEnableServices = true;

    @NonNull
    public static LocationAccess register(@NonNull FragmentActivity activity, @NonNull Options options,
                                          @NonNull Callback callback) {
        return new LocationAccess(activity, activity, () -> activity,
                activity::getSupportFragmentManager, options, callback);
    }

    @NonNull
    public static LocationAccess register(@NonNull Fragment fragment, @NonNull Options options,
                                          @NonNull Callback callback) {
        return new LocationAccess(fragment, fragment, fragment::requireContext,
                fragment::getChildFragmentManager, options, callback);
    }

    private LocationAccess(ActivityResultCaller caller, LifecycleOwner owner, Supplier<Context> context,
                           Supplier<FragmentManager> fragmentManager, Options options, Callback callback) {
        this.context = context;
        this.fragmentManager = fragmentManager;
        this.options = options;
        this.callback = callback;

        permissionLauncher = caller.registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), this::onPermissionResult);
        settingsLauncher = caller.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> onReturnedFromSettings());

        // Fragment'ın FragmentManager'ı ancak oluşturulunca hazır olur; dinleyici o an bağlanır
        owner.getLifecycle().addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
                if (event == Lifecycle.Event.ON_CREATE) {
                    source.getLifecycle().removeObserver(this);
                    fragmentManager.get().setFragmentResultListener(LocationEnableDialogFragment.RESULT_KEY, owner,
                            (key, bundle) -> onEnableDialogAnswered(
                                    bundle.getBoolean(LocationEnableDialogFragment.RESULT_ACCEPTED)));
                }
            }
        });
    }

    /** İzin, konum servisi ve tek seferlik konum adımlarını sırayla yürütür. */
    public void requestSingleLocation() {
        requestSingleLocation(true);
    }

    /**
     * @param askToEnableServices false ise konum kapalıyken pencere gösterilmez, doğrudan
     *                            {@link LocationService.Unavailable#SERVICES_OFF} döner (ör. açılışta sessiz deneme).
     */
    public void requestSingleLocation(boolean askToEnableServices) {
        this.askToEnableServices = askToEnableServices;
        if (service().hasLocationPermission()) {
            continueWhenServicesOn();
        } else {
            permissionLauncher.launch(options.precise
                    ? new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}
                    : new String[]{Manifest.permission.ACCESS_COARSE_LOCATION});
        }
    }

    private void onPermissionResult(Map<String, Boolean> result) {
        if (result.containsValue(Boolean.TRUE)) {
            continueWhenServicesOn();
        } else {
            callback.onUnavailable(LocationService.Unavailable.PERMISSION_DENIED);
        }
    }

    private void continueWhenServicesOn() {
        if (service().isLocationEnabled()) {
            fetch();
            return;
        }
        FragmentManager fm = fragmentManager.get();
        if (!askToEnableServices || fm.isStateSaved()) {
            callback.onUnavailable(LocationService.Unavailable.SERVICES_OFF);
        } else if (fm.findFragmentByTag(LocationEnableDialogFragment.TAG) == null) {
            LocationEnableDialogFragment.newInstance(options.servicesOffMessage, options.declineLabel)
                    .show(fm, LocationEnableDialogFragment.TAG);
        }
    }

    private void onEnableDialogAnswered(boolean accepted) {
        if (!accepted) {
            callback.onUnavailable(LocationService.Unavailable.SERVICES_OFF);
            return;
        }
        try {
            settingsLauncher.launch(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        } catch (ActivityNotFoundException e) {
            // Bazı üretici arayüzlerinde bu ekran yok: genel ayarlar açılır
            AppLogger.warn(TAG, "Konum ayarları ekranı bulunamadı");
            settingsLauncher.launch(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private void onReturnedFromSettings() {
        if (service().isLocationEnabled()) {
            fetch();
        } else {
            callback.onUnavailable(LocationService.Unavailable.SERVICES_OFF);
        }
    }

    private void fetch() {
        service().getSingleLocation(options.fresh, new LocationService.SingleLocationCallback() {
            @Override
            public void onLocation(@NonNull Location location) {
                callback.onLocation(location);
            }

            @Override
            public void onUnavailable(@NonNull LocationService.Unavailable reason) {
                callback.onUnavailable(reason);
            }
        });
    }

    private LocationService service() {
        return LocationService.getInstance(context.get());
    }
}
