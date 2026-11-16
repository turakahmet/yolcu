package com.islamyolu.app.main;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.islamyolu.app.R;
import com.islamyolu.app.theme.SystemBars;

/**
 * Ayarlar kabuğu: yeşil başlık + başlık ekranları arasında gezinme (settings_navigation).
 * Her başlığın mantığı kendi Fragment'ındadır (com.islamyolu.app.settings); bu sınıf yalnızca
 * gezinmeyi, başlığı ve kenardan kenara (edge-to-edge) boşlukları yönetir.
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Android 15+ (targetSdk 35+) kenardan kenara zorunlu; eski sürümlerde de aynı görünüm
        SystemBars.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.settingsToolbar);
        NavHostFragment host = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.settingsNavHost);
        if (host == null) {
            throw new IllegalStateException("settingsNavHost bulunamadı");
        }
        NavController navController = host.getNavController();
        navController.addOnDestinationChangedListener((controller, destination, arguments) ->
                // "{prayerTitle}" gibi etiketler argümanla doldurulur (vakit detay ekranı)
                toolbar.setTitle(destination.fillInLabel(this, arguments)));
        // Geri oku: alt ekrandan ana menüye, ana menüden uygulamaya döner
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // Ana ekrandaki zil doğrudan Bildirimler'i açar; geri dönünce Ayarlar ana menüsü görünür
        if (savedInstanceState == null && getIntent().getBooleanExtra(EXTRA_OPEN_NOTIFICATIONS, false)) {
            navController.navigate(R.id.toNotifications);
        }

        // Başlık durum çubuğunun altına uzanır; içerik gezinme çubuğu/kesik alanla çakışmaz
        SystemBars.padHeader(findViewById(R.id.settingsHeader));
        SystemBars.padContent(findViewById(R.id.settingsNavHost));
    }

    private static final String EXTRA_OPEN_NOTIFICATIONS = "openNotifications";

    /** Ayarlar'ı Bildirimler ekranında açar. */
    @NonNull
    public static Intent notificationsIntent(@NonNull Context context) {
        return new Intent(context, SettingsActivity.class).putExtra(EXTRA_OPEN_NOTIFICATIONS, true);
    }
}
