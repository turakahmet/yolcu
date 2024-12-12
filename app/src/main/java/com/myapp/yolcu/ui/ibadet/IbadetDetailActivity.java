package com.myapp.yolcu.ui.ibadet;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.myapp.yolcu.R;

public class IbadetDetailActivity extends AppCompatActivity {

    private TextView ibadetAciklama2;
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ibadet);
        // ActionBar'ı gizle
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        ImageView ibadetImage = findViewById(R.id.ibadetImage);
        TextView ibadetHeader = findViewById(R.id.ibadetHeader);
        TextView ibadetAciklama1 = findViewById(R.id.ibadetAciklama1);
        TextView aciklamaKaynak = findViewById(R.id.ibadetAciklama1_kaynak);
        ibadetAciklama2 = findViewById(R.id.ibadetAciklama2);

        // Intent'ten verileri al
        Intent intent = getIntent();
        String baslik = intent.getStringExtra("baslik");
        String aciklama1 = intent.getStringExtra("aciklama1");
        String kaynak = intent.getStringExtra("kaynak");
        String aciklama2 = intent.getStringExtra("aciklama2");
        int resimKaynak = intent.getIntExtra("resimKaynak",0);

        // Verileri ilgili görünümlere ayarla
        ibadetHeader.setText(baslik);
        ibadetAciklama1.setText(aciklama1);
        aciklamaKaynak.setText(kaynak);
        ibadetAciklama2.setText(aciklama2);
        ibadetImage.setImageResource(resimKaynak);

    }

}

