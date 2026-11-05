package com.islamyolu.app.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.islamyolu.app.R;
import com.islamyolu.app.preferences.AppPreferences;

public class WidgetSettingsActivity extends AppCompatActivity {
    private static final String TAG = "WidgetSettingsActivity";
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_settings);

        // Widget ID'sini al ve sonucu CANCELED olarak ayarla
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.d(TAG, "Widget ID geçersizse aktiviteyi sonlandır");
            finish();
            return;
        }

        // Sonuç baştan OK: kullanıcı ayarı kaydetmeden geri çıkarsa widget varsayılan şeffaflıkla eklenir.
        // (CANCELED dönülünce başlatıcı widget'ı siler; bazı markalarda widget hiç görünmüyordu.)
        Intent resultValueTemp = new Intent();
        resultValueTemp.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValueTemp);

        SeekBar transparencySeekBar = findViewById(R.id.transparencySeekBar);
        TextView transparencyValueText = findViewById(R.id.transparencyValueText);
        Button widgetSaveButton = findViewById(R.id.widgetSaveButton);

        // Kaydedilmiş şeffaflık değerini al
        int savedTransparency = loadTransparencyPref(this, mAppWidgetId);
        transparencySeekBar.setProgress(savedTransparency);
        updateTransparencyText(transparencyValueText, savedTransparency);

        transparencySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int roundedProgress = Math.round(progress / 25f) * 25;
                seekBar.setProgress(roundedProgress);
                updateTransparencyText(transparencyValueText, roundedProgress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        widgetSaveButton.setOnClickListener(v -> {
            // Kullanıcının seçimini kaydet
            int transparency = transparencySeekBar.getProgress();
            saveTransparencyPref(WidgetSettingsActivity.this, mAppWidgetId, transparency);

            // Widget'ı güncelle
            updateWidget();

            // Doğru sonucu set et
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, resultValue);

            finish();
        });
    }

    private void updateWidget() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        Intent updateIntent = new Intent(this, PrayerTimesWidget.class);
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        int[] ids = new int[]{mAppWidgetId};
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(updateIntent);
    }

    private void updateTransparencyText(TextView textView, int progress) {
        textView.setText(String.format("Şeffaflık: %%%d", progress));
    }

    public static void saveTransparencyPref(Context context, int appWidgetId, int transparency) {
        AppPreferences.from(context).widget().setTransparency(appWidgetId, transparency);
        Log.d(TAG, "Şeffaflık kaydedildi: " + transparency + " for widget: " + appWidgetId);
    }

    public static int loadTransparencyPref(Context context, int appWidgetId) {
        return AppPreferences.from(context).widget().getTransparency(appWidgetId);
    }

}