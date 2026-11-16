package com.islamyolu.app.weather;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.gson.Gson;

import org.junit.Test;

/** Open-Meteo yeni ("current") ve eski önbellek ("current_weather") biçimleri aynı modele okunur. */
public class WeatherDataParseTest {

    private final Gson gson = new Gson();

    @Test
    public void yeniBicim_hissedilenNemRuzgarOkunur() {
        String json = "{\"current\":{\"temperature_2m\":0.8,\"apparent_temperature\":-1.2,"
                + "\"relative_humidity_2m\":62,\"wind_speed_10m\":12.4,\"weather_code\":0,\"is_day\":1},"
                + "\"daily\":{\"time\":[\"2026-11-09\",\"2026-11-10\"],\"weather_code\":[0,61],"
                + "\"temperature_2m_max\":[5.7,11.0]}}";
        WeatherData data = gson.fromJson(json, WeatherData.class);

        WeatherData.CurrentWeather current = data.getCurrentWeather();
        assertNotNull(current);
        assertEquals(0.8, current.getTemperature(), 1e-9);
        assertEquals(-1.2, current.getApparentTemperature(), 1e-9);
        assertEquals(Integer.valueOf(62), current.getHumidity());
        assertEquals(12.4, current.getWindSpeed(), 1e-9);
        assertEquals(Integer.valueOf(61), data.getDaily().getWeatherCodes().get(1));
    }

    @Test
    public void eskiOnbellek_temelAlanlarOkunurEkAlanlarBos() {
        String json = "{\"current_weather\":{\"temperature\":18.4,\"weathercode\":2,\"is_day\":0},"
                + "\"daily\":{\"time\":[\"2026-11-06\"],\"weathercode\":[3],\"temperature_2m_max\":[17.3]}}";
        WeatherData data = gson.fromJson(json, WeatherData.class);

        WeatherData.CurrentWeather current = data.getCurrentWeather();
        assertNotNull(current);
        assertEquals(18.4, current.getTemperature(), 1e-9);
        assertEquals(2, current.getWeatherCode());
        assertNull(current.getApparentTemperature());
        assertNull(current.getHumidity());
        assertEquals(Integer.valueOf(3), data.getDaily().getWeatherCodes().get(0));
    }
}
