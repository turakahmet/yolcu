package com.myapp.yolcu.ui.ibadet;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.myapp.yolcu.R;
import com.myapp.yolcu.ui.Entity.Ibadet;

import java.util.List;

public class IbadetContentActivity extends AppCompatActivity {
    private static final String TAG = "IbadetContentActivity";
    private ImageView contentImage;
    private TextView contentText;
    private List<Ibadet> ibadetList;
    private Ibadet ibadetContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ibadet_content);
        Log.d(TAG, "İbadet içerik sayfası başlatılıyor...");

        initializeViews();
        loadContent();

    }

    private void initializeViews() {
        contentImage = findViewById(R.id.ibadetImageContent);
        contentText = findViewById(R.id.ibadetDetailContent);
    }

    private void loadContent() {
        // Intent'ten içerik tipini al
        // Intent'ten verileri al
        Intent intent = getIntent();
        String value = intent.getStringExtra("CONTENT_TYPE");

        if ("card2".equals(value)) {
            ibadetContent = new Ibadet().ibadetCardIkiListesiniAl(this);
            setTitle(ibadetContent.getBaslik());
            contentText.setText(ibadetContent.getAciklama1());
            contentImage.setImageResource(ibadetContent.getResimKaynak());
        }
    }

    public List<Ibadet> getIbadetList() {
        return ibadetList;
    }

    public void setIbadetList(List<Ibadet> ibadetList) {
        this.ibadetList = ibadetList;
    }

    public Ibadet getIbadetContent() {
        return ibadetContent;
    }

    public void setIbadetContent(Ibadet ibadetContent) {
        this.ibadetContent = ibadetContent;
    }
}