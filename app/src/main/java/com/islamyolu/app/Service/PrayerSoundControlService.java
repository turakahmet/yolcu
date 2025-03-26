package com.islamyolu.app.Service;


import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import java.util.Objects;

public class PrayerSoundControlService extends Service {
    private static final String TAG = "PrayerSoundControlService";
    private BroadcastReceiver screenReceiver;
    public static final String ACTION_STOP_SOUND = "com.myapp.yolcu.STOP_PRAYER_SOUND";

    @Override
    public void onCreate() {
        super.onCreate();
        registerScreenReceiver();
    }

    private void registerScreenReceiver() {
        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Objects.equals(intent.getAction(), Intent.ACTION_SCREEN_OFF) ||
                        Objects.equals(intent.getAction(), Intent.ACTION_SCREEN_ON) ||
                        Objects.equals(intent.getAction(), Intent.ACTION_USER_PRESENT)) {

                    Log.d(TAG, "Ekran durumu değişti: " + intent.getAction());

                    // SoundService'e direkt olarak stop sinyali gönder
                    if (SoundService.getInstance() != null) {
                        Log.d(TAG, "SoundService instance bulundu, ses durdurulacak");
                        SoundService.getInstance().stopAndCleanup();
                    }

                    // Ayrıca broadcast da gönder
                    Intent stopIntent = new Intent(ACTION_STOP_SOUND);
                    sendBroadcast(stopIntent);
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(screenReceiver, filter);
        Log.d(TAG, "Screen receiver kaydedildi");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (screenReceiver != null) {
            unregisterReceiver(screenReceiver);
            screenReceiver = null;
            Log.d(TAG, "Screen receiver unregister edildi");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
} 