package com.islamyolu.app.theme;

import android.content.res.Configuration;
import android.graphics.Color;
import android.view.View;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.islamyolu.app.R;

/**
 * Kenardan kenara (edge-to-edge) görünüm ve sistem çubuğu boşluklarının TEK yönetim noktası.
 * <p>
 * Android 15+ (targetSdk 35+) bu görünümü zorunlu kılar: temadaki durum çubuğu rengi yok sayılır.
 * Yeşil başlık durum çubuğunun arkasına uzanır, içerik başlığın ve gezinme çubuğunun altında kalmaz.
 * Başlık her iki temada koyu yeşil olduğundan durum çubuğu ikonları her zaman beyazdır.
 */
public final class SystemBars {

    private SystemBars() {
    }

    /** setContentView'dan önce çağrılır. */
    public static void enable(@NonNull ComponentActivity activity) {
        // Gezinme çubuğu: kaydırılan içerik altından okunur kalsın diye yarı saydam, tema renginde
        // zemin. Bazı üretici arayüzleri sistemin kendi karartmasını uygulamadığı için açıkça verilir.
        int navScrim = ContextCompat.getColor(activity, R.color.color_nav_bar_scrim);
        boolean night = (activity.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        EdgeToEdge.enable(activity,
                SystemBarStyle.dark(Color.TRANSPARENT),          // yeşil başlık üstünde beyaz ikonlar
                night ? SystemBarStyle.dark(navScrim) : SystemBarStyle.light(navScrim, navScrim));
    }

    /** Başlık: üstte durum çubuğu/kamera kesiği, yanlarda (yatay ekran) güvenli alan kadar iç boşluk. */
    public static void padHeader(@NonNull View header) {
        applyPadding(header, true, false);
    }

    /** İçerik: altta gezinme çubuğu, yanlarda güvenli alan kadar iç boşluk. */
    public static void padContent(@NonNull View content) {
        applyPadding(content, false, true);
    }

    private static void applyPadding(View view, boolean top, boolean bottom) {
        // Düzende verilmiş iç boşluk korunur; boşluk her yeni değerde baştan hesaplanır
        int left = view.getPaddingLeft();
        int topPad = view.getPaddingTop();
        int right = view.getPaddingRight();
        int bottomPad = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(left + bars.left,
                    topPad + (top ? bars.top : 0),
                    right + bars.right,
                    bottomPad + (bottom ? bars.bottom : 0));
            return windowInsets;
        });
    }
}
