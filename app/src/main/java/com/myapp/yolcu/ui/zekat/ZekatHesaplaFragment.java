package com.myapp.yolcu.ui.zekat;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.myapp.yolcu.R;
import com.myapp.yolcu.ui.Service.SoundService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class ZekatHesaplaFragment extends Fragment {

    private Spinner spinnerVarlikTuru, spinnerAltTuru;
    private EditText editTextTutar, kurBilgileriTutar, nisapTutarUser;
    private Button buttonVarlikEkle;
    private ListView listViewVarliklar;
    private TextView textViewZekatSonuc, toplamVarlik, spinnerAltTuruLabel, kurBilgileriTutarLabel, spinnerIconLabel;
    private ArrayAdapter<String> listAdapter;
    private final List<String> varlikListesi = new ArrayList<>();
    private ImageButton buttonPaylas;
    private ImageView spinnerAltIcon;
    private CheckBox kurBilgileriDegistirCheckBox;
    private SoundService soundService;
    private RadioGroup radioGroupVarlikTipi;
    private boolean varlikMi = true;
    private boolean servisAktifMi = true;
    private final BigDecimal nisapOran = new BigDecimal("80.18");
    private BigDecimal nisapTutar = BigDecimal.ZERO;
    private Dialog mDialog;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_zekat_hesapla, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        initializeServices();
        setupVarlikTuruSpinner();
        setupEditTextFormatting();
        setupVarlikEkleButton();
        nisapMiktarServistenAl();

        listAdapter = new CustomArrayAdapter(requireContext(), varlikListesi, this);
        listViewVarliklar.setAdapter(listAdapter);
        kurBilgileriDegistirCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> checkControl(isChecked));
        buttonPaylas.setOnClickListener(v -> shareData());
        radioGroupVarlikTipi.setOnCheckedChangeListener((group, checkedId) -> changeRadioButton(checkedId));

    }

    private void nisapMiktarServistenAl() {
        CurrencyParserAPI.fetchCurrencies("GetGold", currencies -> {
            if (!currencies.isEmpty()) {
                servisAktifMi = true;
                for (Currency currency : currencies) {
                    if (currency.getAciklama().contains("Gram Altın")) {
                        nisapTutar = nisapOran.multiply(BigDecimal.valueOf(currency.getSatis()));
                    }
                }
            } else
                servisAktifMi = false;
            if (nisapTutar.compareTo(BigDecimal.ZERO) == 0)
                showNisapInfoPopup();
        });
    }

    private void showNisapInfoPopup() {
        mDialog.setCancelable(false);
        Button closeButton = mDialog.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(view -> {
            if (!nisapTutarUser.getText().toString().isEmpty() && new BigDecimal((nisapTutarUser.getText().toString().replace(".", "")).replace(",", ".")).compareTo(BigDecimal.ZERO) > 0) {
                nisapTutar = new BigDecimal((nisapTutarUser.getText().toString().replace(".", "")).replace(",", "."));
                mDialog.dismiss();
            } else {
                Toast.makeText(requireContext(), "Geçersiz değer girildi!", Toast.LENGTH_SHORT).show();
            }
        });

        mDialog.show();
    }

    private void initializeServices() {
        soundService = new SoundService(requireContext());
        soundService.commonButtonEffect();
    }

    private void initializeViews(View view) {
        spinnerVarlikTuru = view.findViewById(R.id.spinnerVarlikTuru);
        spinnerAltTuru = view.findViewById(R.id.spinnerAltTuru);
        editTextTutar = view.findViewById(R.id.editTextTutar);
        buttonVarlikEkle = view.findViewById(R.id.buttonVarlikEkle);
        listViewVarliklar = view.findViewById(R.id.listViewVarliklar);
        textViewZekatSonuc = view.findViewById(R.id.textViewZekatSonuc);
        toplamVarlik = view.findViewById(R.id.toplamVarlik);
        spinnerAltTuruLabel = view.findViewById(R.id.spinnerAltTuruLabel);
        spinnerAltIcon = view.findViewById(R.id.spinnerAltIcon);
        kurBilgileriTutar = view.findViewById(R.id.kurBilgileriTutar);
        kurBilgileriDegistirCheckBox = view.findViewById(R.id.kurBilgileriDegistirCheckBox);
        kurBilgileriTutarLabel = view.findViewById(R.id.kurBilgileriTutarLabel);
        radioGroupVarlikTipi = view.findViewById(R.id.radioGroupVarlikTipi);
        spinnerIconLabel = view.findViewById(R.id.spinnerIconLabel);

        // Butonları bağla
        buttonPaylas = view.findViewById(R.id.buttonPaylas);

        // Yeni bir Dialog oluştur
        mDialog = new Dialog(requireContext());
        Objects.requireNonNull(mDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        // custom_popup.xml layoutunu dialog'a yükle
        mDialog.setContentView(R.layout.zekat_nisap_popup);
        // Dialog dışına tıklanıldığında kapansın
        nisapTutarUser = mDialog.findViewById(R.id.nisapTutarUser);


    }

    private void checkControl(boolean isChecked) {
        kurBilgileriTutar.setEnabled(isChecked);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Spinner'ı yeniden yapılandır ve ilk öğeyi seç
        if (spinnerVarlikTuru.getAdapter() != null) {
            spinnerVarlikTuru.setSelection(0);
        }
    }

    private void setupVarlikTuruSpinner() {
        ArrayAdapter<CharSequence> adapter = getCharSequenceArrayAdapter();
        spinnerVarlikTuru.setAdapter(adapter);
        String kurDegeri = "0.00";
        spinnerVarlikTuru.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 1) { // Döviz
                    // GetCurrency çağrısı
                    if (servisAktifMi) {
                        fetchCurrencyDataAndPopulateSpinner("GetCurrency", spinnerAltTuru);
                        setupAltTuruSpinner();
                    } else {
                        //burada servis çalışmaz ise default veri gelsin kullanıcı kendi belirlesin kuru
                        ArrayAdapter<CharSequence> dovizAdapter = new ArrayAdapter<CharSequence>(
                                requireContext(),
                                android.R.layout.simple_spinner_item,
                                getResources().getStringArray(R.array.doviz_turu)) {
                            @Override
                            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                                View view = super.getDropDownView(position, convertView, parent);
                                TextView text = view.findViewById(android.R.id.text1);

                                // Yazı rengini siyah yapmak
                                if (text != null) {
                                    text.setTextColor(ContextCompat.getColor(requireContext(), R.color.black)); // Dropdown menü yazı rengini ayarlama
                                }
                                return view;
                            }
                        };
                        dovizAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerAltTuru.setAdapter(dovizAdapter);
                        kurBilgileriDegistirCheckBox.setChecked(true);
                        kurBilgileriTutar.setText(kurDegeri);
                    }


                    spinnerAltTuru.setVisibility(View.VISIBLE);
                    spinnerAltTuruLabel.setText(getString(R.string.dovizTuru));
                    spinnerAltTuruLabel.setVisibility(View.VISIBLE);
                    spinnerAltIcon.setVisibility(View.VISIBLE);
                    kurBilgileriTutarLabel.setVisibility(ViewGroup.VISIBLE);
                    kurBilgileriTutar.setVisibility(ViewGroup.VISIBLE);
                    kurBilgileriDegistirCheckBox.setVisibility(ViewGroup.VISIBLE);
                    // Currency verilerini almak için AsyncTask başlatma


//                    setKurBilgileri(spinnerAltTuru.getSelectedItem().toString()); // Örnek olarak dolar

                } else if (position == 2) { // Altın
                    if (servisAktifMi) {
                        fetchCurrencyDataAndPopulateSpinner("GetGold", spinnerAltTuru);
                        setupAltTuruSpinner();
                    } else {
                        //burada servis çalışmaz ise default veri gelsin kullanıcı kendi belirlesin kuru
                        ArrayAdapter<CharSequence> altinAdapter = new ArrayAdapter<CharSequence>(
                                requireContext(),
                                android.R.layout.simple_spinner_item,
                                getResources().getStringArray(R.array.altin_turu)) {
                            @Override
                            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                                View view = super.getDropDownView(position, convertView, parent);
                                TextView text = view.findViewById(android.R.id.text1);

                                // Yazı rengini siyah yapmak
                                if (text != null) {
                                    text.setTextColor(ContextCompat.getColor(requireContext(), R.color.black)); // Dropdown menü yazı rengini ayarlama
                                }
                                return view;
                            }
                        };
                        altinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerAltTuru.setAdapter(altinAdapter);
                        kurBilgileriDegistirCheckBox.setChecked(true);
                        kurBilgileriTutar.setText(kurDegeri);
                    }
                    spinnerAltTuru.setVisibility(View.VISIBLE);
                    spinnerAltTuruLabel.setText(getString(R.string.altinTuru));
                    spinnerAltTuruLabel.setVisibility(View.VISIBLE);
                    spinnerAltIcon.setVisibility(View.VISIBLE);
                    kurBilgileriTutarLabel.setVisibility(ViewGroup.VISIBLE);
                    kurBilgileriTutar.setVisibility(ViewGroup.VISIBLE);
                    kurBilgileriDegistirCheckBox.setVisibility(ViewGroup.VISIBLE);

