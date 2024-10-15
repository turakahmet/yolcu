package com.myapp.yolcu.ui.VakitApi;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.myapp.yolcu.R;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PrayerTimesFetcher extends Service {

    private Handler handler = new Handler();
    private Runnable runnable;
    private Context context;
    private TextView imsakTime, gunesTime, ogleTime, ikindiTime, aksamTime, yatsiTime, kalanSureTextView, toolbarKalanSureLabel;
    private LinearLayout imsakTimeLayout, gunesTimeLayout, ogleTimeLayout, ikindiTimeLayout, aksamTimeLayout, yatsiTimeLayout, locationRefreshLayout, remain_time_layout;
    private AtomicInteger namazVakitTip =new AtomicInteger(0);
    public PrayerTimesFetcher(Context context, TextView imsakTime, TextView gunesTime, TextView ogleTime, TextView ikindiTime, TextView aksamTime, TextView yatsiTime, LinearLayout imsakTimeLayout, LinearLayout gunesTimeLayout, LinearLayout ogleTimeLayout, LinearLayout ikindiTimeLayout, LinearLayout aksamTimeLayout, LinearLayout yatsiTimeLayout, LinearLayout locationRefreshLayout, TextView kalanSureTextView, LinearLayout remain_time_layout, TextView toolbarKalanSureLabel) {
        this.context = context;
        this.imsakTime = imsakTime;
        this.gunesTime = gunesTime;
        this.ogleTime = ogleTime;
        this.ikindiTime = ikindiTime;
        this.aksamTime = aksamTime;
        this.yatsiTime = yatsiTime;
        this.kalanSureTextView=kalanSureTextView;

        this.imsakTimeLayout = imsakTimeLayout;
        this.gunesTimeLayout = gunesTimeLayout;
        this.ogleTimeLayout = ogleTimeLayout;
        this.ikindiTimeLayout = ikindiTimeLayout;
        this.aksamTimeLayout = aksamTimeLayout;
        this.yatsiTimeLayout = yatsiTimeLayout;

        this.locationRefreshLayout = locationRefreshLayout;
        this.remain_time_layout=remain_time_layout;
        this.toolbarKalanSureLabel=toolbarKalanSureLabel;
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
                        assert locationRefreshLayout != null;
                        locationRefreshLayout.setVisibility(View.GONE);
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
                    // UI işlemleri yine ana thread'de yapılmalı
                    new Handler(Looper.getMainLooper()).post(() -> {
                        assert locationRefreshLayout != null;
                        locationRefreshLayout.setVisibility(View.VISIBLE);
                    });
                }
            }
        });
    }

    private void updateBackgroundColor(LocalTime currentTime) {
        LocalTime namazVakti = null;
        LinearLayout remainTimeLayout =null;
        gunesTimeLayout.setBackground(null);
        ogleTimeLayout.setBackground(null);
        ikindiTimeLayout.setBackground(null);
        aksamTimeLayout.setBackground(null);
        yatsiTimeLayout.setBackground(null);
        imsakTimeLayout.setBackground(null);
        if (imsakTime != null && currentTime.isAfter(parseTime(String.valueOf(imsakTime.getText()))) && currentTime.isBefore(parseTime(String.valueOf(gunesTime.getText())))) {
            gunesTimeLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.current_namaz_time));
            scheduleNotification(context.getString(R.string.namaz_vakit_notification_header), context.getString(R.string.gunes_vakit_notification_body), Objects.requireNonNull(parseTime(String.valueOf(gunesTime.getText()))));
            namazVakti= LocalTime.parse(gunesTime.getText());
            toolbarKalanSureLabel.setText(R.string.gunes_kalan);
            remainTimeLayout=gunesTimeLayout;
            namazVakitTip.set(1);
        } else if (gunesTime != null && currentTime.isAfter(parseTime(String.valueOf(gunesTime.getText()))) && currentTime.isBefore(parseTime(String.valueOf(ogleTime.getText())))) {
            ogleTimeLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.current_namaz_time));
            scheduleNotification(context.getString(R.string.namaz_vakit_notification_header), context.getString(R.string.ogle_vakit_notification_body), Objects.requireNonNull(parseTime(String.valueOf(ogleTime.getText()))));
            namazVakti= LocalTime.parse(ogleTime.getText());
            toolbarKalanSureLabel.setText(R.string.ogle_kalan);
            remainTimeLayout=ogleTimeLayout;
            namazVakitTip.set(2);
        } else if (ogleTime != null && currentTime.isAfter(parseTime(String.valueOf(ogleTime.getText()))) && currentTime.isBefore(parseTime(String.valueOf(ikindiTime.getText())))) {
            ikindiTimeLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.current_namaz_time));
            scheduleNotification(context.getString(R.string.namaz_vakit_notification_header), context.getString(R.string.ikindi_vakit_notification_body), Objects.requireNonNull(parseTime(String.valueOf(ikindiTime.getText()))));
            namazVakti= LocalTime.parse(ikindiTime.getText());
            toolbarKalanSureLabel.setText(R.string.ikindi_kalan);
            remainTimeLayout=ikindiTimeLayout;
            namazVakitTip.set(3);
        } else if (ikindiTime != null && currentTime.isAfter(parseTime(String.valueOf(ikindiTime.getText()))) && currentTime.isBefore(parseTime(String.valueOf(aksamTime.getText())))) {
            aksamTimeLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.current_namaz_time));
            scheduleNotification(context.getString(R.string.namaz_vakit_notification_header), context.getString(R.string.aksam_vakit_notification_body), Objects.requireNonNull(parseTime(String.valueOf(aksamTime.getText()))));
            namazVakti= LocalTime.parse(aksamTime.getText());
            toolbarKalanSureLabel.setText(R.string.aksam_kalan);
            remainTimeLayout=aksamTimeLayout;
            namazVakitTip.set(4);
        } else if (aksamTime != null && currentTime.isAfter(parseTime(String.valueOf(aksamTime.getText()))) && currentTime.isBefore(parseTime(String.valueOf(yatsiTime.getText())))) {
            yatsiTimeLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.current_namaz_time));
            scheduleNotification(context.getString(R.string.namaz_vakit_notification_header), context.getString(R.string.yatsi_vakit_notification_body), Objects.requireNonNull(parseTime(String.valueOf(yatsiTime.getText()))));
            namazVakti= LocalTime.parse(yatsiTime.getText());
            toolbarKalanSureLabel.setText(R.string.yatsi_kalan);
            remainTimeLayout=yatsiTimeLayout;
            namazVakitTip.set(5);
        } else {
            imsakTimeLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.current_namaz_time));
            scheduleNotification(context.getString(R.string.imsak_vakit_notification_header), context.getString(R.string.imsak_vakit_notification_body), Objects.requireNonNull(parseTime(String.valueOf(imsakTime.getText()))));
            namazVakti= LocalTime.parse(imsakTime.getText());
            toolbarKalanSureLabel.setText(R.string.imsak_kalan);
            remainTimeLayout=imsakTimeLayout;
            namazVakitTip.set(6);
        }
        startCountdown(namazVakti,remainTimeLayout,namazVakitTip);
    }

    private void startCountdown(LocalTime namazVakti, LinearLayout remainTimeLayout, AtomicInteger namazVakitTip) {
        runnable = new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                // Her saniye güncel zamanı alın
                LocalDateTime currentTime = LocalDateTime.now();

                // Sonraki namaz vaktine kalan süreyi hesaplayın
                LocalDateTime nextPrayerTime = LocalDateTime.of(currentTime.toLocalDate(), namazVakti);


                // Eğer mevcut zaman namaz vaktini geçmişse
                if (currentTime.isAfter(nextPrayerTime) && namazVakitTip.get()==6) {
                    nextPrayerTime = nextPrayerTime.plusDays(1);
                    namazVakitTip.set(0);
                }


                // Kalan süreyi hesaplayın
                Duration duration = Duration.between(currentTime, nextPrayerTime);

                if (!duration.isNegative() && !duration.isZero()) {
                    long hours = duration.toHours();
                    long minutes = duration.toMinutes() % 60;
                    long seconds = duration.getSeconds() % 60;

                    // Kalan süreyi "HH:mm:ss" formatında güncelleyin
                    @SuppressLint("DefaultLocale")
                    String timeLeft = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                    kalanSureTextView.setText(timeLeft);
                    if(minutes<10 && hours==0){
                        remain_time_layout.setBackgroundColor(ContextCompat.getColor(context, R.color.remain_namaz_time_3));
                        remainTimeLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.remain_namaz_time_3));
                    }else if(minutes<20 && hours==0){
                        remain_time_layout.setBackgroundColor(ContextCompat.getColor(context, R.color.remain_namaz_time_2));;
                        remainTimeLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.remain_namaz_time_2));;
                    }else {
                        remain_time_layout.setBackgroundColor(ContextCompat.getColor(context, R.color.remain_namaz_time_1));;
                    }
                    // Her saniye tekrar çalıştır
                    handler.postDelayed(this, 1000);
                } else {
                    // Eğer süre dolduysa TextView'i sıfırla veya yeni vakit için bir işlem yap
                    updateBackgroundColor(LocalTime.from(currentTime));
                }
            }
        };

        // İlk çalıştırma
        handler.post(runnable);
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

    public AtomicInteger getNamazVakitTip() {
        return namazVakitTip;
    }

    public void setNamazVakitTip(AtomicInteger namazVakitTip) {
        this.namazVakitTip = namazVakitTip;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handler.post(runnable); // Fetch işlemini başlat
        return START_STICKY; // Servisi devam ettir
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable); // Fetch işlemini durdur
    }

}
