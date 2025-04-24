package com.islamyolu.app.weather;

import java.util.List;

public class WeatherForecastData {
    private List<String> dates;
    private List<Integer> weatherCodes;
    private List<Double> maxTemperatures;

    public WeatherForecastData(List<String> dates, List<Integer> weatherCodes, List<Double> maxTemperatures) {
        this.dates = dates;
        this.weatherCodes = weatherCodes;
        this.maxTemperatures = maxTemperatures;
    }

    public List<String> getDates() {
        return dates;
    }

    public List<Integer> getWeatherCodes() {
        return weatherCodes;
    }

    public List<Double> getMaxTemperatures() {
        return maxTemperatures;
    }
}

