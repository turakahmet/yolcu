package com.islamyolu.app.main;

import android.text.Layout;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.islamyolu.app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Hava kartının üst satırı: solda bugün, sağda Hissedilen / Nem / Rüzgar.
 * Her ekranda aynı tasarım görünsün diye satır yan yana kalır; sığmıyorsa (dar pencere, büyük ekran
 * ölçeği veya yazı boyutu) satırdaki yazılar ve ikonlar aynı oranda ölçeklenir. Oran
 * {@link #MIN_SCALE} altına inmesi gerekiyorsa (çok büyük erişilebilirlik yazısı) okunabilirlik
 * öncelikli olur: ölçek 1'e döner ve değerler bugünün altına tam genişlikte iner.
 * Karar cihaz modeline değil, gerçek pencere genişliğine ve ölçülen metin genişliğine göre verilir.
 */
final class WeatherStatsLayout implements View.OnLayoutChangeListener {

    /** Bu oranın altında yazı okunaksızlaşır; daha dar alanda alt alta düzene geçilir. */
    private static final float MIN_SCALE = 0.78f;
    /** Ölçeklenmeyen iç boşluklar ve yuvarlama için pay */
    private static final float SAFETY = 0.97f;

    private final LinearLayout topRow;
    private final View today;
    private final View divider;
    private final LinearLayout statsRow;
    private final View[] cells;
    private final int stackedTopMargin;
    private final List<Scalable> scalables = new ArrayList<>();
    private float scale = 1f;

    WeatherStatsLayout(@NonNull LinearLayout topRow, @NonNull View today, @NonNull View divider,
                       @NonNull LinearLayout statsRow, @NonNull View... cells) {
        this.topRow = topRow;
        this.today = today;
        this.divider = divider;
        this.statsRow = statsRow;
        this.cells = cells;
        stackedTopMargin = Math.round(8 * topRow.getResources().getDisplayMetrics().density);
        collect(today);
        collect(statsRow);
        topRow.addOnLayoutChangeListener(this);
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom,
                               int oldLeft, int oldTop, int oldRight, int oldBottom) {
        if (right - left != oldRight - oldLeft) {
            update();
        }
    }

    /** Değer metinleri değişince (hava verisi geldi) yeniden değerlendirilir. */
    void update() {
        // Ölçümler yerleşimden sonra geçerli; düzen değişikliği yerleşim sırasında yapılmaz
        topRow.post(this::apply);
    }

    private void apply() {
        if (topRow.getWidth() == 0 || today.getWidth() == 0) {
            return;
        }
        float needed = scaleNeeded();
        boolean fits = needed >= MIN_SCALE;
        float target = fits ? needed : 1f;
        if (Math.abs(target - scale) > 0.01f) {
            setScale(target);
        }
        setHorizontal(fits);
    }

    /**
     * Yan yana sığmak için gereken ölçek (en fazla 1). Genişlikler mevcut ölçekten 1'e geri çevrilir;
     * bugünün genişliği iki düzende de aynı (wrap_content) olduğundan karar salınım yapmaz.
     */
    private float scaleNeeded() {
        LinearLayout.LayoutParams dividerParams = (LinearLayout.LayoutParams) divider.getLayoutParams();
        int innerDividers = statsRow.getChildCount() - cells.length;
        float fixed = topRow.getPaddingStart() + topRow.getPaddingEnd()
                + dividerParams.width + dividerParams.getMarginStart() + innerDividers;
        float content = today.getWidth() / scale;
        float widestCell = 0f;
        int cellPadding = 0;
        for (View cell : cells) {
            widestCell = Math.max(widestCell, cellTextWidth(cell) / scale);
            cellPadding = Math.max(cellPadding, cell.getPaddingStart() + cell.getPaddingEnd());
        }
        // Hücreler eşit genişlikte; en genişi belirleyicidir
        fixed += cellPadding * cells.length;
        content += widestCell * cells.length;
        float available = topRow.getWidth() - fixed;
        return Math.min(1f, available * SAFETY / content);
    }

    private static float cellTextWidth(@NonNull View cell) {
        TextView label = cell.findViewById(R.id.weather_stat_label);
        TextView value = cell.findViewById(R.id.weather_stat_value);
        // Etiket kelime sınırında iki satıra inebilir: en uzun kelimesi sığmalı. Değer tek satırdır.
        float widest = textWidth(value, value.getText().toString());
        for (String word : label.getText().toString().split("\\s+")) {
            widest = Math.max(widest, textWidth(label, word));
        }
        return widest;
    }

    private static float textWidth(@NonNull TextView view, @NonNull String text) {
        return Layout.getDesiredWidth(text, view.getPaint()) + view.getPaddingStart() + view.getPaddingEnd();
    }

    private void setHorizontal(boolean horizontal) {
        if ((topRow.getOrientation() == LinearLayout.HORIZONTAL) == horizontal) {
            return;
        }
        topRow.setOrientation(horizontal ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        divider.setVisibility(horizontal ? View.VISIBLE : View.GONE);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) statsRow.getLayoutParams();
        params.width = horizontal ? 0 : LinearLayout.LayoutParams.MATCH_PARENT;
        params.weight = horizontal ? 1f : 0f;
        params.topMargin = horizontal ? 0 : stackedTopMargin;
        statsRow.setLayoutParams(params);
    }

    private void setScale(float newScale) {
        scale = newScale;
        for (Scalable item : scalables) {
            item.apply(newScale);
        }
    }

    /** Ölçeklenecek yazı ve ikonların asıl (ölçek 1) değerleri bir kez kaydedilir. */
    private void collect(@NonNull View view) {
        if (view instanceof TextView) {
            scalables.add(new ScalableText((TextView) view));
        } else if (view instanceof ImageView) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            if (params != null && params.width > 0 && params.height > 0) {
                scalables.add(new ScalableImage(view, params.width, params.height));
            }
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collect(group.getChildAt(i));
            }
        }
    }

    private interface Scalable {
        void apply(float scale);
    }

    private static final class ScalableText implements Scalable {
        private final TextView view;
        private final float textSizePx;
        private final int maxWidth;

        ScalableText(@NonNull TextView view) {
            this.view = view;
            textSizePx = view.getTextSize();
            maxWidth = view.getMaxWidth();
        }

        @Override
        public void apply(float scale) {
            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx * scale);
            if (maxWidth > 0 && maxWidth < Integer.MAX_VALUE) {
                view.setMaxWidth(Math.round(maxWidth * scale));
            }
        }
    }

    private static final class ScalableImage implements Scalable {
        private final View view;
        private final int width;
        private final int height;

        ScalableImage(@NonNull View view, int width, int height) {
            this.view = view;
            this.width = width;
            this.height = height;
        }

        @Override
        public void apply(float scale) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.width = Math.round(width * scale);
            params.height = Math.round(height * scale);
            view.setLayoutParams(params);
        }
    }
}
