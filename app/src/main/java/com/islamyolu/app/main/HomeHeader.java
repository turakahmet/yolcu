package com.islamyolu.app.main;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.islamyolu.app.R;

/**
 * MainActivity'nin ortak üst bölümünü ekrana göre düzenler (app_bar_main).
 * <ul>
 *   <li>Ana Sayfa: cami manzaralı büyük başlık — caminin solundaki açık alanda gölgeli etiket ve
 *       saat / dakika / saniye cam kutuları; altında cam vakit şeridi. Başlık yazısı gösterilmez (manzara öne çıksın).
 *       İçerik kaydırılınca toolbar ve sayaç yukarı çekilir, vakit şeridi üstte sabit kalır.</li>
 *   <li>Diğer ekranlar: ince çubuk — ekran adı ve küçük geri sayım; kaydırma yok.</li>
 * </ul>
 * Vakit hesapları ve vurgular PrayerTimesPanel'dedir; bu sınıf yalnızca görünümü değiştirir.
 */
final class HomeHeader {

    private static final String TAG = "HomeHeader";

    /**
     * Ana Sayfa: toolbar ve sayaç içerikle yukarı kayar; vakit şeridi (bayraksız) üstte sabit kalır.
     * enterAlways yok: aşağı inerken önce içerik gelir, en üste varınca başlık açılır.
     */
    private static final int EXPANDED_FLAGS = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
            | AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP;

    /** Görselde ortada tutulacak dikey konum: cami ve ağaç çizgisi */
    private static final float PHOTO_FOCUS_Y = 0.45f;
    /** Başlık fotoğrafının alttan zemine solan bölümü */
    private static final float PHOTO_FADE_RATIO = 0.25f;
    /**
     * Ana Sayfa başlığının (toolbar üstünden vakit şeridine kadar) pencere genişliğine oranı.
     * Cihaza veya genişlik sınırına göre sabit boşluk yerine bu oran kullanılır: dar ve geniş
     * telefonlarda başlık ve cami fotoğrafı aynı oranda görünür.
     */
    private static final float HOME_HEADER_WIDTH_RATIO = 0.42f;

    private final AppBarLayout appBar;
    private final Toolbar toolbar;
    private final View[] scrollingChildren;
    /** Geri sayım alanı */
    private final View panel;
    private final View[] homeOnly;
    private final TextView label;
    /** Vakit etiketinin üstündeki kerahat uyarısı (etiketten küçük) */
    private final TextView kerahatLabel;
    /** Diğer ekranlardaki tek satır sayaç (Ana Sayfa'da kutular kullanılır) */
    private final TextView countdown;

    private final int homeTopPadding;
    private final int compactPadding;
    /** Ana Sayfa etiket boyutu (px); dar pencerede daha küçük */
    private final float labelSize;
    /** Ana Sayfa kerahat uyarısı boyutu (px) */
    private final float kerahatSize;

    /** Diğer ekranlardaki ince çubuğun degrade zemini */
    private final Drawable compactBackground;
    /** Ana Sayfa'nın cami manzaralı zemini; görsel açılamazsa degrade kullanılır */
    @Nullable
    private final Drawable homeBackground;
    /** Sayaç paneli: Ana Sayfa'da başlık kapandıkça solar */
    private final View countdownPanels;
    /** Sayaç ile vakit şeridi arasındaki cami boşluğu; yüksekliği genişliğe oranla hesaplanır */
    private final View stripSpacer;
    private final int stripSpacerMin;
    private boolean home;

