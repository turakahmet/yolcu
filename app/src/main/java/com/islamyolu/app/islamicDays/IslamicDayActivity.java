package com.islamyolu.app.islamicDays;

import android.content.SharedPreferences;
import android.icu.util.IslamicCalendar;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.islamyolu.app.R;
import com.islamyolu.app.VakitApi.NamazVakiti;
import com.islamyolu.app.main.MainActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class IslamicDayActivity extends AppCompatActivity {

    private TextView cityNameText, hijriDateText, yaklasanaTarih, yaklasanaTarihDetay;
    private ImageView islamicDayImage;
    private RecyclerView diniGunlerRecycler;
    private String getCityNameFromPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_islamic_days);

        initializeViews();
        setupRecyclerView();
        loadCityData();
    }

    private void initializeViews() {
        cityNameText = findViewById(R.id.city_name);
        hijriDateText = findViewById(R.id.hijri_date);
        islamicDayImage = findViewById(R.id.islamic_day_image);
        diniGunlerRecycler = findViewById(R.id.dini_gunler_recycler);

        yaklasanaTarih = findViewById(R.id.yaklasanaTarih);
        yaklasanaTarihDetay = findViewById(R.id.yaklasanaTarihDetay);


        hijriDateText.setText(NamazVakiti.getInstance().getHicriDate());

    }

    private void setupRecyclerView() {
        diniGunlerRecycler.setLayoutManager(new LinearLayoutManager(this));
        // Adapter setup will be done when data is loaded
    }

    private void loadCityData() {
        SharedPreferences locationPrefs = getSharedPreferences(MainActivity.LOCATION_PREFS, MODE_PRIVATE);
        getCityNameFromPreferences = locationPrefs.getString("city_name", "");

        if (!getCityNameFromPreferences.isEmpty()) {
            cityNameText.setText(getCityNameFromPreferences);
            loadDiniGunler();
        }
    }

    private void loadDiniGunler() {
        // Hicri yıl
        // IslamicCalendar nesnesi oluştur
        List<DiniGun> diniGunList = new ArrayList<>();
        IslamicCalendar hijriYear = new IslamicCalendar();
        int hijriYearValue = hijriYear.get(IslamicCalendar.YEAR);
        IslamicCalendar hijriCalendar = new IslamicCalendar(hijriYearValue, IslamicCalendar.MUHARRAM, 1);

        // Üç Ayların Başlangıcı (Recep 1)
        hijriCalendar.set(IslamicCalendar.MONTH, IslamicCalendar.RAJAB);
        hijriCalendar.set(IslamicCalendar.DAY_OF_MONTH, 1);
        diniGunList.add(new DiniGun(DiniGunEnum.UC_AYLARIN_BASLANGICI, hijriToGregorian(hijriCalendar, 0)));

        // Regaib Kandili (Recep 3)
        hijriCalendar.set(IslamicCalendar.DAY_OF_MONTH, 3);
        diniGunList.add(new DiniGun(DiniGunEnum.REGAIB_KANDILI, getRegaibKandiliDate(hijriCalendar)));

        // Miraç Kandili (Recep 27)
        hijriCalendar.set(IslamicCalendar.DAY_OF_MONTH, 27);
        diniGunList.add(new DiniGun(DiniGunEnum.MIRAC_KANDILI, hijriToGregorian(hijriCalendar, 1)));

        // Berat Kandili (Şaban 15)
        hijriCalendar.set(IslamicCalendar.MONTH, IslamicCalendar.SHABAN);
        hijriCalendar.set(IslamicCalendar.DAY_OF_MONTH, 15);
        diniGunList.add(new DiniGun(DiniGunEnum.BERAT_KANDILI, hijriToGregorian(hijriCalendar, 1)));

        // Ramazan Başlangıcı (Ramazan 1)
        hijriCalendar.set(IslamicCalendar.MONTH, IslamicCalendar.RAMADAN);
        hijriCalendar.set(IslamicCalendar.DAY_OF_MONTH, 1);
        diniGunList.add(new DiniGun(DiniGunEnum.RAMAZAN_BASLANGICI, hijriToGregorian(hijriCalendar, 0)));

        // Kadir Gecesi (Ramazan 27)
        hijriCalendar.set(IslamicCalendar.DAY_OF_MONTH, 27);
        diniGunList.add(new DiniGun(DiniGunEnum.KADIR_GECESI, hijriToGregorian(hijriCalendar, 1)));

        // Ramazan Bayramı (Şevval 1)
        hijriCalendar.set(IslamicCalendar.MONTH, IslamicCalendar.SHAWWAL);
        hijriCalendar.set(IslamicCalendar.DAY_OF_MONTH, 1);
        diniGunList.add(new DiniGun(DiniGunEnum.RAMAZAN_BAYRAMI_AREFE, hijriToGregorian(hijriCalendar, 2)));
        diniGunList.add(new DiniGun(DiniGunEnum.RAMAZAN_BAYRAMI_1, hijriToGregorian(hijriCalendar, 1)));
        hijriCalendar.set(IslamicCalendar.DAY_OF_MONTH, 2);
        diniGunList.add(new DiniGun(DiniGunEnum.RAMAZAN_BAYRAMI_2, hijriToGregorian(hijriCalendar, 1)));
        hijriCalendar.set(IslamicCalendar.DAY_OF_MONTH, 3);
        diniGunList.add(new DiniGun(DiniGunEnum.RAMAZAN_BAYRAMI_3, hijriToGregorian(hijriCalendar, 1)));

        // Kurban Bayramı (Zilhicce 10)
        hijriCalendar.set(IslamicCalendar.MONTH, IslamicCalendar.DHU_AL_HIJJAH);
        hijriCalendar.set(IslamicCalendar.DAY_OF_MONTH, 10);
        diniGunList.add(new DiniGun(DiniGunEnum.KURBAN_BAYRAMI_AREFE, hijriToGregorian(hijriCalendar, 2)));
        diniGunList.add(new DiniGun(DiniGunEnum.KURBAN_BAYRAMI_1, hijriToGregorian(hijriCalendar, 1)));
        hijriCalendar.set(IslamicCalendar.DAY_OF_MONTH, 11);
        diniGunList.add(new DiniGun(DiniGunEnum.KURBAN_BAYRAMI_2, hijriToGregorian(hijriCalendar, 1)));
        hijriCalendar.set(IslamicCalendar.DAY_OF_MONTH, 12);
        diniGunList.add(new DiniGun(DiniGunEnum.KURBAN_BAYRAMI_3, hijriToGregorian(hijriCalendar, 1)));
        hijriCalendar.set(IslamicCalendar.DAY_OF_MONTH, 13);
        diniGunList.add(new DiniGun(DiniGunEnum.KURBAN_BAYRAMI_4, hijriToGregorian(hijriCalendar, 1)));

        // Hicri Yılbaşı (Muharrem 1)
        hijriCalendar.add(IslamicCalendar.YEAR, 1); // Yılı 1 artır
        hijriCalendar.set(IslamicCalendar.MONTH, IslamicCalendar.MUHARRAM);
        hijriCalendar.set(IslamicCalendar.DAY_OF_MONTH, 1);
        diniGunList.add(new DiniGun(DiniGunEnum.HICRI_YILBASI, hijriToGregorian(hijriCalendar, 1)));

        // Aşure Günü (Muharrem 10)
        hijriCalendar.set(IslamicCalendar.DAY_OF_MONTH, 10);
        diniGunList.add(new DiniGun(DiniGunEnum.ASURE_GUNU, hijriToGregorian(hijriCalendar, 1)));

        // Mevlid Kandili (Rebiülevvel 11)
        hijriCalendar.set(IslamicCalendar.MONTH, IslamicCalendar.RABI_1);
        hijriCalendar.set(IslamicCalendar.DAY_OF_MONTH, 11);
        diniGunList.add(new DiniGun(DiniGunEnum.MEVLID_KANDILI, hijriToGregorian(hijriCalendar, 1)));


        // Üç Ayların Başlangıcı (Recep 1)
        hijriCalendar.set(IslamicCalendar.MONTH, IslamicCalendar.RAJAB);
        hijriCalendar.set(IslamicCalendar.DAY_OF_MONTH, 1);
        diniGunList.add(new DiniGun(DiniGunEnum.UC_AYLARIN_BASLANGICI, hijriToGregorian(hijriCalendar, 0)));

        // Regaib Kandili (Recep 3)
        hijriCalendar.set(IslamicCalendar.DAY_OF_MONTH, 3);
        diniGunList.add(new DiniGun(DiniGunEnum.REGAIB_KANDILI, getRegaibKandiliDate(hijriCalendar)));

        IslamicDayAdapter adapter = new IslamicDayAdapter(diniGunList);
        diniGunlerRecycler.setAdapter(adapter);

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", new Locale("tr", "TR"));
        Date currentDate = new Date();

        for (DiniGun diniGun : diniGunList) {
            try {
                if (!currentDate.after(Objects.requireNonNull(sdf.parse(diniGun.getDiniTarih())))) {
                    yaklasanaTarih.setText(diniGun.getDiniGun().getAd());
                    yaklasanaTarihDetay.setText(diniGun.getDiniTarih());
                    diniGun.setAktifGunMu(true);
                    updateIslamicDayImage(diniGun);
                    break;
                } else {
                    yaklasanaTarih.setText(getApplicationContext().getString(R.string.yaklasanTarihYok));
                    yaklasanaTarihDetay.setText(getApplicationContext().getString(R.string.yaklasanTarihYok));
                }
            } catch (ParseException ignored) {
            }
        }
    }

    private String getRegaibKandiliDate(IslamicCalendar hijriCalendar) {

        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(hijriCalendar.getTime());

        // İlk Perşembe gününü bul
        while (gregorianCalendar.get(Calendar.DAY_OF_WEEK) != Calendar.THURSDAY) {
            gregorianCalendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        return hijriToGregorian(gregorianCalendar);
    }

    /**
     * Hicri takvimi Miladi'ye çevirir.
     */
    private static String hijriToGregorian(Calendar hijriCalendar) {
        int day = hijriCalendar.get(Calendar.DAY_OF_MONTH);
        int month = hijriCalendar.get(Calendar.MONTH) + 1; // Java'da Ocak=0 olduğu için 1 ekliyoruz
        int year = hijriCalendar.get(Calendar.YEAR);

        String dayOfWeek = getDayOfWeekName(hijriCalendar.get(Calendar.DAY_OF_WEEK));

        return day + " " + getMonthName(month) + " " + year + " " + dayOfWeek;
    }

    private static String hijriToGregorian(IslamicCalendar hijriCalendar, int gunCikar) {
        GregorianCalendar gregorianDate = new GregorianCalendar();
        gregorianDate.setTime(hijriCalendar.getTime());
        if (gunCikar > 0)
            gregorianDate.add(Calendar.DAY_OF_MONTH, -gunCikar);

        int day = gregorianDate.get(Calendar.DAY_OF_MONTH);
        int month = gregorianDate.get(Calendar.MONTH) + 1; // Java'da Ocak=0 olduğu için 1 ekliyoruz
        int year = gregorianDate.get(Calendar.YEAR);
        // Haftanın gününü al
        String dayOfWeek = getDayOfWeekName(gregorianDate.get(Calendar.DAY_OF_WEEK));


        return day + " " + getMonthName(month) + " " + year + " " + dayOfWeek;
    }

    // Haftanın gününün adını döndüren metot
    private static String getDayOfWeekName(int dayOfWeek) {
        String[] days = {"Pazar", "Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi"};
        return days[dayOfWeek - 1]; // Calendar.DAY_OF_WEEK, 1'den (Pazar) başlıyor.
    }

    /**
     * Ay numarasını String olarak döndürür.
     */
    private static String getMonthName(int month) {
        String[] months = {
                "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
                "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
        };
        return months[month - 1];
    }


    private void updateIslamicDayImage(DiniGun diniGun) {
        // Update prayer time image based on current prayer time
        // Example:
        int imageResource;

        if (diniGun.getDiniGun().equals(DiniGunEnum.UC_AYLARIN_BASLANGICI)) {
            imageResource = R.drawable.islamic_day_1;
        } else if (diniGun.getDiniGun().equals(DiniGunEnum.REGAIB_KANDILI)) {
            imageResource = R.drawable.islamic_day_2;
        } else if (diniGun.getDiniGun().equals(DiniGunEnum.MIRAC_KANDILI)) {
            imageResource = R.drawable.islamic_day_3;
        } else if (diniGun.getDiniGun().equals(DiniGunEnum.BERAT_KANDILI)) {
            imageResource = R.drawable.islamic_day_4;
        } else if (diniGun.getDiniGun().equals(DiniGunEnum.RAMAZAN_BASLANGICI)) {
            imageResource = R.drawable.islamic_day_5;
        } else if (diniGun.getDiniGun().equals(DiniGunEnum.KADIR_GECESI)) {
            imageResource = R.drawable.islamic_day_6;
        } else if (diniGun.getDiniGun().equals(DiniGunEnum.RAMAZAN_BAYRAMI_AREFE)) {
            imageResource = R.drawable.islamic_day_7;
        } else if (diniGun.getDiniGun().equals(DiniGunEnum.KURBAN_BAYRAMI_AREFE)) {
            imageResource = R.drawable.islamic_day_8;
        } else if (diniGun.getDiniGun().equals(DiniGunEnum.HICRI_YILBASI)) {
            imageResource = R.drawable.islamic_day_9;
        } else if (diniGun.getDiniGun().equals(DiniGunEnum.ASURE_GUNU)) {
            imageResource = R.drawable.islamic_day_10;
        } else {
            imageResource = R.drawable.islamic_day_1;
        }
        islamicDayImage.setImageResource(imageResource);
    }


}
