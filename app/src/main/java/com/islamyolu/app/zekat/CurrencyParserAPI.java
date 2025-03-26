package com.islamyolu.app.zekat;

import android.os.Handler;
import android.os.Looper;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CurrencyParserAPI {

    private static final String SERVICE_URL = "http://data.altinkaynak.com/DataService.asmx";
    private static final String USERNAME = "AltinkaynakWebServis";
    private static final String PASSWORD = "AltinkaynakWebServis";

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());


    public interface CurrencyCallback {
        void onCurrenciesFetched(List<Currency> currencies);
    }

    public static void fetchCurrencies(String requestType, CurrencyCallback callback) {
        executorService.submit(() -> {
            List<Currency> currencies = new ArrayList<>();
            try {
                // SOAP isteği oluştur ve gönder
                currencies = performSoapRequest(requestType);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Sonuçları callback ile döndür
            List<Currency> finalCurrencies = currencies;
            mainHandler.post(() -> callback.onCurrenciesFetched(finalCurrencies));
        });
    }


    private static List<Currency> performSoapRequest(String requestType) throws Exception {
        List<Currency> currencies = new ArrayList<>();

        String soapRequest = buildSoapRequest(requestType);

        URL url = new URL(SERVICE_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/soap+xml; charset=utf-8");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(soapRequest.getBytes());
            os.flush();
        }

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) { // 200 yerine sabit kullanımı tercih edilir
            InputStream inputStream = connection.getInputStream();

            // InputStream'den veriyi okuma
            StringBuilder responseBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
            }

            String response = responseBuilder.toString();

            // XML verilerini işleme
            String rawXml = extractInnerXml(response, requestType + "Result");
            String decodedXml = decodeXml(rawXml);
            currencies = parseXmlResponse(decodedXml);
        } else {
            throw new Exception("HTTP Error: " + connection.getResponseCode());
        }


        connection.disconnect();
        return currencies;
    }

    private static String buildSoapRequest(String requestType) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<soap12:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
                "xmlns:soap12=\"http://www.w3.org/2003/05/soap-envelope\">" +
                "<soap12:Header>" +
                "<AuthHeader xmlns=\"http://data.altinkaynak.com/\">" +
                "<Username>" + USERNAME + "</Username>" +
                "<Password>" + PASSWORD + "</Password>" +
                "</AuthHeader>" +
                "</soap12:Header>" +
                "<soap12:Body>" +
                "<" + requestType + " xmlns=\"http://data.altinkaynak.com/\" />" +
                "</soap12:Body>" +
                "</soap12:Envelope>";
    }

    private static String extractInnerXml(String xml, String tagName) {
        String startTag = "<" + tagName + ">";
        String endTag = "</" + tagName + ">";
        int startIndex = xml.indexOf(startTag) + startTag.length();
        int endIndex = xml.indexOf(endTag);
        if (startIndex >= startTag.length() && endIndex > startIndex) {
            return xml.substring(startIndex, endIndex);
        }
        return "";
    }

    private static String decodeXml(String xml) {
        return xml.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&apos;", "'");
    }

    public static List<Currency> parseXmlResponse(String response) {
        List<Currency> currencies = new ArrayList<>();
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(response));

            String tagName = null;
            Currency currency = null;

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        tagName = parser.getName();
                        if ("Kur".equals(tagName)) {
                            currency = new Currency();
                        } else if (currency != null) {
                            if ("Kod".equals(tagName)) {
                                currency.setKod(parser.nextText());
                            } else if ("Aciklama".equals(tagName)) {
                                currency.setAciklama(parser.nextText());
                            } else if ("Alis".equals(tagName)) {
                                currency.setSatis(Double.parseDouble(parser.nextText()));
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if ("Kur".equals(parser.getName()) && currency != null) {
                            currencies.add(currency);
                        }
                        break;
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return currencies;
    }

}
