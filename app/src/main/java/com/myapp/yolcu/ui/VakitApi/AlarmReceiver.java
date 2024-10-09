package com.myapp.yolcu.ui.VakitApi;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.myapp.yolcu.ui.main.MainActivity;
import com.myapp.yolcu.R;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("AlarmReceiver", "Alarm tetiklendi! --AlarmReceiver");

        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");

        // Bildirim kanalı oluşturma (Android 8.0 ve üstü için)
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "namaz_vakitleri_channel";

        // Android 8.0 ve üstü cihazlar için bildirim kanalı oluştur
        NotificationChannel channel = new NotificationChannel(channelId, "Namaz Vakitleri", NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(channel);

        // PendingIntent oluşturun (bildirime tıkladığında uygulamayı açmak için)
        Intent notificationIntent = new Intent(context, MainActivity.class); // Ana aktivitenizi belirtin
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        int requestCode = (int) System.currentTimeMillis(); // Benzersiz bir requestCode oluşturun
        PendingIntent pendingIntent = PendingIntent.getActivity(context, requestCode, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Bildirim oluşturma
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.crescent) // Bildirim simgesi
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent) // PendingIntent ekleyin
                .setAutoCancel(true); // Kullanıcı bildirime tıkladığında bildirim kaybolur

        // Bildirimi göster
        notificationManager.notify(1, builder.build());
    }
}
