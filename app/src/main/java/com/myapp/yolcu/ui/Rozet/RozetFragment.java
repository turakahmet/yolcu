package com.myapp.yolcu.ui.Rozet;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.myapp.yolcu.R;
import com.myapp.yolcu.databinding.FragmentRozetBinding;
import com.myapp.yolcu.ui.game.SoundService;

import java.util.ArrayList;
import java.util.List;

public class RozetFragment extends Fragment {

    private FragmentRozetBinding binding;
    private SoundService soundService;
    private BadgeService badgeService;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRozetBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        initializeSoundService(root);

        // BadgeService'i başlat ve tüm rozetleri al
        badgeService = new BadgeService(binding.getRoot().getContext());
        List<Badge> allBadges = badgeService.getBadges();

        // RecyclerView ve adapteri ayarla
        BadgeAdapter badgeAdapter = new BadgeAdapter(allBadges);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(binding.getRoot().getContext()));
        binding.recyclerView.setAdapter(badgeAdapter);

        return root;
    }
    // Başarı kazanıldığında bu metodu çağırabilirsiniz

    private void initializeSoundService(View root) {
        soundService = new SoundService(root.getContext());
        soundService.commonButtonEffect();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
