package com.myapp.yolcu.ui.Service;

import android.content.Context;
import android.content.SharedPreferences;

public class GoldService {
    private static final String PREF_NAME = "GamePreferences";
    private static final String GOLD_KEY = "userGold";
    private static final int DEFAULT_GOLD = 5000;
    private SharedPreferences sharedPreferences;

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
}


