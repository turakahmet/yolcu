package com.islamyolu.app.Rozet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.islamyolu.app.Service.BadgeService;
import com.islamyolu.app.Service.SoundService;
import com.islamyolu.app.databinding.FragmentRozetBinding;

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
