package com.islamyolu.app.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.islamyolu.app.R;
import com.islamyolu.app.theme.AppThemeManager;

/** Uygulama Ayarları: tema (Sistem / Açık / Koyu). Seçim AppThemeManager ile kaydedilir ve uygulanır. */
public class AppSettingsFragment extends Fragment {

    public AppSettingsFragment() {
        super(R.layout.fragment_settings_app);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        RadioGroup group = view.findViewById(R.id.themeGroup);
        group.check(buttonFor(AppThemeManager.getMode(requireContext())));
        // check() dinleyiciden önce: yalnızca kullanıcı seçimi uygulanır. Uygulanınca ekran yeni temayla
        // yeniden oluşturulur ve seçim tercihten tekrar okunur.
        group.setOnCheckedChangeListener((g, checkedId) ->
                AppThemeManager.setMode(requireContext(), modeFor(checkedId)));
    }

    private static int buttonFor(AppThemeManager.Mode mode) {
        switch (mode) {
            case LIGHT:
                return R.id.themeLight;
            case DARK:
                return R.id.themeDark;
            default:
                return R.id.themeSystem;
        }
    }

    private static AppThemeManager.Mode modeFor(int buttonId) {
        if (buttonId == R.id.themeLight) return AppThemeManager.Mode.LIGHT;
        if (buttonId == R.id.themeDark) return AppThemeManager.Mode.DARK;
        return AppThemeManager.Mode.SYSTEM;
    }
}
