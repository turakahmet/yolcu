package com.islamyolu.app.ibadet;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ScrollView;

public class ZoomableScrollView extends ScrollView {

    private float scaleFactor = 1.0f; // Varsayılan ölçek
    private ScaleGestureDetector scaleGestureDetector;

    private float focusX = 0; // Odak noktası X
    private float focusY = 0; // Odak noktası Y

    private float lastFocusX = 0; // Son odak X
    private float lastFocusY = 0; // Son odak Y

    public ZoomableScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        setWillNotDraw(false); // Yeniden çizim için gerekli
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Ölçek hareketlerini algılar
        scaleGestureDetector.onTouchEvent(ev);

        // Scroll hareketlerini düzgün işleyebilmek için
        return super.onTouchEvent(ev);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.save();

        // Odak noktasına göre ölçekleme işlemi
        canvas.scale(scaleFactor, scaleFactor, focusX, focusY);

        super.dispatchDraw(canvas);
        canvas.restore();
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            // Odak noktası alınır
            focusX = detector.getFocusX();
            focusY = detector.getFocusY();

            // Ölçek faktörünü güncelle
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(1f, Math.min(scaleFactor, 1.2f)); // Minimum 0.5x, maksimum 3.0x zoom

            // Kaydırmayı odak noktasına göre düzenle
            adjustScrollForFocus();

            invalidate(); // Ekranı yeniden çiz
            return true;
        }

        private void adjustScrollForFocus() {
            // Odak noktası ile kaydırma hareketini hesapla
            float dx = focusX - lastFocusX;
            float dy = focusY - lastFocusY;

            // Kaydırmayı uygula
            scrollBy((int) (-dx * (scaleFactor - 1)), (int) (-dy * (scaleFactor - 1)));

            // Son odak noktalarını güncelle
            lastFocusX = focusX;
            lastFocusY = focusY;
        }
    }
}
