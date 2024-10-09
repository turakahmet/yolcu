package com.myapp.yolcu.ui.VakitApi;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.myapp.yolcu.R;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PrayerTimesFetcher {
    private Context context;
    private TextView imsakTime, gunesTime, ogleTime, ikindiTime, aksamTime, yatsiTime;
    private LinearLayout imsakTimeLayout, gunesTimeLayout, ogleTimeLayout, ikindiTimeLayout, aksamTimeLayout, yatsiTimeLayout, locationRefreshLayout;

    public PrayerTimesFetcher(Context context, TextView imsakTime, TextView gunesTime, TextView ogleTime, TextView ikindiTime, TextView aksamTime, TextView yatsiTime, LinearLayout imsakTimeLayout, LinearLayout gunesTimeLayout, LinearLayout ogleTimeLayout, LinearLayout ikindiTimeLayout, LinearLayout aksamTimeLayout, LinearLayout yatsiTimeLayout, LinearLayout locationRefreshLayout) {
        this.context = context;
        this.imsakTime = imsakTime;
        this.gunesTime = gunesTime;
        this.ogleTime = ogleTime;
        this.ikindiTime = ikindiTime;
        this.aksamTime = aksamTime;
        this.yatsiTime = yatsiTime;

        this.imsakTimeLayout = imsakTimeLayout;
        this.gunesTimeLayout = gunesTimeLayout;
        this.ogleTimeLayout = ogleTimeLayout;
        this.ikindiTimeLayout = ikindiTimeLayout;
        this.aksamTimeLayout = aksamTimeLayout;
        this.yatsiTimeLayout = yatsiTimeLayout;

        this.locationRefreshLayout = locationRefreshLayout;
    }

    // Namaz vakitlerini çekmek için method
    public void fetchPrayerTimes(String url) {
        OkHttpClient client = new OkHttpClient();

        // Namaz vakitlerini çekeceğimiz URL
        Request request = new Request.Builder()
                .url(url)
                .build();

        // İstek yapılıyor
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    assert locationRefreshLayout != null;
                    locationRefreshLayout.setVisibility(View.GONE);
                    // Simdiki zaman
                    LocalTime currentTime = LocalTime.now();

                    // HTML içeriğini al
                    assert response.body() != null;
                    String htmlContent = response.body().string();

                    // HTML içeriğini Jsoup ile parse et
                    Document doc = Jsoup.parse(htmlContent);

                    // Vakitlerin bulunduğu div'i bul
                    Element prayTimesDiv = doc.selectFirst("div.today-pray-times");

                    // Her bir namaz vaktini al
                    assert prayTimesDiv != null;
                    Elements timeCells = prayTimesDiv.select("div.tpt-cell");

                    // UI işlemleri ana thread'de yapılmalı
                    new Handler(Looper.getMainLooper()).post(() -> {
                        for (Element cell : timeCells) {
                            String title = Objects.requireNonNull(cell.selectFirst("div.tpt-title")).text();
                            String time = Objects.requireNonNull(cell.selectFirst("div.tpt-time")).text();
                            switch (title) {
                                case "İmsak":
                                    imsakTime.setText(time);
                                    break;
                                case "Güneş":
                                    gunesTime.setText(time);
                                    break;
                                case "Öğle":
                                    ogleTime.setText(time);
                                    break;
                                case "İkindi":
                                    ikindiTime.setText(time);
                                    break;
                                case "Akşam":
                                    aksamTime.setText(time);
                                    break;
                                case "Yatsı":
                                    yatsiTime.setText(time);
                                    break;
                            }
                        }
                        // Vakitlerin arka plan rengini ayarla
                        updateBackgroundColor(currentTime);
                    });
                } else {
                    locationRefreshLayout.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void updateBackgroundColor(LocalTime currentTime) {
        if (imsakTime != null && currentTime.isAfter(parseTime(String.valueOf(imsakTime.getText()))) && currentTime.isBefore(parseTime(String.valueOf(gunesTime.getText())))) {
            gunesTimeLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.current_namaz_time));
            scheduleNotification("Namaz Vakti", "Güneş Doğmasına 10 dakika kaldı!", Objects.requireNonNull(parseTime(String.valueOf(gunesTime.getText()))));
        } else if (gunesTime != null && currentTime.isAfter(parseTime(String.valueOf(gunesTime.getText()))) && currentTime.isBefore(parseTime(String.valueOf(ogleTime.getText())))) {
            ogleTimeLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.current_namaz_time));
            scheduleNotification("Namaz Vakti", "Öğle namazına 10 dakika kaldı!", Objects.requireNonNull(parseTime(String.valueOf(ogleTime.getText()))));
        } else if (ogleTime != null && currentTime.isAfter(parseTime(String.valueOf(ogleTime.getText()))) && currentTime.isBefore(parseTime(String.valueOf(ikindiTime.getText())))) {
            ikindiTimeLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.current_namaz_time));
            scheduleNotification("Namaz Vakti", "İkindi namazına 10 dakika kaldı!", Objects.requireNonNull(parseTime(String.valueOf(ikindiTime.getText()))));
        } else if (ikindiTime != null && currentTime.isAfter(parseTime(String.valueOf(ikindiTime.getText()))) && currentTime.isBefore(parseTime(String.valueOf(aksamTime.getText())))) {
            aksamTimeLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.current_namaz_time));
            scheduleNotification("Namaz Vakti", "Akşam namazına 10 dakika kaldı!", Objects.requireNonNull(parseTime(String.valueOf(aksamTime.getText()))));
        } else if (aksamTime != null && currentTime.isAfter(parseTime(String.valueOf(aksamTime.getText()))) && currentTime.isBefore(parseTime(String.valueOf(yatsiTime.getText())))) {
            yatsiTimeLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.current_namaz_time));
            scheduleNotification("Namaz Vakti", "Yatsı namazına 10 dakika kaldı!", Objects.requireNonNull(parseTime(String.valueOf(yatsiTime.getText()))));
        } else if (yatsiTime != null && currentTime.isAfter(parseTime(String.valueOf(yatsiTime.getText()))) && currentTime.isBefore(parseTime(String.valueOf(imsakTime.getText())))) {
            imsakTimeLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.current_namaz_time));
            scheduleNotification("İmsak Vakti", "İmsak vaktine 10 dakika kaldı!", Objects.requireNonNull(parseTime(String.valueOf(imsakTime.getText()))));
        }
    }

    private LocalTime parseTime(String time) {
        try {
            // String zamanını "HH:mm" formatına göre LocalTime'a dönüştür
            return LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException e) {
            e.printStackTrace();
            return null;  // Eğer bir hata oluşursa null döndür
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private void scheduleNotification(String title, String message, LocalTime notificationTime) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("message", message);

        int requestCode = (int) System.currentTimeMillis(); // Benzersiz bir requestCode oluşturun
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE );

        // Namaz vaktinden 10 dakika önceye ayarlama
        long triggerTime = notificationTime
                .minusMinutes(10) // 10 dakika önce
                .atDate(LocalDate.now())
                .atZone(ZoneId.systemDefault()) // Zaman dilimine göre dönüştür
                .toInstant()
                .toEpochMilli(); // Milisaniye cinsine çevirme
//         triggerTime = System.currentTimeMillis() + 5000; // 5 saniye sonrası

        // Şu anki zamandan daha ilerideki bir zamana ayarlayın
        if (triggerTime > System.currentTimeMillis()) {
            Log.d("AlarmReceiver", "Alarm tetiklendi!");
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }
}
