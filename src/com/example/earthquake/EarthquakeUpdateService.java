package com.example.earthquake;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

public class EarthquakeUpdateService extends IntentService {

	private Notification.Builder earthquakeNotificationBuilder;
	public static final int NOTIFICATION_ID = 1;

	public EarthquakeUpdateService(String name) {
		super(name);
	}

	public EarthquakeUpdateService() {
		super("EarthquakeUpdateService");
	}

	public static String TAG = "EARTHQUAKE_UPDATE_SERVICE";

	public void refreshEarthquakes() {
		// Get the XML
		URL url;
		try {
			String quakeFeed = getString(R.string.quake_feed);
			url = new URL(quakeFeed);

			URLConnection connection;
			connection = url.openConnection();

			HttpURLConnection httpConnection = (HttpURLConnection) connection;
			int responseCode = httpConnection.getResponseCode();

			if (responseCode == HttpURLConnection.HTTP_OK) {
				InputStream in = httpConnection.getInputStream();

				DocumentBuilderFactory dbf = DocumentBuilderFactory
						.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();

				// Parse the earthquake feed.
				Document dom = db.parse(in);
				Element docEle = dom.getDocumentElement();

				// Clear the old earthquakes

				// Get a list of each earthquake entry.
				NodeList nl = docEle.getElementsByTagName("entry");
				if (nl != null && nl.getLength() > 0) {
					for (int i = 0; i < nl.getLength(); i++) {
						Element entry = (Element) nl.item(i);
						Element title = (Element) entry.getElementsByTagName(
								"title").item(0);
						Element when = (Element) entry.getElementsByTagName(
								"updated").item(0);
						Element link = (Element) entry.getElementsByTagName(
								"link").item(0);
						Element g = (Element) entry.getElementsByTagName(
								"georss:point").item(0);
						if (g == null) {
							return;
						}

						String details = title.getFirstChild().getNodeValue();
						String hostname = "http://earthquake.usgs.gov";
						String linkString = hostname
								+ link.getAttribute("href");

						String point = g.getFirstChild().getNodeValue();
						String dt = (when.getFirstChild().getNodeValue())
								.replaceFirst("Z", "+0000");

						String format = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
						SimpleDateFormat sdf = new SimpleDateFormat(format);
						sdf.setTimeZone(TimeZone.getDefault());
						Date qdate = new GregorianCalendar(0, 0, 0).getTime();
						try {
							qdate = sdf.parse(dt);
						} catch (ParseException e) {
							Log.d(TAG, "Date parsing exception.", e);
						}

						String[] location = point.split(" ");
						Location l = new Location("dummyGPS");
						l.setLatitude(Double.parseDouble(location[0]));
						l.setLongitude(Double.parseDouble(location[1]));

						String magnitudeString = details.split(" ")[1];
						int end = magnitudeString.length();
						double magnitude = Double.parseDouble(magnitudeString
								.substring(0, end));
						String a=magnitudeString
								.substring(0, end);
						if (details.contains(",")) {
							details = details.split(",")[1].trim();
						} else {
							details = details.split("-")[1].trim();
						}

						final Quake quake = new Quake(qdate, details, l,
								magnitude, linkString);
						addNewQuake(quake);
						// Process a newly found earthquake

					}
				}
			}
		} catch (MalformedURLException e) {
			Log.d(TAG, "MalformedURLException", e);
		} catch (IOException e) {
			Log.d(TAG, "IOException", e);
		} catch (ParserConfigurationException e) {
			Log.d(TAG, "Parser Configuration Exception", e);
		} catch (SAXException e) {
			Log.d(TAG, "SAX Exception", e);
		} finally {
		}
	}

