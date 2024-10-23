package com.myapp.yolcu.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.myapp.yolcu.R;
import com.myapp.yolcu.databinding.FragmentHomeBinding;
import com.myapp.yolcu.ui.game.InfoGameActivity;
import com.myapp.yolcu.ui.game.WordGameActivity;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // "Oyna" butonlarını bağlama
        Button playInfoGameButton = root.findViewById(R.id.play_info_game_button);
        Button playWordGameButton = root.findViewById(R.id.play_word_game_button);

        // Bilgi oyunu butonuna tıklama olayını tanımla
        playInfoGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Bilgi oyununu aç
                Intent intent = new Intent(getActivity(), InfoGameActivity.class);
                startActivity(intent);
            }
        });

        // Kelime oyunu butonuna tıklama olayını tanımla
        playWordGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Kelime oyununu aç
                Intent intent = new Intent(getActivity(), WordGameActivity.class);
                startActivity(intent);
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
