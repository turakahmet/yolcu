package com.islamyolu.app.main;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.islamyolu.app.R;

/**
 * Ayarlar kabuğu: yeşil başlık + başlık ekranları arasında gezinme (settings_navigation).
 * Her başlığın mantığı kendi Fragment'ındadır (com.islamyolu.app.settings); bu sınıf yalnızca
 * gezinmeyi, başlığı ve kenardan kenara (edge-to-edge) boşlukları yönetir.
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Android 15+ (targetSdk 35+) kenardan kenara zorunlu; eski sürümlerde de aynı görünüm
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.settingsToolbar);
        NavHostFragment host = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.settingsNavHost);
        if (host == null) {
            throw new IllegalStateException("settingsNavHost bulunamadı");
        }
        NavController navController = host.getNavController();
        navController.addOnDestinationChangedListener((controller, destination, arguments) ->
                toolbar.setTitle(destination.getLabel()));
        // Geri oku: alt ekrandan ana menüye, ana menüden uygulamaya döner
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        applyInsets();
    }

    /** Başlık durum çubuğunun altına uzanır; içerik gezinme çubuğu/kesik alanla çakışmaz. */
    private void applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settingsHeader), (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, view.getPaddingBottom());
            return windowInsets;
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settingsNavHost), (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, 0, bars.right, bars.bottom);
            return windowInsets;
        });
    }
}