    HomeHeader(@NonNull Activity activity, @NonNull Toolbar toolbar) {
        this.toolbar = toolbar;
        appBar = activity.findViewById(R.id.app_bar);
        countdownPanels = activity.findViewById(R.id.home_header_panels);
        scrollingChildren = new View[]{toolbar, countdownPanels};
        panel = activity.findViewById(R.id.remain_time_layout);
        homeOnly = new View[]{
                // Sağdaki boşluk: sayaç caminin solunda kalsın
                activity.findViewById(R.id.home_countdown_spacer),
                activity.findViewById(R.id.home_countdown_line_start),
                activity.findViewById(R.id.home_countdown_icon),
                activity.findViewById(R.id.home_countdown_line_end),
                activity.findViewById(R.id.home_countdown_boxes),
                activity.findViewById(R.id.home_strip_spacer),
                activity.findViewById(R.id.prayer_strip)
        };
        stripSpacer = activity.findViewById(R.id.home_strip_spacer);
        label = activity.findViewById(R.id.toolbarKalanSureLabel);
        kerahatLabel = activity.findViewById(R.id.home_countdown_kerahat);
        countdown = activity.findViewById(R.id.toolbarKalanSure);

        Resources res = activity.getResources();
        homeTopPadding = res.getDimensionPixelSize(R.dimen.home_countdown_top_padding);
        compactPadding = res.getDimensionPixelSize(R.dimen.home_header_compact_padding);
        labelSize = res.getDimension(R.dimen.home_countdown_label_text);
        kerahatSize = res.getDimension(R.dimen.home_countdown_kerahat_text);
        stripSpacerMin = res.getDimensionPixelSize(R.dimen.home_strip_spacer_min);

        setCaption(activity, R.id.home_countdown_hours, R.string.countdown_unit_hours);
        setCaption(activity, R.id.home_countdown_minutes, R.string.countdown_unit_minutes);
        setCaption(activity, R.id.home_countdown_seconds, R.string.countdown_unit_seconds);

        compactBackground = AppCompatResources.getDrawable(activity, R.drawable.bg_app_header);
        homeBackground = createHomeBackground(activity);

        appBar.addOnOffsetChangedListener((bar, verticalOffset) -> fadeCountdown(verticalOffset));
        // Pencere genişliği (döndürme, bölünmüş ekran) veya sayaç yüksekliği (kerahat satırı
        // açılıp kapanınca) değişince cami boşluğu yeniden hesaplanır
        panel.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (r - l != or - ol || b - t != ob - ot) {
                panel.post(this::updateStripSpacer);
            }
        });
    }

    /**
     * Başlık yüksekliği = pencere genişliği × {@link #HOME_HEADER_WIDTH_RATIO}. Toolbar ve sayaçtan
     * artan kısım cami boşluğuna verilir; dar ekranda sayaç uzunsa en az {@code stripSpacerMin} kalır.
     */
    private void updateStripSpacer() {
        int width = appBar.getWidth();
        if (width == 0) {
            return;
        }
        int target = Math.round(width * HOME_HEADER_WIDTH_RATIO);
        int used = toolbar.getHeight() + panel.getHeight();
        int height = Math.max(stripSpacerMin, target - used);
        ViewGroup.LayoutParams params = stripSpacer.getLayoutParams();
        if (params.height != height) {
            params.height = height;
            stripSpacer.setLayoutParams(params);
        }
    }

    /**
     * Ana Sayfa'da başlık kapandıkça sayaç paneli solar. AppBarLayout tam kapalıyken durum çubuğu
     * yüksekliği kadar alanı açık tutar; panel saydam olduğu için orada sayaç kalıntısı görünmez,
     * yalnızca cami fotoğrafı kalır. Diğer ekranlarda panel kaymaz, hep tam görünür.
     */
    private void fadeCountdown(int verticalOffset) {
        int range = appBar.getTotalScrollRange();
        float collapsed = home && range > 0 ? Math.min(1f, -verticalOffset / (float) range) : 0f;
        countdownPanels.setAlpha(1f - collapsed);
    }

    /** Kutunun altındaki birim yazısı (SAAT / DAKİKA / SANİYE) */
    private static void setCaption(@NonNull Activity activity, int boxId, int textRes) {
        TextView caption = activity.findViewById(boxId).findViewById(R.id.countdown_box_caption);
        caption.setText(textRes);
    }

    @Nullable
    private static Drawable createHomeBackground(@NonNull Activity activity) {
        Resources res = activity.getResources();
        // drawable-nodpi: görsel ölçeklenmeden bir kez açılır (~120 KB WebP)
        Bitmap photo = BitmapFactory.decodeResource(res, R.drawable.bg_home_header_mosque);
        if (photo == null) {
            Log.e(TAG, "Başlık görseli açılamadı; degrade zemin kullanılacak");
            return null;
        }
        // Ana Sayfa köşesiz biter ve alt kenarı sayfa zeminine solar (başlık + içerik tek manzara)
        return new HeaderPhotoDrawable(photo, 0f,
                ContextCompat.getColor(activity, R.color.color_header_photo_scrim),
                PHOTO_FOCUS_Y,
                ContextCompat.getColor(activity, R.color.color_home_bg_top),
                PHOTO_FADE_RATIO);
    }

    /** @param home Ana Sayfa mı (büyük başlık) yoksa diğer ekran mı (ince çubuk) */
    void setExpanded(boolean home) {
        this.home = home;
        for (View view : homeOnly) {
            view.setVisibility(home ? View.VISIBLE : View.GONE);
        }
        countdown.setVisibility(home ? View.GONE : View.VISIBLE);
        appBar.setBackground(home && homeBackground != null ? homeBackground : compactBackground);
        if (home) {
            panel.setPaddingRelative(0, homeTopPadding, 0, 0);
            label.setTextSize(TypedValue.COMPLEX_UNIT_PX, labelSize);
            kerahatLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, kerahatSize);
            // Manzara öne çıksın: Ana Sayfa'da başlık yazısı yok
            toolbar.setTitle(null);
        } else {
            panel.setPaddingRelative(0, 0, 0, compactPadding);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            kerahatLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            countdown.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        }

        for (View child : scrollingChildren) {
            AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) child.getLayoutParams();
            params.setScrollFlags(home ? EXPANDED_FLAGS : AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL);
            child.setLayoutParams(params);
        }
        // Ekran değişince başlık tam açık başlar (önceki ekranda yukarı çekilmiş olabilir)
        appBar.setExpanded(true, false);
        // Başlık tam açık başladı: sayaç tam görünür
        fadeCountdown(0);
    }
}
