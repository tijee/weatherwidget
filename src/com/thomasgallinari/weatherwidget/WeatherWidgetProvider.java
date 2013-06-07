package com.thomasgallinari.weatherwidget;

import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.text.format.DateUtils;
import android.widget.RemoteViews;

import com.thomasgallinari.weatherwidget.data.Forecast;
import com.thomasgallinari.weatherwidget.ws.WSHandler;
import com.thomasgallinari.weatherwidget.ws.WSRequest;

public class WeatherWidgetProvider extends AppWidgetProvider {

    private static final String URL_HOURLY = "http://api.openweathermap.org/data/2.5/forecast";
    private static final String URL_DAILY = "http://api.openweathermap.org/data/2.5/forecast/daily";
    private static final String PARAM_LAT = "lat";
    private static final String PARAM_LON = "lon";
    private static final String PARAM_CNT = "cnt";
    private static final String PARAM_APIKEY = "APIKEY";
    private static final String APIKEY = "5c307f04ad238e3be670dfd1e8f4014e";

    private static final int MAX_HOURLY_DATA = 6;
    private static final int MAX_DAILY_DATA = 7;

    @Override
    public void onUpdate(final Context context,
	    final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
	final int widgetsCount = appWidgetIds.length;

	LocationManager locationManager = (LocationManager) context
		.getSystemService(Context.LOCATION_SERVICE);
	Location lastLocation = locationManager
		.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	if (lastLocation == null) {
	    return;
	}
	final String lat = String.valueOf(lastLocation.getLatitude());
	final String lon = String.valueOf(lastLocation.getLongitude());

	// hourly
	new WSRequest(context, URL_HOURLY).withParam(PARAM_LAT, lat)
		.withParam(PARAM_LON, lon).withParam(PARAM_APIKEY, APIKEY)
		.handleWith(new WSHandler() {

		    @Override
		    public void onResult(Context context, JSONObject result) {
			try {
			    final Forecast forecast = new Forecast();
			    forecast.parseHourly(result, MAX_HOURLY_DATA);

			    // daily
			    new WSRequest(context, URL_DAILY)
				    .withParam(PARAM_LAT, lat)
				    .withParam(PARAM_LON, lon)
				    .withParam(PARAM_CNT, MAX_DAILY_DATA)
				    .withParam(PARAM_APIKEY, APIKEY)
				    .handleWith(new WSHandler() {

					@Override
					public void onResult(Context context,
						JSONObject result) {
					    try {
						forecast.parseDaily(result,
							MAX_DAILY_DATA);

						// update widget(s)
						for (int i = 0; i < widgetsCount; i++) {
						    int appWidgetId = appWidgetIds[i];
						    RemoteViews views = new RemoteViews(
							    context.getPackageName(),
							    R.layout.weatherwidget);
						    updateWidget(context,
							    views, forecast);
						    appWidgetManager
							    .updateAppWidget(
								    appWidgetId,
								    views);
						}
					    } catch (JSONException e) {
					    }
					}
				    }).call();
			} catch (JSONException e) {
			}
		    }
		}).call();
    }

    private void updateWidget(Context context, RemoteViews views,
	    Forecast forecast) {
	// hourly
	int hourlyCount = forecast.hourly.size();
	for (int i = 0; i < MAX_HOURLY_DATA && i < hourlyCount; i++) {
	    Forecast.Data data = forecast.hourly.get(i);

	    int timeTextView = context.getResources().getIdentifier(
		    "hourly_time_" + (i + 1), "id", context.getPackageName());
	    int tempTextView = context.getResources().getIdentifier(
		    "hourly_temp_" + (i + 1), "id", context.getPackageName());
	    int iconImageView = context.getResources().getIdentifier(
		    "hourly_img_" + (i + 1), "id", context.getPackageName());

	    views.setTextViewText(timeTextView, DateUtils.formatDateTime(
		    context, data.time, DateUtils.FORMAT_SHOW_TIME));
	    views.setTextViewText(tempTextView,
		    getTemperatureLabel(context, data.tempMin));

	    int iconDrawable = context.getResources().getIdentifier(
		    "weather_white_" + data.icon, "drawable",
		    context.getPackageName());
	    if (iconDrawable == 0) {
		iconDrawable = R.drawable.weather_white_na;
	    }
	    views.setImageViewResource(iconImageView, iconDrawable);
	}

	// daily
	int dailyCount = forecast.daily.size();
	for (int i = 1; i < MAX_DAILY_DATA + 1 && i < dailyCount; i++) {
	    Forecast.Data data = forecast.daily.get(i);

	    int timeTextView = context.getResources().getIdentifier(
		    "daily_time_" + i, "id", context.getPackageName());
	    int tempTextView = context.getResources().getIdentifier(
		    "daily_temp_" + i, "id", context.getPackageName());
	    int iconImageView = context.getResources().getIdentifier(
		    "daily_img_" + i, "id", context.getPackageName());

	    views.setTextViewText(timeTextView, DateUtils.formatDateTime(
		    context, data.time, DateUtils.FORMAT_SHOW_WEEKDAY
			    | DateUtils.FORMAT_ABBREV_WEEKDAY));
	    views.setTextViewText(tempTextView,
		    getTemperatureLabel(context, data.tempMin, data.tempMax));

	    int iconDrawable = context.getResources().getIdentifier(
		    "weather_white_" + data.icon, "drawable",
		    context.getPackageName());
	    if (iconDrawable == 0) {
		iconDrawable = R.drawable.weather_white_na;
	    }
	    views.setImageViewResource(iconImageView, iconDrawable);
	}
    }

    private String getTemperatureLabel(Context context, double tempMin,
	    double tempMax) {
	boolean showFarenheits = Locale.getDefault().getCountry().equals("US");
	tempMin = showFarenheits ? kelvinToFarenheit(tempMin)
		: kelvinToCelsius(tempMin);
	tempMax = showFarenheits ? kelvinToFarenheit(tempMax)
		: kelvinToCelsius(tempMax);
	if (tempMin == tempMax) {
	    return Math.round(tempMin)
		    + context.getString(R.string.degrees_unit);
	} else {
	    return Math.round(tempMin) + "-" + Math.round(tempMax)
		    + context.getString(R.string.degrees_unit);
	}
    }

    private String getTemperatureLabel(Context context, double temp) {
	return getTemperatureLabel(context, temp, temp);
    }

    private double kelvinToCelsius(double temp) {
	return temp - 273.15;
    }

    private double kelvinToFarenheit(double temp) {
	return kelvinToCelsius(temp) * 1.8 + 32;
    }
}
