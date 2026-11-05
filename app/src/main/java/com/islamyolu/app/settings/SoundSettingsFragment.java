package com.islamyolu.app.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.ListAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.islamyolu.app.R;
import com.islamyolu.app.VakitApi.PrayerAlarmScheduler;
import com.islamyolu.app.preferences.AppPreferences;
import com.islamyolu.app.preferences.SettingsPreferences;

/**
 * Uygulama sesleri, namazda titreşim ve ezan/dua seçimi. Seçim listesi (exposed dropdown) yalnızca
 * kullanıcı seçtiğinde bildirir; eski Spinner'daki "ilk tetiklemeyi yok say" hilesine gerek kalmaz.
 */
public class SoundSettingsFragment extends Fragment {

    private SettingsPreferences settings;

    public SoundSettingsFragment() {
        super(R.layout.fragment_settings_sound);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        settings = AppPreferences.from(requireContext()).settings();

        SettingsRowView rowAppSounds = view.findViewById(R.id.rowAppSounds);
        rowAppSounds.setChecked(settings.isSwitchOn(SettingsPreferences.KEY_SOUND));
        rowAppSounds.setOnToggleListener((row, checked) -> settings.setSwitch(SettingsPreferences.KEY_SOUND, checked));

        SettingsRowView rowVibrate = view.findViewById(R.id.rowVibrateAtPrayer);
        rowVibrate.setChecked(settings.isVakitSoundModeOn());
        rowVibrate.setOnToggleListener((row, checked) -> settings.setVakitSoundMode(checked));

        bindDropdown(view.findViewById(R.id.ezanSoundDropdown), settings.getEzanSoundType(), position -> {
            settings.setEzanSoundType(position);
            rescheduleAlarms(); // ses alarmları seçime göre yeniden kurulur ("Kapalı" ise kurulmaz)
        });
        bindDropdown(view.findViewById(R.id.ezanDuaDropdown), settings.getEzanDuaType(), position -> {
            settings.setEzanDuaType(position);
            rescheduleAlarms();
        });
    }

    private interface OnPositionSelected {
        void onSelected(int position);
    }

    private static void bindDropdown(MaterialAutoCompleteTextView dropdown, int selected, OnPositionSelected callback) {
        ListAdapter adapter = dropdown.getAdapter();
        if (adapter != null && selected >= 0 && selected < adapter.getCount()) {
            dropdown.setText(String.valueOf(adapter.getItem(selected)), false); // false: listeyi filtreleme
        }
        dropdown.setOnItemClickListener((parent, view, position, id) -> callback.onSelected(position));
    }

    private void rescheduleAlarms() {
        PrayerAlarmScheduler.rescheduleAsync(requireContext(), null);
    }
}
