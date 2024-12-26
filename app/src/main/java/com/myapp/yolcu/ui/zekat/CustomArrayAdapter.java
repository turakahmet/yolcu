package com.myapp.yolcu.ui.zekat;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.myapp.yolcu.R;

import java.util.List;

public class CustomArrayAdapter extends ArrayAdapter<String> {

    private final Context context;
    private final List<String> items;
    private final ZekatHesaplaFragment fragment;

    public CustomArrayAdapter(@NonNull Context context, @NonNull List<String> objects, ZekatHesaplaFragment fragment) {
        super(context, android.R.layout.simple_list_item_1, objects);
        this.context = context;
        this.items = objects;
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Dinamik layout oluşturma
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(16, 16, 16, 16);

        // TextView oluşturma
        TextView textView = new TextView(context);
        textView.setText(items.get(position));
        textView.setTextSize(18);
        textView.setTypeface(textView.getTypeface(), Typeface.BOLD);
        textView.setTextColor(ContextCompat.getColor(context, R.color.black));
        textView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        // ImageButton oluşturma
        ImageButton imageButtonSil = new ImageButton(context);
        imageButtonSil.setImageResource(R.drawable.baseline_delete_24); // Silme simgesini buraya ekleyin
        imageButtonSil.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent)); // Arka planı şeffaf yapma
        imageButtonSil.setOnClickListener(v -> {
            // Öğeyi listeden kaldır
            items.remove(position);
            notifyDataSetChanged(); // Listeyi güncelle
            fragment.calculateZekat(); // calculateZekat() metodunu çağır
        });

// View'leri layout'a ekleme
        layout.addView(textView);
        layout.addView(imageButtonSil);


        // Arka plan rengi ayarlama
        int backgroundColor = (position % 2 == 0)
                ? ContextCompat.getColor(context, R.color.lightGreenColor)  // Özel yeşil ton
                : ContextCompat.getColor(context, R.color.lightGrayColor);  // Açık gri ton

        if(textView.getText().toString().contains("?0")){
            backgroundColor=ContextCompat.getColor(context, R.color.pastelRedColor);
            textView.setText(textView.getText().toString().replaceAll("\\?0$", ""));
        }else if(textView.getText().toString().contains("?1")){
            textView.setText(textView.getText().toString().replaceAll("\\?1$", ""));
        }
        layout.setBackgroundColor(backgroundColor);

        return layout;
    }
}
