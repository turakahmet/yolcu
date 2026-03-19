package com.islamyolu.app.zekat;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CurrencyParserAPI {

    // Yeni REST URL'leri
    private static final String GOLD_URL = "https://static.altinkaynak.com/public/Gold";
    private static final String CURRENCY_URL = "https://static.altinkaynak.com/public/Currency";

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface CurrencyCallback {
        void onCurrenciesFetched(List<Currency> currencies);
    }

    public static void fetchCurrencies(String requestType, CurrencyCallback callback) {
        executorService.submit(() -> {
            List<Currency> currencies = new ArrayList<>();
            try {
                // İsteği at ve JSON cevabını işle
                currencies = performRestRequest(requestType);
            } catch (Exception e) {
                Log.e("CurrencyParserAPI", "Veri çekilirken hata oluştu: " + e.getMessage());
                e.printStackTrace();
            }

            // Sonuçları Main Thread (UI) üzerinde callback ile döndür
            List<Currency> finalCurrencies = currencies;
            mainHandler.post(() -> callback.onCurrenciesFetched(finalCurrencies));
        });
    }

    private static List<Currency> performRestRequest(String requestType) throws Exception {
        // Gelen requestType'a göre ilgili URL'yi seçiyoruz
        String targetUrl = "GetCurrency".equals(requestType) ? CURRENCY_URL : GOLD_URL;

        URL url = new URL(targetUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET"); // Artık sadece GET isteği atıyoruz
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);

        List<Currency> currencies = new ArrayList<>();

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            InputStream inputStream = connection.getInputStream();
            StringBuilder responseBuilder = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
            }

            String jsonResponse = responseBuilder.toString();
            currencies = parseJsonResponse(jsonResponse);
        } else {
            throw new Exception("HTTP Error: " + connection.getResponseCode());
        }

        connection.disconnect();
        return currencies;
    }

    private static List<Currency> parseJsonResponse(String jsonResponse) {
        List<Currency> currencies = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(jsonResponse);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                Currency currency = new Currency();

                if (jsonObject.has("Kod")) currency.setKod(jsonObject.getString("Kod"));
                if (jsonObject.has("Aciklama"))
                    currency.setAciklama(jsonObject.getString("Aciklama"));

                // Not: Eski kodda XML'den "Alis" okunup setSatis yapılmıştı.
                // Yeni yapıda ikisini de ayrı ayrı set ediyoruz. Eğer modelinizde setAlis yoksa eklemeniz iyi olur.
                if (jsonObject.has("Alis")) {
                    currency.setAlis(parsePrice(jsonObject.getString("Alis")));
                }
                if (jsonObject.has("Satis")) {
                    currency.setSatis(parsePrice(jsonObject.getString("Satis")));
                }

                currencies.add(currency);
            }
        } catch (Exception e) {
            Log.e("CurrencyParserAPI", "JSON Parse Hatası: " + e.getMessage());
            e.printStackTrace();
        }
        return currencies;
    }

    /**
     * "46.771,43" formatındaki string veriyi, Java'nın anlayabileceği
     * 46771.43 Double formatına çeviren yardımcı metod.
     */
    private static double parsePrice(String priceStr) {
        try {
            if (priceStr == null || priceStr.isEmpty()) return 0.0;
            String formattedPrice = priceStr.replace(".", "").replace(",", ".");
            return Double.parseDouble(formattedPrice);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}