	private void addNewQuake(Quake _q) {
		ContentResolver cr = getContentResolver();

		String w = EarthquakeProvider.KEY_DATE + " = " + _q.getDate().getTime();

		Cursor query = cr.query(EarthquakeProvider.CONTENT_URI, null, w, null,
				null);
		if (query.getCount() == 0) {
			ContentValues values = new ContentValues();

			values.put(EarthquakeProvider.KEY_DATE, _q.getDate().getTime());
			values.put(EarthquakeProvider.KEY_DETAILS, _q.getDetails());
			values.put(EarthquakeProvider.KEY_SUMMARY, _q.toString());

			double lat = _q.getLocation().getLatitude();
			double lng = _q.getLocation().getLongitude();
			values.put(EarthquakeProvider.KEY_LOCATION_LAT, lat);
			values.put(EarthquakeProvider.KEY_LOCATION_LNG, lng);
			values.put(EarthquakeProvider.KEY_LINK, _q.getLink());
			values.put(EarthquakeProvider.KEY_MAGNITUDE, _q.getMagnitude());
			broadcastNotification(_q);
			cr.insert(EarthquakeProvider.CONTENT_URI, values);

		}
		query.close();
	}

	private AlarmManager alarmManager;
	private PendingIntent alarmIntent;

	@Override
	public void onCreate() {
		super.onCreate();
		alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		String ALARM_ACTION = EarthquakeAlarmReceiver.ACTION_REFRESH_EARTHQUAKE_ALARM;
		Intent intentToFire = new Intent(ALARM_ACTION);
		alarmIntent = PendingIntent.getBroadcast(this, 0, intentToFire, 0);

		earthquakeNotificationBuilder = new Notification.Builder(this);
		earthquakeNotificationBuilder.setAutoCancel(true)
				.setTicker("Earthquake Detected")
				.setSmallIcon(R.drawable.notification_icon);

	}

	private void broadcastNotification(Quake quake) {
		Intent startActivityIntent = new Intent(this, Earthquake.class);
		PendingIntent lauchIntent = PendingIntent.getActivity(this, 0,
				startActivityIntent, 0);

		earthquakeNotificationBuilder.setContentIntent(lauchIntent)
				.setWhen(quake.getDate().getTime())
				.setContentTitle("M : " + quake.getMagnitude())
				.setContentText(quake.getDetails());

		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		notificationManager.notify(NOTIFICATION_ID,
				earthquakeNotificationBuilder.getNotification());

		if (quake.getMagnitude() > 6) {
			Uri ringUri = RingtoneManager
					.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			earthquakeNotificationBuilder.setSound(ringUri);
		}
		double vibrateLength = 100 * Math.exp(0.53 * quake.getMagnitude());
		long[] vibrate = new long[] { 100, 100, (long) vibrateLength };
		earthquakeNotificationBuilder.setVibrate(vibrate);

		int color;
		if (quake.getMagnitude() < 5.4)
			color = Color.GREEN;
		else if (quake.getMagnitude() > 6)
			color = Color.YELLOW;
		else
			color = Color.RED;
		earthquakeNotificationBuilder.setLights(color, (int) vibrateLength,
				(int) vibrateLength);

	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public static String QUAKES_REFRESHED = "com.example.earthquake.QUAKES_REFRESHED";

	@Override
	protected void onHandleIntent(Intent intent) {
		Context context = getApplicationContext();
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);

		int updateFreq = Integer.parseInt(prefs.getString(
				PreferencesActivity.PREF_UPDATE_FREQ, "60"));

		boolean autoUpdateChecked = prefs.getBoolean(
				PreferencesActivity.PREF_AUTO_UPDATE, false);

		if (autoUpdateChecked) {
			int alarmType = AlarmManager.ELAPSED_REALTIME_WAKEUP;
			long timeToRefresh = SystemClock.elapsedRealtime() + updateFreq
					* 60 * 1000;
			alarmManager.setInexactRepeating(alarmType, timeToRefresh,
					updateFreq * 60 * 1000, alarmIntent);
		} else {
			alarmManager.cancel(alarmIntent);
		}
		refreshEarthquakes();
		sendBroadcast(new Intent(QUAKES_REFRESHED));

		AppWidgetManager appWidgetManager = AppWidgetManager
				.getInstance(context);

		ComponentName earthquakeWidget = new ComponentName(context,
				EarthquakeListWidget.class);
		int[] appWidgetIds = appWidgetManager.getAppWidgetIds(earthquakeWidget);
		appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds,
				R.id.widget_list_view);

	}

}
