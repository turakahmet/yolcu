package com.islamyolu.app.ibadet;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.islamyolu.app.Entity.Ibadet;
import com.islamyolu.app.R;

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
        String context = intent.getStringExtra("CONTENT_CONTEXT");
        String title = intent.getStringExtra("CONTENT_TITLE");
        String image = intent.getStringExtra("CONTENT_IMAGE");

        Glide.with(this)
                .load(image)
                .thumbnail(Glide.with(this).load(R.drawable.loading))
                .error(R.drawable.baseline_image_not_supported_24) // Hata olursa gösterilecek resim
                .into(contentImage);

//            ibadetContent = new Ibadet().ibadetCardDetay(this, type);
//            contentImage.setImageResource(image);
        setTitle(title);
        contentText.setText(context);
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