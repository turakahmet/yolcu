package com.islamyolu.app.VakitApi;

import static com.islamyolu.app.main.MainActivity.SETTINGS_PREFS;

import android.annotation.SuppressLint;
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

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.islamyolu.app.Entity.PrayerNames;
import com.islamyolu.app.R;
import com.islamyolu.app.Service.SharedPrefsHelperService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.atomic.AtomicInteger;

public class PrayerTimesFetcher extends Service {

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable;
    private Context context;
    private TextView imsakTime, gunesTime, ogleTime, ikindiTime, aksamTime, yatsiTime, kalanSureTextView, toolbarKalanSureLabel;
    private LinearLayout imsakTimeLayout, gunesTimeLayout, ogleTimeLayout, ikindiTimeLayout, aksamTimeLayout, yatsiTimeLayout, locationRefreshLayout, remain_time_layout;
    private AtomicInteger namazVakitTip = new AtomicInteger(0);
    private NamazVakiti namazVakiti;
    private final int kerahatVakti = 45;
    private SharedPrefsHelperService sharedPrefsHelperService;

    public PrayerTimesFetcher(Context context, TextView imsakTime, TextView gunesTime, TextView ogleTime, TextView ikindiTime, TextView aksamTime, TextView yatsiTime, LinearLayout imsakTimeLayout, LinearLayout gunesTimeLayout, LinearLayout ogleTimeLayout, LinearLayout ikindiTimeLayout, LinearLayout aksamTimeLayout, LinearLayout yatsiTimeLayout, LinearLayout locationRefreshLayout, TextView kalanSureTextView, LinearLayout remain_time_layout, TextView toolbarKalanSureLabel) {
        this.context = context;
        this.imsakTime = imsakTime;
        this.gunesTime = gunesTime;
        this.ogleTime = ogleTime;
        this.ikindiTime = ikindiTime;
        this.aksamTime = aksamTime;
        this.yatsiTime = yatsiTime;
        this.kalanSureTextView = kalanSureTextView;

        this.imsakTimeLayout = imsakTimeLayout;
        this.gunesTimeLayout = gunesTimeLayout;
        this.ogleTimeLayout = ogleTimeLayout;
        this.ikindiTimeLayout = ikindiTimeLayout;
        this.aksamTimeLayout = aksamTimeLayout;
        this.yatsiTimeLayout = yatsiTimeLayout;

        this.locationRefreshLayout = locationRefreshLayout;
        this.remain_time_layout = remain_time_layout;
        this.toolbarKalanSureLabel = toolbarKalanSureLabel;
        sharedPrefsHelperService = new SharedPrefsHelperService(context, SETTINGS_PREFS);
    }

    public boolean fetchPrayerTimesNoAPI(String city) {
        LocalTime currentTime = LocalTime.now();
        VakitNoApi vakitNoApi = new VakitNoApi();
        namazVakiti = vakitNoApi.getLocalPrayerTimesByCityName(city);
        if (namazVakiti.getGecerliVakit() == null) {
            return false;
        }
        if (namazVakiti != null) {
            imsakTime.setText(namazVakiti.getImsak());
            gunesTime.setText(namazVakiti.getGunes());
            ogleTime.setText(namazVakiti.getOgle());
            ikindiTime.setText(namazVakiti.getIkindi());
            aksamTime.setText(namazVakiti.getAksam());
            yatsiTime.setText(namazVakiti.getYatsi());
            updateBackgroundColor(currentTime);
            assert locationRefreshLayout != null;
            locationRefreshLayout.setVisibility(View.GONE);
        } else {
            locationRefreshLayout.setVisibility(View.VISIBLE);
        }
        return true;
    }