//                    GetGold çağrısı

                } else {
                    spinnerAltTuru.setVisibility(View.GONE);
                    spinnerAltTuruLabel.setVisibility(View.GONE);
                    spinnerAltIcon.setVisibility(View.GONE);
                    kurBilgileriTutarLabel.setVisibility(ViewGroup.GONE);
                    kurBilgileriTutar.setVisibility(ViewGroup.GONE);
                    kurBilgileriDegistirCheckBox.setVisibility(ViewGroup.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }


    private void fetchCurrencyDataAndPopulateSpinner(String requestType, Spinner spinner) {
        CurrencyParserAPI.fetchCurrencies(requestType, new CurrencyParserAPI.CurrencyCallback() {
            @Override
            public void onCurrenciesFetched(List<Currency> currencies) {
                if (currencies != null && !currencies.isEmpty()) {
                    // Spinner'ı doldur
                    List<String> currencyNames = new ArrayList<>();
                    Iterator<Currency> iterator = currencies.iterator();

                    while (iterator.hasNext()) {
                        Currency currency = iterator.next();
                        if (currency.getAciklama().contains("Toptan") || currency.getAciklama().contains("Eski")) {
                            iterator.remove(); // Güvenli şekilde öğeyi kaldır
                        } else {
                            if (requestType.equals("GetCurrency"))
                                currencyNames.add(currency.getAciklama() + "-" + currency.getKod()); // Currency adını alın
                            else
                                currencyNames.add(currency.getAciklama());
                        }
                    }

                    spinner.setTag(currencies);

                    // UI Thread'de Spinner'a veri ekleme
                    requireActivity().runOnUiThread(() -> {
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                                requireActivity(),
                                android.R.layout.simple_spinner_item, currencyNames) {
                            @Override
                            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                                View view = super.getDropDownView(position, convertView, parent);
                                TextView text = view.findViewById(android.R.id.text1);

                                // Yazı rengini siyah yapmak
                                if (text != null) {
                                    text.setTextColor(ContextCompat.getColor(requireContext(), R.color.black)); // Dropdown menü yazı rengini ayarlama
                                }
                                return view;
                            }
                        };

                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinner.setAdapter(adapter);
                    });
                } else {
                    Log.d("ZekatHesaplaFragment", "Boş veya geçersiz liste.");
                }
            }
        });
    }
    @SuppressWarnings("unchecked")
    private void setupAltTuruSpinner() {
        spinnerAltTuru.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                List<Currency> currencies = (List<Currency>) spinnerAltTuru.getTag();
                if (currencies != null && position < currencies.size()) {
                    Currency selectedCurrency = currencies.get(position);
                    String satisSayisi = String.valueOf(selectedCurrency.getSatis());
                    satisSayisi = formatCurrency(new BigDecimal(satisSayisi));
                    kurBilgileriTutar.setText(satisSayisi); // Satis değerini TextView'e yaz
                }
                String ziynetTip = "Tutar";
                if (spinnerAltTuru.getSelectedItem().toString().contains("Bilezik") || spinnerAltTuru.getSelectedItem().toString().contains("Hurda") || spinnerAltTuru.getSelectedItem().toString().contains("Gümüş") || spinnerAltTuru.getSelectedItem().toString().contains("Ayar"))
                    ziynetTip = "Gram";
                else if (spinnerVarlikTuru.getSelectedItem().toString().equals("Altın")) {
                    ziynetTip = "Adet";
                }
                editTextTutar.setHint(ziynetTip);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }


    private @NonNull ArrayAdapter<CharSequence> getCharSequenceArrayAdapter() {
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.varlik_turu)) {
            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView text = view.findViewById(android.R.id.text1);

                // Yazı rengini siyah yapmak
                if (text != null) {
                    text.setTextColor(ContextCompat.getColor(requireContext(), R.color.black)); // Dropdown menü yazı rengini ayarlama
                }
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }


    private void setupEditTextFormatting() {
        setupTextWatcher(editTextTutar);
        setupTextWatcher(kurBilgileriTutar);
        setupTextWatcher(nisapTutarUser);
    }

    private void setupTextWatcher(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            private String currentText = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(currentText)) {
                    editText.removeTextChangedListener(this);

                    try {
                        // Kullanıcının girdiği metni temizle
                        String cleanString = s.toString().replaceAll("[,.]", "");

                        // Ondalıklı sayıya dönüştür
                        BigDecimal parsed = new BigDecimal(cleanString).divide(new BigDecimal(100), 2, RoundingMode.DOWN);

                        // Formatlanmış metni EditText'e koy
                        currentText = formatCurrency(parsed);
                        editText.setText(currentText);
                        editText.setSelection(currentText.length()); // İmleci sona taşı
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }

                    editText.addTextChangedListener(this);
                }
            }
        });
    }


    // Formatlama işlemi için yardımcı metot
    private String formatCurrency(BigDecimal amount) {
        // DecimalFormatSymbols ile özel ayırıcıları tanımlayalım
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.'); // Binlik ayırıcı nokta olacak
        symbols.setDecimalSeparator(','); // Ondalık ayırıcı virgül olacak

        // DecimalFormat ile formatlama
        DecimalFormat formatter = new DecimalFormat("#,##0.00", symbols);

        // BigDecimal'i string'e dönüştürüp formatı uygulayalım

        return formatter.format(amount);
    }

    private void setupVarlikEkleButton() {
        buttonVarlikEkle.setOnClickListener(v -> {
            String tutarStr = editTextTutar.getText().toString().replaceAll("[,.]", "");
            try {
                // Tutarı sayıya dönüştür
                BigDecimal tutar = new BigDecimal(tutarStr);
                BigDecimal kurDegeri = BigDecimal.ZERO;
                if (tutar.compareTo(BigDecimal.ZERO) == 0)
                    throw new NumberFormatException();
                tutar = tutar.divide(new BigDecimal(100), 2, RoundingMode.DOWN);  // Kullanıcı inputunu kuruş olarak alıyoruz
                String adetTutar = formatCurrency(tutar);
                ;
                if (kurBilgileriTutar != null && kurBilgileriTutar.getText() != null && !kurBilgileriTutar.getText().toString().isEmpty() && (spinnerVarlikTuru.getSelectedItem().toString().equals("Döviz") || spinnerVarlikTuru.getSelectedItem().toString().equals("Altın"))) {
                    kurDegeri = new BigDecimal(kurBilgileriTutar.getText().toString().replaceAll("[.]", "").replaceAll(",", "."));
                }
                if ( kurDegeri.compareTo(BigDecimal.ZERO) > 0) {
                    tutar = tutar.multiply(new BigDecimal(kurBilgileriTutar.getText().toString().replaceAll("[.]", "").replaceAll(",", ".")));
                } else if (spinnerVarlikTuru.getSelectedItemId()!=0) {
                    Toast.makeText(getContext(), "Geçersiz kur değeri!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (tutar.toPlainString().length() > 14) {
                    Toast.makeText(getContext(), "Geçersiz büyüklükte sayı değeri girildi!", Toast.LENGTH_SHORT).show();
                    return;
                }

                String formattedTutar = formatCurrency(tutar); // Formatlama işlemi

                // Varlık türlerini al
                String varlikTuru = spinnerVarlikTuru.getSelectedItem().toString();
                String altTuru = spinnerAltTuru.getVisibility() == View.VISIBLE
                        ? spinnerAltTuru.getSelectedItem().toString()
                        : "";

                String varlik;
                // Formatlanmış tutarı ve türü birleştir
                if (spinnerAltTuru.getVisibility() == View.VISIBLE) {
                    String ziynetTip = "Tutar:";
                    if (spinnerAltTuru.getSelectedItem().toString().contains("Bilezik") || spinnerAltTuru.getSelectedItem().toString().contains("Hurda") || spinnerAltTuru.getSelectedItem().toString().contains("Gümüş") || spinnerAltTuru.getSelectedItem().toString().contains("Ayar"))
                        ziynetTip = "Gram:";
                    else if (spinnerVarlikTuru.getSelectedItem().toString().equals("Altın")) {
                        ziynetTip = "Adet:";
                    }
                    varlikTuru = varlikTuru + (altTuru.isEmpty() ? "" : " - " + altTuru);
                    if (altTuru.equals("Gümüş")) {
                        varlikTuru = altTuru;
                    }
                    varlik = varlikTuru + (!varlikMi ? "- Borç" : "") + ":\n" + "Kur değeri:" + kurBilgileriTutar.getText().toString() + "\n" + ziynetTip + adetTutar + "\n#" + formattedTutar + "₺";
                } else {
                    varlik = varlikTuru + (!varlikMi ? "- Borç" : "") + ":\n" + "#" + formattedTutar + "₺";

                }
                if (varlikMi) {
                    varlik = varlik + "?1";
                } else
                    varlik = varlik + "?0";


                // Yeni öğeyi listeye başa ekliyoruz
                varlikListesi.add(0, varlik);
                listAdapter.notifyDataSetChanged();  // Listeyi güncelle
                editTextTutar.setText("");  // Text'i temizle

                calculateZekat();  // Hesaplama işlemi

            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Geçerli bir tutar girin!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void calculateZekat() {
        BigDecimal toplamTutar = BigDecimal.ZERO;
        BigDecimal toplamTutarBorc = BigDecimal.ZERO;
        for (String varlik : varlikListesi) {
            String[] parts = varlik.split("#");
            if (parts.length > 1) {
                // Formatlı tutarı, binlik ve ondalık ayırıcıları düzgün şekilde işleyelim
                String formattedTutar = parts[1].substring(0, parts[1].length() - 1).replaceAll("[^0-9,]", ""); // Sadece sayıyı al
                formattedTutar = formattedTutar.replace(",", "."); // Virgülü noktaya çevir

                try {
                    // String'i BigDecimal'a çevirirken noktaları doğru şekilde işleyelim
                    BigDecimal tutar = new BigDecimal(formattedTutar);
                    if (varlik.contains("Borç"))
                        toplamTutarBorc = toplamTutarBorc.add(tutar);
                    else
                        toplamTutar = toplamTutar.add(tutar);

                } catch (NumberFormatException e) {
                    // Geçerli bir sayı olmadığı durumda hata veririz
                    e.printStackTrace();
                }
            }
        }
        BigDecimal total = toplamTutar.subtract(toplamTutarBorc);
        if (total.signum() < 0)
            total = BigDecimal.ZERO;
        // Toplam tutarı bir textview'de gösterelim
        String formattedToplamTutar = formatCurrency(total); // Formatlama işlemi
        toplamVarlik.setText("Toplam Varlik: " + formattedToplamTutar + " ₺");

        if (total.compareTo(nisapTutar) >= 0) {// 80.18 gram altın eşik değeri
            BigDecimal zekat = total.multiply(new BigDecimal("0.025"));
            textViewZekatSonuc.setText("Zekat tutar: " + formatCurrency(zekat) + " ₺");
            textViewZekatSonuc.setVisibility(View.VISIBLE);
        } else {
            textViewZekatSonuc.setVisibility(View.GONE);
        }
    }

    private void changeRadioButton(int checkedId) {
        if (checkedId == R.id.radioButtonVarliklar) {
            // Varlıklar seçildiğinde yapılacak işlemler
            spinnerIconLabel.setText("Varlık Türü");
            buttonVarlikEkle.setText("Varlık Ekle");
            varlikMi = true;

        } else if (checkedId == R.id.radioButtonBorclar) {
            // Borçlar seçildiğinde yapılacak işlemler
            spinnerIconLabel.setText("Borç Türü");
            buttonVarlikEkle.setText("Borç Ekle");
            varlikMi = false;
        }
    }


    private void shareData() {
        StringBuilder message = new StringBuilder();
        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        String formattedDateTime = currentDateTime.format(formatter);
        message.append("Tarih: " + formattedDateTime);
        // Varlık ve borç listelerini başlat
        List<String> varlikListesiFormatted = new ArrayList<>();
        List<String> borcListesiFormatted = new ArrayList<>();

// Listeleri ayır
        for (String varlik : varlikListesi) {
            if (varlik.contains("?1")) {
                varlikListesiFormatted.add(varlik.replace("?1", "")); // "?1" kaldırılıyor
            } else if (varlik.contains("?0")) {
                borcListesiFormatted.add(varlik.replace("?0", "")); // "?0" kaldırılıyor
            }
        }

// Varlık Listesi ekleniyor
        if (!varlikListesiFormatted.isEmpty()) {
            message.append("\n***Varlık Listesi***\n");
            for (String varlik : varlikListesiFormatted) {
                message.append(varlik).append("\n____________\n");
            }
        }

// Borç Listesi ekleniyor
        if (!borcListesiFormatted.isEmpty()) {
            message.append("\n***Borç Listesi***\n");
            for (String borc : borcListesiFormatted) {
                message.append(borc).append("\n____________\n");
            }
        }

        // Toplam Varlık ve Zekat verilerini ekle
        String toplamTutarText = toplamVarlik.getText().toString();
        String zekatTutarText = textViewZekatSonuc.getText().toString();

        message.append("\n").append(toplamTutarText).append("\n");
        if (textViewZekatSonuc.getVisibility() == View.VISIBLE) {
            message.append(zekatTutarText).append("\n");
        }

        // Paylaşmak için Intent oluştur
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, message.toString());

        // WhatsApp gibi bir uygulama ile paylaşmak
        startActivity(Intent.createChooser(shareIntent, "Paylaş"));
    }
}
