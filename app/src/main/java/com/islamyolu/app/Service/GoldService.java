package com.islamyolu.app.Service;

import com.islamyolu.app.sound.AppSound;
import com.islamyolu.app.sound.SoundManager;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.Button;

import com.islamyolu.app.R;
import com.islamyolu.app.preferences.AppPreferences;
import com.islamyolu.app.preferences.GamePreferences;

import java.util.Objects;

public class GoldService {
    private static final int DEFAULT_GOLD = 1000;
    private final GamePreferences gamePrefs;
    private SoundManager sounds;


    public GoldService(Context context) {
        gamePrefs = AppPreferences.from(context).game();
    }

    // Altın miktarını kaydet
    public void saveGoldAmount(int userGold) {
        gamePrefs.setGold(userGold);
    }

    // Kayıtlı altın miktarını getir
    public int getGoldAmount() {
        return gamePrefs.getGold(DEFAULT_GOLD);
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
        gamePrefs.initGoldIfMissing(DEFAULT_GOLD);
    }

    public void yetersizKaynakDialog(Context context) {
        sounds = SoundManager.from(context);
        Dialog mDialog = new Dialog(context);
        mDialog.setContentView(R.layout.yetersiz_kaynak_popup);
        Objects.requireNonNull(mDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mDialog.setCancelable(false);
        // Kapat butonunu bul ve tıklama olayını ayarla
        Button closeButton = mDialog.findViewById(R.id.closeButtonYetersizKaynak);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sounds.play(AppSound.COMMON_BUTTON);
                mDialog.dismiss();
            }
        });
        mDialog.show();
    }
}


