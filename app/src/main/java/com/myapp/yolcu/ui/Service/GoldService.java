package com.myapp.yolcu.ui.Service;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.myapp.yolcu.R;
import com.myapp.yolcu.ui.game.WordGameActivity;

import java.util.Objects;

public class GoldService {
    private static final String PREF_NAME = "GamePreferences";
    private static final String GOLD_KEY = "userGold";
    private static final int DEFAULT_GOLD = 5000;
    private SharedPreferences sharedPreferences;
    private SoundService soundService;


    public GoldService(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Altın miktarını kaydet
    public void saveGoldAmount(int userGold) {
        sharedPreferences.edit().putInt(GOLD_KEY, userGold).apply();
    }

    // Kayıtlı altın miktarını getir
    public int getGoldAmount() {
        return sharedPreferences.getInt(GOLD_KEY, DEFAULT_GOLD);
    }

    // Altın ekle
    public void addGoldAmount(int goldAmount) {
        int currentGold = getGoldAmount();
        int newGoldAmount = currentGold + goldAmount;
        saveGoldAmount(newGoldAmount);
    }

    // Altın harca
    public void minusGoldAmount(int goldAmount) {
        int currentGold = getGoldAmount();
        int newGoldAmount = currentGold - goldAmount;
        saveGoldAmount(newGoldAmount);
    }

    // Altın miktarını sıfırlamak için başlangıç miktarını ayarla
    public void setInitialGoldAmount() {
        if (sharedPreferences.getInt(GOLD_KEY, -1) == -1) {
            saveGoldAmount(DEFAULT_GOLD); // Varsayılan olarak 5000 altın ver
        }
    }

    public void yetersizKaynakDialog(Context context) {
        soundService = new SoundService(context);
        Dialog mDialog = new Dialog(context);
        mDialog.setContentView(R.layout.yetersiz_kaynak_popup);
        Objects.requireNonNull(mDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mDialog.setCancelable(false);
        // Kapat butonunu bul ve tıklama olayını ayarla
        Button closeButton = mDialog.findViewById(R.id.closeButtonYetersizKaynak);
        closeButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                soundService.commonButtonEffect();
                mDialog.dismiss();
            }
        });
        mDialog.show();
    }
}


