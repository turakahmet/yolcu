package com.islamyolu.app.muze;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.islamyolu.app.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MuzeDetailActivity extends AppCompatActivity {
    private ViewPager2 imageSlider;
    private Button btnShowDetails;
    private Dialog mDialog;
    private TextView textHeader, textBody;
    private ImageView leftArrow, rightArrow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.digital_muze_detail);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        imageSlider = findViewById(R.id.imageSlider);
        btnShowDetails = findViewById(R.id.btnShowDetails);
        leftArrow = findViewById(R.id.leftArrow);
        rightArrow = findViewById(R.id.rightArrow);
        // Intent ile gelen verileri al
        String baslik = getIntent().getStringExtra("baslik");
        String aciklama = getIntent().getStringExtra("aciklama");
        int resimKaynak = getIntent().getIntExtra("kapakResim", 0);
        List<Integer> gorselList = getIntent().getIntegerArrayListExtra("gorselList");

        // Slider resimlerini oluştur
        List<Integer> imageList = new ArrayList<>();
        imageList.add(resimKaynak); // Ana resim
        imageList.addAll(gorselList);


        SliderAdapter sliderAdapter = new SliderAdapter(imageList);
        imageSlider.setAdapter(sliderAdapter);

        // Detay gösterme butonu
        btnShowDetails.setOnClickListener(v -> {
            showInfoPopup(baslik, aciklama);
        });

        // ViewPager2 ile ok simgelerinin kontrolü
        imageSlider.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // İlk sayfa ve son sayfa durumunu kontrol et
                if (position == 0) {
                    leftArrow.setVisibility(View.INVISIBLE); // İlk sayfada sol ok gizlensin
                } else {
                    leftArrow.setVisibility(View.VISIBLE);
                }

                if (position == sliderAdapter.getItemCount() - 1) {
                    rightArrow.setVisibility(View.INVISIBLE); // Son sayfada sağ ok gizlensin
                } else {
                    rightArrow.setVisibility(View.VISIBLE);
                }
            }
        });

        // Sol ok tıklama
        leftArrow.setOnClickListener(v -> {
            int currentItem = imageSlider.getCurrentItem();
            if (currentItem > 0) {
                imageSlider.setCurrentItem(currentItem - 1, true);
            }
        });

        // Sağ ok tıklama
        rightArrow.setOnClickListener(v -> {
            int currentItem = imageSlider.getCurrentItem();
            if (currentItem < sliderAdapter.getItemCount() - 1) {
                imageSlider.setCurrentItem(currentItem + 1, true);
            }
        });
    }


    private void showInfoPopup(String baslik, String aciklama) {
        // Yeni bir Dialog oluştur
        mDialog = new Dialog(this);
        Objects.requireNonNull(mDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        // custom_popup.xml layoutunu dialog'a yükle
        mDialog.setContentView(R.layout.muze_bilgi_popup);
        // Dialog dışına tıklanıldığında kapansın
        mDialog.setCancelable(true);
        Button closeButton = mDialog.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(view -> mDialog.dismiss());

        textHeader = mDialog.findViewById(R.id.textHeader);
        textBody = mDialog.findViewById(R.id.textBody);

        textHeader.setText(baslik);
        textBody.setText(aciklama);
        mDialog.show();
    }
}

