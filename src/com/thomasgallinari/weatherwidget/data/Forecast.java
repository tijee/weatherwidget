package com.thomasgallinari.weatherwidget.data;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Forecast {

    public ArrayList<Forecast.Data> hourly;
    public ArrayList<Forecast.Data> daily;

    public Forecast() {
	hourly = new ArrayList<Forecast.Data>();
	daily = new ArrayList<Forecast.Data>();
    }

    public void parseHourly(JSONObject json, int maxData) throws JSONException {
	hourly.clear();

	JSONArray hourlyData = json.getJSONArray("list");

	int hourlyDataCount = Math.min(hourlyData.length(), maxData);

	for (int i = 0; i < hourlyDataCount; i++) {
	    JSONObject data = hourlyData.getJSONObject(i);
	    long time = data.getLong("dt") * 1000;
	    JSONObject main = data.getJSONObject("main");
	    JSONArray weather = data.getJSONArray("weather");
	    double temp = main.getDouble("temp");
	    String icon = weather.getJSONObject(0).getString("icon");
	    hourly.add(new Data(time, icon, temp));
	}
    }

    public void parseDaily(JSONObject json, int maxData) throws JSONException {
	daily.clear();

	JSONArray dailyData = json.getJSONArray("list");

	int dailyDataCount = Math.min(dailyData.length(), maxData);

	for (int i = 0; i < dailyDataCount; i++) {
	    JSONObject data = dailyData.getJSONObject(i);
	    long time = data.getLong("dt") * 1000;
	    JSONObject temp = data.getJSONObject("temp");
	    JSONArray weather = data.getJSONArray("weather");
	    double tempMin = temp.getDouble("min");
	    double tempMax = temp.getDouble("max");
	    String icon = weather.getJSONObject(0).getString("icon");
	    daily.add(new Data(time, icon, tempMin, tempMax));
	}
    }

    public class Data {

	public long time;
	public String icon;
	public double tempMin;
	public double tempMax;

	public Data(long time, String icon, double tempMin, double tempMax) {
	    this.time = time;
	    this.icon = icon;
	    this.tempMin = tempMin;
	    this.tempMax = tempMax;
	}

	public Data(long time, String icon, double temp) {
	    this(time, icon, temp, temp);
	}
    }
}
