package com.myapp.yolcu.ui.pusula;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.myapp.yolcu.R;

public class QiblaDirectionView extends View {
    private Paint paint;
    private Bitmap qiblaArrowBitmap;
    private float qiblaDegree; // Kıble derecesi
    private float currentCompassDegree; // Pusulanın döndüğü derece

    public QiblaDirectionView(Context context) {
        super(context);
        init(context);
    }

    public QiblaDirectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        paint = new Paint();
        paint.setAntiAlias(true);

        // Kıble okunu drawable klasöründen yükle
        qiblaArrowBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.compass_qibla);
    }

    public void updateDegrees(float compassDegree, float qiblaDegree) {
        this.currentCompassDegree = compassDegree;
        this.qiblaDegree = qiblaDegree;
        invalidate(); // Yeniden çizim
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        float radius = Math.min(centerX, centerY) - 50;

        // Pusula görselini döndür
        canvas.save();
        canvas.rotate(-currentCompassDegree, centerX, centerY);

        // Kıble okunu çiz
        float arrowX = (float) (centerX + radius * Math.sin(Math.toRadians(qiblaDegree))) - (qiblaArrowBitmap.getWidth() / 2);
        float arrowY = (float) (centerY - radius * Math.cos(Math.toRadians(qiblaDegree))) - (qiblaArrowBitmap.getHeight() / 2);

        canvas.drawBitmap(qiblaArrowBitmap, arrowX, arrowY, paint);

        canvas.restore(); // Orijinal döndürmeyi geri al
    }
}