    private void updateBackgroundColor(LocalTime currentTime) {
        LocalTime vakit = null;
        LinearLayout remainTimeLayout = null;
        gunesTimeLayout.setBackground(null);
        ogleTimeLayout.setBackground(null);
        ikindiTimeLayout.setBackground(null);
        aksamTimeLayout.setBackground(null);
        yatsiTimeLayout.setBackground(null);
        imsakTimeLayout.setBackground(null);
        if (namazVakiti.getImsak() != null && currentTime.isAfter(parseTime(namazVakiti.getImsak())) && currentTime.isBefore(parseTime(namazVakiti.getGunes()))) {
            Log.d("PrayerTimesFetcher", "Namaz Vakti tetiklendi! Gunes dogmasi");
            gunesTimeLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.current_namaz_time));
            vakit = LocalTime.parse(gunesTime.getText());
            toolbarKalanSureLabel.setText(R.string.gunes_kalan);
            remainTimeLayout = gunesTimeLayout;
            namazVakitTip.set(1);
            namazVakiti.setGecerliVakit(NamazVakiti.GecerliVakit.GUNES);
        } else if (namazVakiti.getGunes() != null && currentTime.isAfter(parseTime(namazVakiti.getGunes())) && currentTime.isBefore(parseTime(namazVakiti.getOgle()))) {
            Log.d("PrayerTimesFetcher", "Namaz Vakti tetiklendi! Ogle namazi");
            ogleTimeLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.current_namaz_time));
            vakit = LocalTime.parse(ogleTime.getText());
            toolbarKalanSureLabel.setText(R.string.ogle_kalan);
            remainTimeLayout = ogleTimeLayout;
            namazVakitTip.set(2);
            namazVakiti.setGecerliVakit(NamazVakiti.GecerliVakit.OGLE);
        } else if (namazVakiti.getOgle() != null && currentTime.isAfter(parseTime(namazVakiti.getOgle())) && currentTime.isBefore(parseTime(namazVakiti.getIkindi()))) {
            Log.d("PrayerTimesFetcher", "Namaz Vakti tetiklendi! ikindi namazi");
            ikindiTimeLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.current_namaz_time));
            vakit = LocalTime.parse(ikindiTime.getText());
            toolbarKalanSureLabel.setText(R.string.ikindi_kalan);
            remainTimeLayout = ikindiTimeLayout;
            namazVakitTip.set(3);
            namazVakiti.setGecerliVakit(NamazVakiti.GecerliVakit.IKINDI);
        } else if (namazVakiti.getIkindi() != null && currentTime.isAfter(parseTime(namazVakiti.getIkindi())) && currentTime.isBefore(parseTime(namazVakiti.getAksam()))) {
            Log.d("PrayerTimesFetcher", "Namaz Vakti tetiklendi! aksam namazi");
            aksamTimeLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.current_namaz_time));
            vakit = LocalTime.parse(aksamTime.getText());
            toolbarKalanSureLabel.setText(R.string.aksam_kalan);
            remainTimeLayout = aksamTimeLayout;
            namazVakitTip.set(4);
            namazVakiti.setGecerliVakit(NamazVakiti.GecerliVakit.AKSAM);
        } else if (namazVakiti.getAksam() != null && currentTime.isAfter(parseTime(namazVakiti.getAksam())) && currentTime.isBefore(parseTime(namazVakiti.getYatsi()))) {
            Log.d("PrayerTimesFetcher", "Namaz Vakti tetiklendi! yatsi namazi");
            yatsiTimeLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.current_namaz_time));
            vakit = LocalTime.parse(yatsiTime.getText());
            toolbarKalanSureLabel.setText(R.string.yatsi_kalan);
            remainTimeLayout = yatsiTimeLayout;
            namazVakitTip.set(5);
            namazVakiti.setGecerliVakit(NamazVakiti.GecerliVakit.YATSI);
        } else if (currentTime.isAfter(parseTime(namazVakiti.getYatsi()))) {
            Log.d("PrayerTimesFetcher", "Namaz Vakti tetiklendi! imsak vakti");
            imsakTimeLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.current_namaz_time));
            vakit = LocalTime.parse(imsakTime.getText());
            toolbarKalanSureLabel.setText(R.string.imsak_kalan);
            remainTimeLayout = imsakTimeLayout;
            namazVakitTip.set(6);
            namazVakiti.setGecerliVakit(NamazVakiti.GecerliVakit.IMSAK);
        } else if (currentTime.isAfter(LocalTime.MIDNIGHT)) {
            Log.d("PrayerTimesFetcher", "Namaz Vakti tetiklendi! Test");
            imsakTimeLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.current_namaz_time));
            vakit = LocalTime.parse(imsakTime.getText());
            toolbarKalanSureLabel.setText(R.string.imsak_kalan);
            remainTimeLayout = imsakTimeLayout;
            namazVakitTip.set(6);
            namazVakiti.setGecerliVakit(NamazVakiti.GecerliVakit.IMSAK);
        }

        startCountdown(vakit, remainTimeLayout, namazVakitTip);
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
//                if (currentTime.isAfter(nextPrayerTime) && namazVakitTip.get() == 6) {
//                    nextPrayerTime = nextPrayerTime.plusDays(1);
//                    namazVakitTip.set(0);
//                }
                if (PrayerNames.IMSAK.getValue().equals(namazVakiti.getNextPrayer().getName())) {
                    if ((currentTime.isAfter(LocalDateTime.of(currentTime.toLocalDate(), LocalTime.parse(namazVakiti.getYatsi()))) && currentTime.isBefore(LocalDateTime.of(currentTime.toLocalDate().plusDays(1), LocalTime.MIDNIGHT)))) {
                        nextPrayerTime = nextPrayerTime.plusDays(1);

                    }
//                    nextPrayerTime = nextPrayerTime.plusDays(1);
                }

                // Kalan süreyi hesaplayın
                Duration duration = Duration.between(currentTime, nextPrayerTime);
                namazVakiti.setKalanSure(duration);

                if (!duration.isNegative() && !duration.isZero()) {
                    long hours = duration.toHours();
                    long minutes = duration.toMinutes() % 60;
                    long seconds = duration.getSeconds() % 60;

                    // Kalan süreyi "HH:mm:ss" formatında güncelleyin
                    @SuppressLint("DefaultLocale")
                    String timeLeft = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                    kalanSureTextView.setText(timeLeft);
                    if (minutes < 10 && hours == 0) {
                        remain_time_layout.setBackgroundColor(ContextCompat.getColor(context, R.color.remain_namaz_time_3));
                        remainTimeLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.remain_namaz_time_3));
                    } else if (minutes < 20 && hours == 0) {
                        remain_time_layout.setBackgroundColor(ContextCompat.getColor(context, R.color.remain_namaz_time_2));
                        remainTimeLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.remain_namaz_time_2));
                    } else if (sharedPrefsHelperService.getSwitchState("switchNotificationKerahat") && minutes < kerahatVakti && hours == 0 && (NamazVakiti.GecerliVakit.OGLE.equals(NamazVakiti.getInstance().getGecerliVakit()) || NamazVakiti.GecerliVakit.AKSAM.equals(NamazVakiti.getInstance().getGecerliVakit()))) {
                        remain_time_layout.setBackgroundColor(ContextCompat.getColor(context, R.color.remain_kerahat_time));
                        remainTimeLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.remain_kerahat_time));
                        if (NamazVakiti.GecerliVakit.OGLE.equals(NamazVakiti.getInstance().getGecerliVakit()))
                            toolbarKalanSureLabel.setText(context.getString(R.string.ogle_kalan) + " / KERAHAT VAKTİ İÇİNDESİNİZ");
                        else if (NamazVakiti.GecerliVakit.AKSAM.equals(NamazVakiti.getInstance().getGecerliVakit())) {
                            toolbarKalanSureLabel.setText(context.getString(R.string.aksam_kalan) + " / KERAHAT VAKTİ İÇİNDESİNİZ");
                        }
                    } else if (sharedPrefsHelperService.getSwitchState("switchNotificationKerahat") && (NamazVakiti.GecerliVakit.OGLE.equals(NamazVakiti.getInstance().getGecerliVakit()))) {
                        // Güneş doğumu saatini alın
                        LocalTime gunesDogumu = NamazVakiti.getInstance().getVakitTime(NamazVakiti.GecerliVakit.GUNES);

                        // Sabah namazından 45 dakika sonrasını hesaplayın
                        LocalTime kerahatBitis = gunesDogumu.plusMinutes(45);

                        // Mevcut zaman
                        LocalDateTime gunesDogumuDateTime = LocalDateTime.of(currentTime.toLocalDate(), gunesDogumu);
                        LocalDateTime kerahatBitisDateTime = LocalDateTime.of(currentTime.toLocalDate(), kerahatBitis);

                        // Eğer şu anki zaman kerahat vakti içindeyse
                        if (currentTime.isAfter(gunesDogumuDateTime) && currentTime.isBefore(kerahatBitisDateTime)) {
                            remain_time_layout.setBackgroundColor(ContextCompat.getColor(context, R.color.remain_kerahat_time));
                            remainTimeLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.remain_kerahat_time));
                            toolbarKalanSureLabel.setText("KERAHAT VAKTİ İÇİNDESİNİZ");
                        } else {
                            toolbarKalanSureLabel.setText(context.getString(R.string.ogle_kalan));
                            remain_time_layout.setBackgroundColor(ContextCompat.getColor(context, R.color.remain_namaz_time_1));
                            ;
                        }
                    } else {
                        remain_time_layout.setBackgroundColor(ContextCompat.getColor(context, R.color.remain_namaz_time_1));
                        ;
                    }
                    // Her saniye tekrar çalıştır
                    handler.postDelayed(this, 1000);
                } else {
                    // Eğer süre dolduysa TextView'i sıfırla veya yeni vakit için bir işlem yap
                    updateBackgroundColor(LocalTime.from(currentTime));
                    handler.removeCallbacks(this); // Runnable'ı durdur
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        handler.post(runnable); // Fetch işlemini başlat
        return START_STICKY; // Servisi devam ettir
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
//        handler.removeCallbacks(runnable); // Fetch işlemini durdur
    }

}
