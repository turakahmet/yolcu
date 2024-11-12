package com.myapp.yolcu.ui.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.myapp.yolcu.R;
import com.myapp.yolcu.databinding.FragmentHomeBinding;
import com.myapp.yolcu.ui.Service.BadgeService;
import com.myapp.yolcu.ui.Rozet.BadgeType;
import com.myapp.yolcu.ui.game.InfoGameActivity;
import com.myapp.yolcu.ui.Service.SoundService;
import com.myapp.yolcu.ui.game.WordGameActivity;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private SoundService soundService;
    private BadgeService badgeService;

    private final String PREFS_NAME = "GameJoinPreferences";
    private final String INFO_GAME_COUNT_KEY = "infoGameCount";
    private final String WORD_GAME_COUNT_KEY = "wordGameCount";
    private final int GAME_THRESHOLD = 10;  // 10 defa tıklama sonrası rozet için
    private SharedPreferences sharedPreferences;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        initializeServices(binding.getRoot());
        soundService.commonButtonEffect();

        // "Oyna" butonlarını bağlama
        Button playInfoGameButton = root.findViewById(R.id.play_info_game_button);
        Button playWordGameButton = root.findViewById(R.id.play_word_game_button);

        // Bilgi oyunu butonuna tıklama olayını tanımla
        playInfoGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                incrementGameCount(INFO_GAME_COUNT_KEY);

                // Bilgi oyununu aç
                soundService.playGameButtonEffect();
                Intent intent = new Intent(getActivity(), InfoGameActivity.class);
                startActivity(intent);
            }
        });

        // Kelime oyunu butonuna tıklama olayını tanımla
        playWordGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                incrementGameCount(WORD_GAME_COUNT_KEY);

                // Kelime oyununu aç
                soundService.playGameButtonEffect();
                Intent intent = new Intent(getActivity(), WordGameActivity.class);
                startActivity(intent);
            }
        });

        return root;
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
            if(INFO_GAME_COUNT_KEY.equals(key))
                badgeService.enableBadge(BadgeType.BILGI_YARISMA_5);
            else if(WORD_GAME_COUNT_KEY.equals(key))
                badgeService.enableBadge(BadgeType.KELIME_YARISMA_5);
        }
    }
    private void initializeServices(ConstraintLayout root) {
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        soundService = new SoundService(root.getContext());
        badgeService = new BadgeService(root.getContext());
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
