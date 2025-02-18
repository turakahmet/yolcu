package com.myapp.yolcu.ui.Service;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.review.ReviewException;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.review.model.ReviewErrorCode;
import com.myapp.yolcu.ui.Rozet.Badge;
import com.myapp.yolcu.ui.Rozet.BadgeType;
import com.myapp.yolcu.ui.Service.SoundService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    public void showInAppReview(Activity activity) {
        // ReviewManager oluştur
        ReviewManager manager = ReviewManagerFactory.create(activity);

        // İnceleme bilgilerini talep et
        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Başarılı bir şekilde ReviewInfo alındı
                ReviewInfo reviewInfo = task.getResult();

                // İnceleme akışını başlat
                Task<Void> flow = manager.launchReviewFlow(activity, reviewInfo);
                flow.addOnCompleteListener(flowTask -> {
                    // İnceleme akışı tamamlandı
                    // Kullanıcının puanlama yapıp yapmadığı bilgisi API tarafından sağlanmaz
                    Log.d("InAppReview", "İnceleme akışı tamamlandı");
                });
            } else {
                // Hata durumunda işlem yap
                @ReviewErrorCode int reviewErrorCode = ((ReviewException) Objects.requireNonNull(task.getException())).getErrorCode();
                Log.e("InAppReview", "İnceleme isteği başarısız oldu. Hata kodu: " + reviewErrorCode);
            }
        });
    }

}


