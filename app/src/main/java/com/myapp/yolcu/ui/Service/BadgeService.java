package com.myapp.yolcu.ui.Service;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.myapp.yolcu.ui.Rozet.Badge;
import com.myapp.yolcu.ui.Rozet.BadgeType;
import com.myapp.yolcu.ui.Service.SoundService;

import java.util.ArrayList;
import java.util.List;

public class BadgeService {

    private static final String PREFS_NAME = "BadgePreferences";
    private SharedPreferences sharedPreferences;
    private SoundService soundService;
    private Context context;
    private List<Badge> badges;


    public BadgeService(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        soundService=new SoundService(context);
        initializeBadges();
    }

    private void initializeBadges() {
        badges = new ArrayList<>();
        for (BadgeType badgeType : BadgeType.values()) {
            boolean isEnabled = sharedPreferences.getBoolean(badgeType.name(), false);
            badges.add(new Badge(badgeType, isEnabled));
        }
    }

    public List<Badge> getBadges() {
        return badges;
    }

    public void enableBadge(BadgeType badgeType) {
        for (Badge badge : badges) {
            if (badge.getBadgeType() == badgeType && !sharedPreferences.getBoolean(badge.getBadgeType().name(),false)) {
                badge.setEnabled(true);
                soundService.successRozet();
                showToast("Tebrikler, "+badgeType.getLabel()+" rozetini kazandınız.");
                // SharedPreferences'e kaydet
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(badgeType.name(), true);
                editor.apply();
                break;
            }
        }
    }

    // Kazanılan rozet sayısını döndüren metod
    public int getEnabledBadgeCount() {
        int rozetCount=0;
        for (BadgeType badgeType : BadgeType.values()) {
            boolean isEnabled = sharedPreferences.getBoolean(badgeType.name(), false);
            if(isEnabled)
                rozetCount+=1;
        }
        return rozetCount;
    }
    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}


