package com.example.earthquake;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class EarthquakeAlarmReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent startIntent=new Intent(context,EarthquakeUpdateService.class);
		context.startService(startIntent);
	}
	
	public static final String ACTION_REFRESH_EARTHQUAKE_ALARM=
			"com.example.earthquake.ACTION_REFRESH_EARTHQUAKE_ALARM";
	

}
