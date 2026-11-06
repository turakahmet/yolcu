package com.islamyolu.app.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.ListAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.islamyolu.app.R;
import com.islamyolu.app.sound.PrayerSound;
import com.islamyolu.app.sound.SoundManager;
import com.islamyolu.app.sound.SoundPreviewPlayer;
import com.islamyolu.app.VakitApi.PrayerAlarmScheduler;
import com.islamyolu.app.preferences.AppPreferences;
import com.islamyolu.app.preferences.SettingsPreferences;

/**
 * Uygulama sesleri, namazda titreşim ve ezan/dua seçimi + seçilen sesin önizlemesi.
 * Seçim listesi (exposed dropdown) yalnızca kullanıcı seçtiğinde bildirir.
 */
public class SoundSettingsFragment extends Fragment {

    /** Titreşimde kalma süresi seçenekleri (dakika); varsayılan 20 */
    private static final int[] SILENCE_MINUTE_OPTIONS = {10, 20, 30, 45};

    private SettingsPreferences settings;
    @Nullable private SoundPreviewPlayer preview;

    public SoundSettingsFragment() {
        super(R.layout.fragment_settings_sound);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        settings = AppPreferences.from(requireContext()).settings();
        preview = SoundManager.from(requireContext()).newPreviewPlayer();

        SettingsRowView rowAppSounds = view.findViewById(R.id.rowAppSounds);
        rowAppSounds.setChecked(settings.isSwitchOn(SettingsPreferences.KEY_SOUND));
        rowAppSounds.setOnToggleListener((row, checked) -> settings.setSwitch(SettingsPreferences.KEY_SOUND, checked));

        bindVibrateAtPrayer(view);

        SoundPreviewPlayer.Views ezanPreview = new SoundPreviewPlayer.Views(view.findViewById(R.id.ezanSoundPreview));
        SoundPreviewPlayer.Views duaPreview = new SoundPreviewPlayer.Views(view.findViewById(R.id.ezanDuaPreview));
        ezanPreview.button.setOnClickListener(v ->
                preview.toggle(PrayerSound.ezanRes(settings.getEzanSoundType()), ezanPreview));
        duaPreview.button.setOnClickListener(v ->
                preview.toggle(PrayerSound.duaRes(settings.getEzanDuaType()), duaPreview));
        SoundPreviewPlayer.bindIdle(ezanPreview, PrayerSound.ezanRes(settings.getEzanSoundType()) != 0);
        SoundPreviewPlayer.bindIdle(duaPreview, PrayerSound.duaRes(settings.getEzanDuaType()) != 0);

        bindDropdown(view.findViewById(R.id.ezanSoundDropdown), settings.getEzanSoundType(), position -> {
            settings.setEzanSoundType(position);
            preview.stop();
            SoundPreviewPlayer.bindIdle(ezanPreview, PrayerSound.ezanRes(position) != 0);
            rescheduleAlarms(); // ses alarmları seçime göre yeniden kurulur ("Kapalı" ise kurulmaz)
        });
        bindDropdown(view.findViewById(R.id.ezanDuaDropdown), settings.getEzanDuaType(), position -> {
            settings.setEzanDuaType(position);
            preview.stop();
            SoundPreviewPlayer.bindIdle(duaPreview, PrayerSound.duaRes(position) != 0);
            rescheduleAlarms();
        });
    }

    /** Namazda titreşime al + kaç dakika sonra eski profile dönüleceği. */
    private void bindVibrateAtPrayer(View view) {
        SettingsRowView rowVibrate = view.findViewById(R.id.rowVibrateAtPrayer);
        TextInputLayout minutesLayout = view.findViewById(R.id.silenceMinutesLayout);
        MaterialAutoCompleteTextView minutesDropdown = view.findViewById(R.id.silenceMinutesDropdown);

        rowVibrate.setChecked(settings.isVakitSoundModeOn());
        rowVibrate.setSubtitle(getString(R.string.settings_vibrate_subtitle, settings.getSilenceMinutes()));
        minutesLayout.setEnabled(settings.isVakitSoundModeOn());
        rowVibrate.setOnToggleListener((row, checked) -> {
            settings.setVakitSoundMode(checked);
            minutesLayout.setEnabled(checked);
            rescheduleAlarms(); // titreşim vakit anı alarmıyla tetiklenir
        });

        String[] labels = new String[SILENCE_MINUTE_OPTIONS.length];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = getString(R.string.settings_minutes_after, SILENCE_MINUTE_OPTIONS[i]);
        }
        minutesDropdown.setSimpleItems(labels);
        minutesDropdown.setText(getString(R.string.settings_minutes_after, settings.getSilenceMinutes()), false);
        minutesDropdown.setOnItemClickListener((parent, v, position, id) -> {
            settings.setSilenceMinutes(SILENCE_MINUTE_OPTIONS[position]);
            rowVibrate.setSubtitle(getString(R.string.settings_vibrate_subtitle, SILENCE_MINUTE_OPTIONS[position]));
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        // Ekrandan çıkınca (başka ekran, ana ekran tuşu) önizleme susar
        if (preview != null) preview.stop();
    }

    @Override
    public void onDestroyView() {
        if (preview != null) {
            preview.release();
            preview = null;
        }
        super.onDestroyView();
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
