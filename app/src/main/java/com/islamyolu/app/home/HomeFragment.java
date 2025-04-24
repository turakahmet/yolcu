package com.islamyolu.app.home;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.islamyolu.app.Entity.SoruTip;
import com.islamyolu.app.R;
import com.islamyolu.app.Rozet.BadgeType;
import com.islamyolu.app.Service.BadgeService;
import com.islamyolu.app.Service.SoundService;
import com.islamyolu.app.databinding.FragmentHomeBinding;
import com.islamyolu.app.game.GenericGameActivity;
import com.islamyolu.app.game.InfoGameActivity;
import com.islamyolu.app.game.MatchesGameActivity;
import com.islamyolu.app.game.WordGameActivity;

import java.util.Objects;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private SoundService soundService;
    private BadgeService badgeService;
    private Dialog mDialog;

    private final String PREFS_NAME = "GameJoinPreferences";
    private final String INFO_GAME_COUNT_KEY = "infoGameCount";
    private final String WORD_GAME_COUNT_KEY = "wordGameCount";
    private final String MATCHES_GAME_COUNT_KEY = "matchesGameCount";
    private final String KURAN_GAME_COUNT_KEY = "kuranGameCount";
    private final String SIYER_GAME_COUNT_KEY = "siyerGameCount";
    private final String PEYGAMBERLER_GAME_COUNT_KEY = "peygamberlerGameCount";
    private final String ISLAMTARIHI_GAME_COUNT_KEY = "islamTarihiGameCount";
    private final int GAME_THRESHOLD = 10;  // 10 defa tıklama sonrası rozet için
    private SharedPreferences sharedPreferences;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        initializeServices(binding.getRoot());
        soundService.commonButtonEffect();

        // "Oyna" butonlarını bağlama
        Button playInfoGameButton = root.findViewById(R.id.play_info_game_button);
        Button playWordGameButton = root.findViewById(R.id.play_word_game_button);
        Button playMatchesGameButton = root.findViewById(R.id.play_matches_game_button);
        Button playYarismaKuran = root.findViewById(R.id.play_yarisma_kuran_button);
        Button playYarismaSiyerButton = root.findViewById(R.id.play_yarisma_siyer_button);
        Button playPeygamberlerTarihiButton = root.findViewById(R.id.play_peygamberler_tarihi_button);
        Button playIslamTarihiButton = root.findViewById(R.id.play_islam_tarihi_button);

        // Bilgi oyunu butonuna tıklama olayını tanımla
        playInfoGameButton.setOnClickListener(v -> {
            incrementGameCount(INFO_GAME_COUNT_KEY);
            if (!isNetworkAvailable(requireContext())) {
                showHintPopup();
                return;
            }
            // Bilgi oyununu aç
            soundService.playGameButtonEffect();
            Intent intent = new Intent(getActivity(), InfoGameActivity.class);
            startActivity(intent);
        });

        // Kelime oyunu butonuna tıklama olayını tanımla
        playWordGameButton.setOnClickListener(v -> {
            incrementGameCount(WORD_GAME_COUNT_KEY);
            if (!isNetworkAvailable(requireContext())) {
                showHintPopup();
                return;
            }
            // Kelime oyununu aç
            soundService.playGameButtonEffect();
            Intent intent = new Intent(getActivity(), WordGameActivity.class);
            startActivity(intent);
        });

        // Esleme oyunu butonuna tıklama olayını tanımla
        playMatchesGameButton.setOnClickListener(v -> {
            incrementGameCount(MATCHES_GAME_COUNT_KEY);
            if (!isNetworkAvailable(requireContext())) {
                showHintPopup();
                return;
            }
            // Kelime oyununu aç
            soundService.playGameButtonEffect();
            Intent intent = new Intent(getActivity(), MatchesGameActivity.class);
            startActivity(intent);
        });


        playYarismaKuran.setOnClickListener(v -> {
            incrementGameCount(KURAN_GAME_COUNT_KEY);
            if (!isNetworkAvailable(requireContext())) {
                showHintPopup();
                return;
            }
            showGameSettigPopup(SoruTip.KURAN.name());

        });


        playYarismaSiyerButton.setOnClickListener(v -> {
            incrementGameCount(SIYER_GAME_COUNT_KEY);
            if (!isNetworkAvailable(requireContext())) {
                showHintPopup();
                return;
            }
            showGameSettigPopup(SoruTip.SIYER.name());

        });


        playPeygamberlerTarihiButton.setOnClickListener(v -> {
            incrementGameCount(PEYGAMBERLER_GAME_COUNT_KEY);
            if (!isNetworkAvailable(requireContext())) {
                showHintPopup();
                return;
            }
            showGameSettigPopup(SoruTip.PEYGAMBERLER_TARIHI.name());

        });

        playIslamTarihiButton.setOnClickListener(v -> {
            incrementGameCount(ISLAMTARIHI_GAME_COUNT_KEY);
            if (!isNetworkAvailable(requireContext())) {
                showHintPopup();
                return;
            }
            showGameSettigPopup(SoruTip.ISLAM_TARIHI.name());

        });

        return root;
    }

    private void showGameSettigPopup(String soruTip) {
        // Yeni bir Dialog oluştur
        mDialog = new Dialog(requireContext());
        Objects.requireNonNull(mDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        // custom_popup.xml layoutunu dialog'a yükle
        mDialog.setContentView(R.layout.game_setting_popup);
        // Dialog dışına tıklanıldığında kapansın
        mDialog.setCancelable(false);
        // Kapat butonunu bul ve tıklama olayını ayarla
        Button closeButton = mDialog.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(view -> {
            soundService.commonButtonEffect();
            mDialog.dismiss();
        });

        Button btnBasla = mDialog.findViewById(R.id.btnBasla);
        SeekBar seekBarSeviye = mDialog.findViewById(R.id.seekBarSeviye);
        TextView txtSeviye = mDialog.findViewById(R.id.txtSeviye);
        RadioGroup radioGroupSure = mDialog.findViewById(R.id.radioGroupSure);
        // Seviye kontrolü
        seekBarSeviye.setMax(1); // 0: Normal, 1: Zor
        seekBarSeviye.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress == 0) {
                    txtSeviye.setText("Seviye: Normal");
                } else {
                    txtSeviye.setText("Seviye: Zor");
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });


        btnBasla.setOnClickListener(view -> {
            int sure = 60; // default
            int selectedId = radioGroupSure.getCheckedRadioButtonId();
            if (selectedId == R.id.radio120) {
                sure = 120;
            } else if (selectedId == R.id.radio180) {
                sure = 180;
            }
            int seviye = seekBarSeviye.getProgress() == 0 ? 0 : 1;

            Intent intent = new Intent(getActivity(), GenericGameActivity.class);
            intent.putExtra("gameType", soruTip);
            intent.putExtra("sure", sure);
            intent.putExtra("seviye", seviye);
            startActivity(intent);
            soundService.playGameButtonEffect();
            mDialog.dismiss();
        });

        // Dialog'u göster
        mDialog.show();
    }

    private void incrementGameCount(String key) {
        // Tıklama sayısını artır
        int currentCount = sharedPreferences.getInt(key, 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, currentCount + 1);
        editor.apply();
        checkAndAwardBadge(key);
    }

    private void checkAndAwardBadge(String key) {
        // Rozet verilmesi gereken durumu kontrol et
        int currentCount = sharedPreferences.getInt(key, 0);
        if (currentCount == GAME_THRESHOLD) {
            if (INFO_GAME_COUNT_KEY.equals(key))
                badgeService.enableBadge(BadgeType.BILGI_YARISMA_5);
            else if (WORD_GAME_COUNT_KEY.equals(key))
                badgeService.enableBadge(BadgeType.KELIME_YARISMA_5);
            else if (KURAN_GAME_COUNT_KEY.equals(key))
                badgeService.enableBadge(BadgeType.KURAN_YARISMA_3);
            else if (SIYER_GAME_COUNT_KEY.equals(key))
                badgeService.enableBadge(BadgeType.SIYER_YARISMA_3);
            else if (PEYGAMBERLER_GAME_COUNT_KEY.equals(key))
                badgeService.enableBadge(BadgeType.PEYGAMBERLER_YARISMA_3);
            else if (ISLAMTARIHI_GAME_COUNT_KEY.equals(key))
                badgeService.enableBadge(BadgeType.ISLAMTARIHI_YARISMA_3);
        }
    }

    private void initializeServices(ConstraintLayout root) {
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        soundService = new SoundService(root.getContext());
        badgeService = new BadgeService(root.getContext());
    }

    // İnternet bağlantısı kontrolü için yardımcı metod
    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkCapabilities capabilities = connectivityManager
                    .getNetworkCapabilities(connectivityManager.getActiveNetwork());

            if (capabilities != null) {
                return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            }
        }
        return false;
    }


    private void showHintPopup() {
        // Yeni bir Dialog oluştur
        mDialog = new Dialog(requireContext());
        Objects.requireNonNull(mDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        // custom_popup.xml layoutunu dialog'a yükle
        mDialog.setContentView(R.layout.custom_popup);
        // Dialog dışına tıklanıldığında kapansın
        mDialog.setCancelable(true);
        // Kapat butonunu bul ve tıklama olayını ayarla
        Button closeButton = mDialog.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(view -> {
            soundService.commonButtonEffect();
            mDialog.dismiss();
        });
        //hint text set
        TextView hitpouptext = mDialog.findViewById(R.id.hintPopUpText);
        hitpouptext.setText(R.string.no_Internet);
        // Dialog'u göster
        mDialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
