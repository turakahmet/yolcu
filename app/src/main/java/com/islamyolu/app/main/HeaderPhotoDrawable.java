package com.islamyolu.app.main;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Ana Sayfa başlığının fotoğraflı arka planı.
 * <p>
 * Görsel bozulmadan ölçeklenir ve başlığa sığacak şekilde kırpılır (centerCrop). Dikeyde
 * {@code focusY} oranındaki satır ortada tutulur, böylece cami ve ağaç çizgisi her ekran oranında
 * görünür kalır. Yazıların okunması için üstüne hafif karartma çizilir. Alt kenar sayfanın zemin
 * rengine solar; böylece başlık ile altındaki içerik tek parça bir manzara gibi görünür.
 * İstenirse alt köşeler yuvarlatılır (0 = köşesiz).
 */
final class HeaderPhotoDrawable extends Drawable {

    private final int bitmapWidth;
    private final int bitmapHeight;
    private final float bottomRadius;
    private final float focusY;
    private final BitmapShader shader;
    private final Paint photoPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint scrimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fadePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    @ColorInt
    private final int fadeColor;
    /** Alt kenarda zemine solan bölümün yükseklik oranı */
    private final float fadeRatio;
    private final Matrix matrix = new Matrix();
    private final Path shape = new Path();
    private final RectF rect = new RectF();

    /**
     * @param bottomRadius alt köşe yarıçapı (px)
     * @param scrim        fotoğraf üstündeki karartma rengi (yarı saydam)
     * @param focusY       görselde ortada tutulacak dikey konum (0 = üst, 1 = alt)
     * @param fadeColor    alt kenarın solduğu zemin rengi (saydam = solma yok)
     * @param fadeRatio    solan bölümün yükseklik oranı (0–1)
     */
    HeaderPhotoDrawable(@NonNull Bitmap bitmap, float bottomRadius, @ColorInt int scrim, float focusY,
                        @ColorInt int fadeColor, float fadeRatio) {
        this.bitmapWidth = bitmap.getWidth();
        this.bitmapHeight = bitmap.getHeight();
        this.bottomRadius = bottomRadius;
        this.focusY = focusY;
        shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        photoPaint.setShader(shader);
        scrimPaint.setColor(scrim);
        this.fadeColor = fadeColor;
        this.fadeRatio = fadeRatio;
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        float width = bounds.width();
        float height = bounds.height();
        if (width <= 0 || height <= 0) {
            return;
        }
        float scale = Math.max(width / bitmapWidth, height / bitmapHeight);
        float scaledHeight = bitmapHeight * scale;
        float dx = (width - bitmapWidth * scale) / 2f;
        // focusY satırı ortaya gelsin; görselin dışına taşmasın
        float dy = height / 2f - focusY * scaledHeight;
        dy = Math.max(height - scaledHeight, Math.min(0f, dy));

        matrix.setScale(scale, scale);
        matrix.postTranslate(bounds.left + dx, bounds.top + dy);
        shader.setLocalMatrix(matrix);

        rect.set(bounds);
        float r = Math.min(bottomRadius, height / 2f);
        shape.reset();
        shape.addRoundRect(rect, new float[]{0, 0, 0, 0, r, r, r, r}, Path.Direction.CW);

        float fadeTop = bounds.bottom - height * fadeRatio;
        fadePaint.setShader(new LinearGradient(0, fadeTop, 0, bounds.bottom,
                fadeColor & 0x00FFFFFF, fadeColor, Shader.TileMode.CLAMP));
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        canvas.drawPath(shape, photoPaint);
        canvas.drawPath(shape, scrimPaint);
        if (Color.alpha(fadeColor) != 0 && fadeRatio > 0f) {
            canvas.drawPath(shape, fadePaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        photoPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        photoPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    /** Zorunlu soyut metot; API 29+ sistem kullanmaz */
    @SuppressWarnings("deprecation")
    @Override
    public int getOpacity() {
        // Yuvarlak köşelerin dışı saydam
        return PixelFormat.TRANSLUCENT;
    }
}
