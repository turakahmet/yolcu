package com.islamyolu.app.prayertimes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * times/{cityId}.json ayrıştırıcısı.
 * "days" satırları dizidir; sütunlar sabit indeksle değil "fields" içindeki adlarla eşlenir.
 * Bilinmeyen alanlar yok sayılır, zorunlu alan eksikse dosya reddedilir.
 * Yalnızca biçimi okur; içerik kuralları {@link PrayerTimesValidator} içindedir.
 */
public final class PrayerTimesParser {

    static final String FIELD_DATE = "date";
    static final String FIELD_FAJR = "fajr";
    static final String FIELD_SUNRISE = "sunrise";
    static final String FIELD_DHUHR = "dhuhr";
    static final String FIELD_ASR = "asr";
    static final String FIELD_MAGHRIB = "maghrib";
    static final String FIELD_ISHA = "isha";
    static final String FIELD_HIJRI = "hijri";
    static final String FIELD_QIBLA = "qibla";
    static final String FIELD_GMT = "gmt";

    private static final String[] REQUIRED_FIELDS = {
            FIELD_DATE, FIELD_FAJR, FIELD_SUNRISE, FIELD_DHUHR,
            FIELD_ASR, FIELD_MAGHRIB, FIELD_ISHA, FIELD_GMT
    };

    @NonNull
    public CityTimesDto parse(@NonNull String json) throws PrayerTimesFormatException {
        JsonObject root;
        try {
            JsonElement element = JsonParser.parseString(json);
            if (!element.isJsonObject()) {
                throw new PrayerTimesFormatException("Kök öğe nesne değil");
            }
            root = element.getAsJsonObject();
        } catch (JsonParseException e) {
            throw new PrayerTimesFormatException("Geçersiz JSON", e);
        }

        Map<String, Integer> columns = readColumns(requireArray(root, "fields"));
        for (String field : REQUIRED_FIELDS) {
            if (!columns.containsKey(field)) {
                throw new PrayerTimesFormatException("Zorunlu alan eksik: " + field);
            }
        }

        JsonArray rows = requireArray(root, "days");
        List<PrayerDayDto> days = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            days.add(readDay(rows.get(i), columns, i));
        }

        return new CityTimesDto(
                requireInt(root, "version"),
                requireInt(root, "cityId"),
                optString(root, "name"),
                optString(root, "state"),
                optInt(root, "stateId"),
                optString(root, "country"),
                optInt(root, "countryId"),
                optString(root, "updatedAt"),
                requireString(root, "validFrom"),
                requireString(root, "validTo"),
                days);
    }

    private Map<String, Integer> readColumns(JsonArray fields) throws PrayerTimesFormatException {
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < fields.size(); i++) {
            JsonElement field = fields.get(i);
            if (!isString(field)) {
                throw new PrayerTimesFormatException("fields[" + i + "] metin değil");
            }
            String name = field.getAsString();
            if (columns.containsKey(name)) {
                throw new PrayerTimesFormatException("Tekrar eden alan: " + name);
            }
            columns.put(name, i);
        }
        return columns;
    }

    private PrayerDayDto readDay(JsonElement rowElement, Map<String, Integer> columns, int rowIndex)
            throws PrayerTimesFormatException {
        if (!rowElement.isJsonArray()) {
            throw new PrayerTimesFormatException("days[" + rowIndex + "] dizi değil");
        }
        JsonArray row = rowElement.getAsJsonArray();
        if (row.size() < columns.size()) {
            throw new PrayerTimesFormatException("days[" + rowIndex + "] eksik sütun içeriyor");
        }

        String date = cell(row, columns, FIELD_DATE, rowIndex);
        if (date == null) {
            throw new PrayerTimesFormatException("days[" + rowIndex + "] tarih boş");
        }
        return new PrayerDayDto(
                date,
                cell(row, columns, FIELD_FAJR, rowIndex),
                cell(row, columns, FIELD_SUNRISE, rowIndex),
                cell(row, columns, FIELD_DHUHR, rowIndex),
                cell(row, columns, FIELD_ASR, rowIndex),
                cell(row, columns, FIELD_MAGHRIB, rowIndex),
                cell(row, columns, FIELD_ISHA, rowIndex),
                cell(row, columns, FIELD_HIJRI, rowIndex),
                cell(row, columns, FIELD_QIBLA, rowIndex),
                gmtOffsetMinutes(row.get(columns.get(FIELD_GMT)), rowIndex));
    }

    /** İsteğe bağlı sütun dosyada yoksa veya değer null ise null döner. */
    @Nullable
    private String cell(JsonArray row, Map<String, Integer> columns, String field, int rowIndex)
            throws PrayerTimesFormatException {
        Integer column = columns.get(field);
        if (column == null) {
            return null;
        }
        JsonElement value = row.get(column);
        if (value == null || value.isJsonNull()) {
            return null;
        }
        if (!isString(value)) {
            throw new PrayerTimesFormatException("days[" + rowIndex + "]." + field + " metin değil");
        }
        return value.getAsString();
    }

    /** gmt saat cinsindendir ve kesirli olabilir (ör. 3.5 → 210 dakika). */
    private int gmtOffsetMinutes(JsonElement value, int rowIndex) throws PrayerTimesFormatException {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new PrayerTimesFormatException("days[" + rowIndex + "].gmt sayı değil");
        }
        return (int) Math.round(value.getAsDouble() * 60);
    }

    private static boolean isString(JsonElement element) {
        return element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
    }

    private static JsonArray requireArray(JsonObject root, String key) throws PrayerTimesFormatException {
        JsonElement value = root.get(key);
        if (value == null || !value.isJsonArray()) {
            throw new PrayerTimesFormatException(key + " dizi değil veya eksik");
        }
        return value.getAsJsonArray();
    }

    private static int requireInt(JsonObject root, String key) throws PrayerTimesFormatException {
        Integer value = optInt(root, key);
        if (value == null) {
            throw new PrayerTimesFormatException(key + " sayı değil veya eksik");
        }
        return value;
    }

    @Nullable
    private static Integer optInt(JsonObject root, String key) throws PrayerTimesFormatException {
        JsonElement value = root.get(key);
        if (value == null || value.isJsonNull()) {
            return null;
        }
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new PrayerTimesFormatException(key + " sayı değil");
        }
        JsonPrimitive primitive = value.getAsJsonPrimitive();
        double number = primitive.getAsDouble();
        if (number != Math.rint(number)) {
            throw new PrayerTimesFormatException(key + " tam sayı değil");
        }
        return primitive.getAsInt();
    }

    private static String requireString(JsonObject root, String key) throws PrayerTimesFormatException {
        String value = optString(root, key);
        if (value == null) {
            throw new PrayerTimesFormatException(key + " metin değil veya eksik");
        }
        return value;
    }

    @Nullable
    private static String optString(JsonObject root, String key) throws PrayerTimesFormatException {
        JsonElement value = root.get(key);
        if (value == null || value.isJsonNull()) {
            return null;
        }
        if (!isString(value)) {
            throw new PrayerTimesFormatException(key + " metin değil");
        }
        return value.getAsString();
    }
}
