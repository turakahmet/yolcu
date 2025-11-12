package com.islamyolu.app.islamicDays;

import android.icu.util.IslamicCalendar;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.islamyolu.app.R;
import com.islamyolu.app.Service.APIService;
import com.islamyolu.app.Service.NetworkServiceUtil;
import com.islamyolu.app.Utils.APIClient;
import com.islamyolu.app.VakitApi.NamazVakiti;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Response;

public class IslamicDayActivity extends AppCompatActivity {

    private TextView hijriDateText, yaklasanaTarih, yaklasanaTarihDetay;
    private ImageView islamicDayImage;
    private RecyclerView diniGunlerRecycler;
    private String getCityNameFromPreferences;
    private ProgressBar loadingDialog;
    private Spinner yearSelectSpinner;
    private List<String> years;
    private List<IslamicDayResponse> islamicDayResponses;
    private IslamicDayAdapter diniGunAdapter;
    private NetworkServiceUtil networkServiceUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_islamic_days);

        initializeViews();
        initializeServices();

        setupRecyclerView();

        loadGithubDiniGunler();

        yearSelectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selectedYear = Integer.parseInt(years.get(position));

                // Yıl filtreleme ve RecyclerView güncelleme
                List<DiniGun> filteredList = convertToDiniGun(filterByYear(selectedYear));
                diniGunAdapter.updateData(filteredList); // Adapter içinde updateData metodu olmal
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void initializeServices() {
        networkServiceUtil = NetworkServiceUtil.getInstance(this);
        if (!networkServiceUtil.isConnected()) {
            // veya kullanıcıya devam/iptal seçeneği ver
            networkServiceUtil.showConfirmNoInternetDialog(this, getString(R.string.no_Internet), getString(R.string.takvim_local_uyari), new NetworkServiceUtil.OnUserChoiceListener() {
                @Override
                public void onContinue() {
                    // Kullanıcı internet olmadan devam etti
                    loadLocalDiniGunler();
                    loadingDialog.setVisibility(View.GONE);
                }

                @Override
                public void onCancel() {
                    // Kullanıcı iptal etti
                    finish();
                }
            });
        }
    }

    private void initializeViews() {
//        cityNameText = findViewById(R.id.city_name);
        hijriDateText = findViewById(R.id.hijri_date);
        islamicDayImage = findViewById(R.id.islamic_day_image);
        diniGunlerRecycler = findViewById(R.id.dini_gunler_recycler);

        yaklasanaTarih = findViewById(R.id.yaklasanaTarih);
        yaklasanaTarihDetay = findViewById(R.id.yaklasanaTarihDetay);
        loadingDialog = findViewById(R.id.loadingDialog);
        yearSelectSpinner = findViewById(R.id.yearSelectSpinner);


        hijriDateText.setText(NamazVakiti.getInstance().getHicriDate());

    }

    private void setupRecyclerView() {
        diniGunlerRecycler.setLayoutManager(new LinearLayoutManager(this));
        // Adapter setup will be done when data is loaded
    }


    private void loadGithubDiniGunler() {
        APIService apiService = APIClient.getClient().create(APIService.class);

        apiService.getIslamicDaysList().enqueue(new retrofit2.Callback<List<IslamicDayResponse>>() {
            @Override
            public void onResponse(Call<List<IslamicDayResponse>> call, Response<List<IslamicDayResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // İşlemleri burada gerçekleşir
                    islamicDayResponses = response.body();
                    islamicDayResponses.sort(Comparator.comparingInt(IslamicDayResponse::getYil));
                    int currentYil = LocalDate.now().getYear(); // Şu anki yıl

                    if (!islamicDayResponses.isEmpty()) {
                        IslamicDayResponse lastItem = islamicDayResponses.get(islamicDayResponses.size() - 1);
                        int lastYil = lastItem.getYil();
                        // secilebilir Yıl listesini oluştur
                        years = new ArrayList<>();
                        for (int i = currentYil; i <= lastYil; i++) {
                            years.add(String.valueOf(i));
                        }
                        // Adapter oluştur ve Spinner'a ata
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(IslamicDayActivity.this,
                                android.R.layout.simple_spinner_item, years);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        yearSelectSpinner.setAdapter(adapter);
                        yearSelectSpinner.setSelection(years.indexOf(String.valueOf(currentYil)));

                        if (lastYil < currentYil) {
                            loadLocalDiniGunler();
                            loadingDialog.setVisibility(View.GONE);
                            return;
                        }

                    } else {
                        loadLocalDiniGunler();
                        loadingDialog.setVisibility(View.GONE);
                        return;
                    }
                    List<DiniGun> diniGunList;
                    // RecyclerView adapter ilk yükleme (tüm günler değil, seçilen yıl)
                    diniGunList = convertToDiniGun(filterByYear(currentYil));

//                    for (IslamicDayResponse islamicDayResponse : islamicDayResponses) {
//                        DiniGun diniGun = new DiniGun(DiniGunEnum.fromValue(islamicDayResponse.getDiniGun()), islamicDayResponse.getDiniTarih());
//                        diniGun.setIslamicDayResponse(islamicDayResponse);
//                        diniGunList.add(diniGun);
//                    }
                    siralamaYap(diniGunList);
                    diniGunAdapter = new IslamicDayAdapter(diniGunList);
                    diniGunlerRecycler.setAdapter(diniGunAdapter);
                    loadingDialog.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<List<IslamicDayResponse>> call, Throwable t) {
                Log.d("servis hata", Objects.requireNonNull(t.getMessage()));
                loadingDialog.setVisibility(View.GONE);

            }
        });

    }

    private List<IslamicDayResponse> filterByYear(int year) {
        List<IslamicDayResponse> list = new ArrayList<>();
        for (IslamicDayResponse day : islamicDayResponses) {
            if (day.getYil() == year) {
                list.add(day);
            }
        }
        return list;
    }

    private List<DiniGun> convertToDiniGun(List<IslamicDayResponse> responses) {
        List<DiniGun> diniGunList = new ArrayList<>();

        for (IslamicDayResponse response : responses) {
            DiniGun diniGun = new DiniGun(DiniGunEnum.fromValue(response.getDiniGun()), response.getDiniTarih());
            diniGun.setIslamicDayResponse(response);
            diniGunList.add(diniGun);
        }
        if (!diniGunList.isEmpty() && LocalDate.now().getYear() == diniGunList.get(0).getIslamicDayResponse().getYil())
            yaklasanTarihGuncelle(diniGunList);
        return diniGunList;
    }


    private void loadLocalDiniGunler() {
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
//        hijriCalendar.add(IslamicCalendar.YEAR, 1); // Yılı 1 artır
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


        siralamaYap(diniGunList);
        IslamicDayAdapter adapter = new IslamicDayAdapter(diniGunList);
        diniGunlerRecycler.setAdapter(adapter);

        yaklasanTarihGuncelle(diniGunList);
    }

    private void yaklasanTarihGuncelle(List<DiniGun> diniGunList) {
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
                    yaklasanaTarih.setText(this.getString(R.string.yaklasanTarihYok));
                    yaklasanaTarihDetay.setText(this.getString(R.string.yaklasanTarihYok));
                }
            } catch (ParseException ignored) {
            }
        }
    }

    private void siralamaYap(List<DiniGun> diniGunList) {
        // Gün adını dikkate almadan Türkçe tarihleri parse edebilmek için formatter
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("tr")); // 'd' tek haneli günleri de kapsar

        diniGunList.sort(Comparator.comparing(d -> {
            try {
                // "2 Şubat 2026 Pazartesi" → "2 Şubat 2026"
                String temizTarih = d.getDiniTarih()
                        .trim()
                        .replaceAll("\\s+[A-ZÇĞİÖŞÜa-zçğıöşü]+$", ""); // sondaki gün adını sil

                return LocalDate.parse(temizTarih, formatter);

            } catch (Exception e) {
                // Hatalı tarih varsa uzak geleceğe al, sıralamada sona düşsün
                return LocalDate.of(9999, 12, 31);
            }
        }));

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
            imageResource = R.drawable.islamic_day_11;
        } else if (diniGun.getDiniGun().equals(DiniGunEnum.MEVLID_KANDILI)) {
            imageResource = R.drawable.islamic_day_10;
        } else {
            imageResource = R.drawable.islamic_day_1;
        }
        islamicDayImage.setImageResource(imageResource);
    }


}
