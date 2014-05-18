package com.example.earthquake;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.ListFragment;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.SimpleCursorAdapter;
import android.app.LoaderManager;

public class EarthquakeListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

	SimpleCursorAdapter adapter;
	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		
		adapter=new SimpleCursorAdapter(getActivity(),
				android.R.layout.simple_list_item_1,
				null, new String[] {EarthquakeProvider.KEY_SUMMARY},
				new int[] {android.R.id.text1},0);
		setListAdapter(adapter);
		
		getLoaderManager().initLoader(0, null, this);
		
		refreshEarthquakes();

		}
	
	private static final String TAG="EARTHQUAKE";
	private Handler handler=new Handler();
	
	 public void refreshEarthquakes() {
		 getLoaderManager().restartLoader(0, null, EarthquakeListFragment.this);
		 
		 getActivity().startService(new Intent(getActivity(),EarthquakeUpdateService.class));
		 
		 
		 
		  }
	 
	 
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String[] proj=new String[]
				{
					EarthquakeProvider.KEY_ID,
					EarthquakeProvider.KEY_SUMMARY
				};
		Earthquake earthquakeActivity=(Earthquake)getActivity();
		String where=EarthquakeProvider.KEY_MAGNITUDE+" > "+
				earthquakeActivity.minimumMagnitude;
		CursorLoader loader=new CursorLoader(getActivity(),
				EarthquakeProvider.CONTENT_URI,proj,where,null,null);
		return loader;
				
		
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		adapter.swapCursor(cursor);
		
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		adapter.swapCursor(null);
		
	}

}
