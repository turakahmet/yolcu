package com.islamyolu.app.ibadet;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.islamyolu.app.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IbadetArapcaContentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ibadet_card_list);
        loadContent();

    }

    private void loadContent() {
        // Intent'ten içerik tipini al
        // Intent'ten verileri al
        Intent intent = getIntent();
        String context = intent.getStringExtra("CONTENT_CONTEXT");
        String title = intent.getStringExtra("CONTENT_TITLE");
        List<IbadetCardLayout> tumKartlar = parseCardContent(context);
        RecyclerView recyclerView = findViewById(R.id.ibadetCardList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        CardAdapter adapter = new CardAdapter(this, tumKartlar);
        recyclerView.setAdapter(adapter);

        setTitle(title);
    }

    public List<IbadetCardLayout> parseCardContent(String input) {
        List<IbadetCardLayout> cardList = new ArrayList<>();
        Pattern cardPattern = Pattern.compile("<card>(.*?)</card>", Pattern.DOTALL);
        Matcher matcher = cardPattern.matcher(input);

        while (matcher.find()) {
            String cardContent = Objects.requireNonNull(matcher.group(1)).trim();

            String imageUrl = extractValue(cardContent, "Görsel:");
            String baslik = extractValue(cardContent, "Başlık:");
            String okunus = extractValue(cardContent, "Türkçe Okunuşu:");
            String anlam = extractValue(cardContent, "Anlamı:");

            cardList.add(new IbadetCardLayout(imageUrl, baslik, okunus, anlam));
        }

        return cardList;
    }

    private String extractValue(String content, String key) {
        Pattern p = Pattern.compile(key + "\\s*(.*?)(?=\\n[A-ZĞÜİŞÖÇ]|$)", Pattern.DOTALL);
        Matcher m = p.matcher(content);
        return m.find() ? Objects.requireNonNull(m.group(1)).trim() : "";
    }

}