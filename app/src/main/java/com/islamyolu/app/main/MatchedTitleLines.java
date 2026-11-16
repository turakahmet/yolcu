package com.islamyolu.app.main;

import android.text.Layout;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

/**
 * Yan yana kartlardaki başlıkların (ör. "Erkek ismi" / "Kız ismi") aynı düzende görünmesini sağlar:
 * biri tek satıra sığmıyorsa hepsi ilk boşluktan iki satıra iner; hepsi sığıyorsa hepsi tek satır.
 * Karar ölçülen metin genişliğine ve gerçek görünüm genişliğine göre verilir (cihaz modeline göre
 * değil); pencere genişliği veya yazı boyutu değişince yeniden değerlendirilir.
 */
final class MatchedTitleLines implements View.OnLayoutChangeListener {

    private final TextView[] titles;
    private final CharSequence[] originals;

    private MatchedTitleLines(@NonNull TextView[] titles) {
        this.titles = titles;
        originals = new CharSequence[titles.length];
        for (int i = 0; i < titles.length; i++) {
            originals[i] = titles[i].getText();
        }
    }

    /** Başlık metinleri verildikten sonra çağrılır. */
    static void attach(@NonNull TextView... titles) {
        MatchedTitleLines matcher = new MatchedTitleLines(titles);
        for (TextView title : titles) {
            title.addOnLayoutChangeListener(matcher);
        }
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom,
                               int oldLeft, int oldTop, int oldRight, int oldBottom) {
        if (right - left != oldRight - oldLeft) {
            // Metin değişikliği yerleşim sırasında yapılmaz
            v.post(this::apply);
        }
    }

    private void apply() {
        boolean allFit = true;
        for (int i = 0; i < titles.length; i++) {
            TextView title = titles[i];
            int available = title.getWidth() - title.getTotalPaddingLeft() - title.getTotalPaddingRight();
            if (available <= 0) {
                return;
            }
            // Orijinal metnin tek satır genişliği: mevcut (bölünmüş olabilir) metne göre değil
            if (Layout.getDesiredWidth(originals[i], title.getPaint()) > available) {
                allFit = false;
                break;
            }
        }
        for (int i = 0; i < titles.length; i++) {
            CharSequence text = allFit ? originals[i] : breakAtFirstSpace(originals[i]);
            if (!text.toString().contentEquals(titles[i].getText())) {
                titles[i].setText(text);
            }
        }
    }

    @NonNull
    private static CharSequence breakAtFirstSpace(@NonNull CharSequence text) {
        String value = text.toString();
        int space = value.indexOf(' ');
        return space < 0 ? value : value.substring(0, space) + '\n' + value.substring(space + 1);
    }
}
