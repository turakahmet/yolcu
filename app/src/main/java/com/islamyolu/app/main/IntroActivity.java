package com.islamyolu.app.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.islamyolu.app.R;
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator;

public class IntroActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "IntroPrefs";
    private static final String KEY_INTRO_COMPLETED = "isIntroCompleted";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isIntroCompleted = prefs.getBoolean(KEY_INTRO_COMPLETED, false);

        if (!isIntroCompleted) {
            // Tanıtım ekranını göster
            setContentView(R.layout.activity_intro);

            ViewPager2 viewPager = findViewById(R.id.viewPager);
            WormDotsIndicator wormDotsIndicator = findViewById(R.id.wormDotsIndicator);
            IntroPagerAdapter adapter = new IntroPagerAdapter(this);

            adapter.addFragment(IntroFragment.newInstance(getString(R.string.intro_1), getString(R.string.intro_1_desc), R.drawable.intro_1, false));
            adapter.addFragment(IntroFragment.newInstance(getString(R.string.intro_2), getString(R.string.intro_2_desc), R.drawable.intro_2, false));
            adapter.addFragment(IntroFragment.newInstance(getString(R.string.intro_3), getString(R.string.intro_3_desc), R.drawable.intro_3, false));
            adapter.addFragment(IntroFragment.newInstance(getString(R.string.intro_4), getString(R.string.intro_4_desc), R.drawable.intro_4, false));
            adapter.addFragment(IntroFragment.newInstance(getString(R.string.intro_5), getString(R.string.intro_5_desc), R.drawable.intro_5, false));
            adapter.addFragment(IntroFragment.newInstance(getString(R.string.intro_6), getString(R.string.intro_6_desc), R.drawable.intro_6, true));

            viewPager.setAdapter(adapter);
            wormDotsIndicator.attachTo(viewPager);
        } else {
            // Tanıtım tamamlandıysa doğrudan MainActivity'ye geç
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }
}


