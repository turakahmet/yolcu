package com.islamyolu.app.main;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.islamyolu.app.R;


public class IntroFragment extends Fragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_DESCRIPTION = "description";
    private static final String ARG_IMAGE = "image";
    private static final String ARG_IS_LAST_PAGE = "isLastPage";

    public static IntroFragment newInstance(String title, String description, int imageResId, boolean isLastPage) {
        IntroFragment fragment = new IntroFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DESCRIPTION, description);
        args.putInt(ARG_IMAGE, imageResId);
        args.putBoolean(ARG_IS_LAST_PAGE, isLastPage);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.intro_first, container, false);

        TextView titleTextView = view.findViewById(R.id.introTitle);
        TextView descriptionTextView = view.findViewById(R.id.introDescription);
        ImageView imageView = view.findViewById(R.id.introImage);
        Button startButton = view.findViewById(R.id.introStartButton);

        if (getArguments() != null) {
            titleTextView.setText(getArguments().getString(ARG_TITLE));
            descriptionTextView.setText(getArguments().getString(ARG_DESCRIPTION));
            imageView.setImageResource(getArguments().getInt(ARG_IMAGE));

            // Eğer bu son sayfa ise butonu görünür yap
            boolean isLastPage = getArguments().getBoolean(ARG_IS_LAST_PAGE);
            if (isLastPage) {
                startButton.setVisibility(View.VISIBLE);
                startButton.setOnClickListener(v -> {
                    // Tanıtım tamamlandı olarak işaretle
                    SharedPreferences prefs = requireActivity().getSharedPreferences("IntroPrefs", Context.MODE_PRIVATE);
                    prefs.edit().putBoolean("isIntroCompleted", true).apply();

                    // MainActivity'ye geç
                    Intent intent = new Intent(requireContext(), MainActivity.class);
                    startActivity(intent);
                    requireActivity().finish();
                });
            }
        }

        return view;
    }
}



