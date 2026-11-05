package com.islamyolu.app.settings;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.islamyolu.app.R;

/**
 * Bildirim gerektiren bir ayar açılırken Android 13+ bildirim iznini kontrol eder ve gerekirse ister.
 * Eski ekrandaki ActivityCompat.requestPermissions / onRequestPermissionsResult yerine güncel
 * Activity Result API kullanılır. Fragment oluşturulurken (onCreate'den önce) kaydedilmelidir.
 */
final class NotificationPermissionGate {

    private final Fragment fragment;
    private final ActivityResultLauncher<String> launcher;

    /** @param onGranted izin verilince çalışır (ör. anahtarları yeniden yükle) */
    NotificationPermissionGate(@NonNull Fragment fragment, @NonNull Runnable onGranted) {
        this.fragment = fragment;
        this.launcher = fragment.registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) {
                onGranted.run();
            } else if (fragment.getView() != null) {
                Snackbar.make(fragment.getView(), R.string.settings_notification_permission_denied,
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }

    /** İzin varsa true; yoksa izni ister ve false döner (çağıran anahtarı geri almalı). */
    boolean ensureGranted() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true; // Android 13 öncesinde bildirim izni kurulumla verilir
        }
        if (ContextCompat.checkSelfPermission(fragment.requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS);
        return false;
    }
}
