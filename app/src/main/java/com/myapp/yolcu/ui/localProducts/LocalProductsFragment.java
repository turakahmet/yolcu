package com.myapp.yolcu.ui.localProducts;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.myapp.yolcu.R;
import com.myapp.yolcu.databinding.FragmentLocalProductsBinding;

public class LocalProductsFragment extends Fragment {
    private static final String TAG = "LocalProductsFragment";
    private static final String APP_MARKET_URL = "https://play.google.com/store/apps/details?id=com.myapp.boykot";

    private FragmentLocalProductsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Binding ile root view'i alıyoruz
        binding = FragmentLocalProductsBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        // Button'a click listener ekliyoruz
        binding.btnOpenApp.setOnClickListener(v -> openMarketApp());

        return view;
    }

    private void openMarketApp() {
        try {
            Log.d(TAG, "Opening market app: " + APP_MARKET_URL);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(APP_MARKET_URL));
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening market app", e);
        }
    }
}
