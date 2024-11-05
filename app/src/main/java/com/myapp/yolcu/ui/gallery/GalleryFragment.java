package com.myapp.yolcu.ui.gallery;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.myapp.yolcu.databinding.FragmentGalleryBinding;
import com.myapp.yolcu.ui.game.SoundService;

public class GalleryFragment extends Fragment {
    private SoundService soundService;

    private FragmentGalleryBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        GalleryViewModel galleryViewModel =
                new ViewModelProvider(this).get(GalleryViewModel.class);

        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        initializeSoundService(binding.getRoot());
        soundService.commonButtonEffect();
        final TextView textView = binding.textGallery;
        galleryViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }
    private void initializeSoundService(ConstraintLayout root) {
        soundService = new SoundService(root.getContext());
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}