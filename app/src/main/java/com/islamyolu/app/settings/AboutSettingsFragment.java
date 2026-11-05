package com.islamyolu.app.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.islamyolu.app.R;
import com.islamyolu.app.feedback.BilgilendirmeActivity;
import com.islamyolu.app.feedback.FeedbackActivity;
import com.islamyolu.app.feedback.PrivacyActivity;

/** İletişim, gizlilik politikası, sorumluluk reddi ve sürüm. */
public class AboutSettingsFragment extends Fragment {

    public AboutSettingsFragment() {
        super(R.layout.fragment_settings_about);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        openOnClick(view.findViewById(R.id.rowContact), FeedbackActivity.class);
        openOnClick(view.findViewById(R.id.rowPrivacy), PrivacyActivity.class);
        openOnClick(view.findViewById(R.id.rowDisclaimer), BilgilendirmeActivity.class);
    }

    private void openOnClick(View row, Class<?> activity) {
        row.setOnClickListener(v -> startActivity(new Intent(requireContext(), activity)));
    }
}
