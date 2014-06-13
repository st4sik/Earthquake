package com.example.earthquake;

import com.google.android.maps.MapView;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;

public class EarthquakeMapFragment extends Fragment implements
		LoaderManager.LoaderCallbacks<Cursor> {

	EarthquakeOverlay eo;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		MapView earthquakeMap = ((Earthquake) getActivity()).mapView;
		eo = new EarthquakeOverlay(null);
		earthquakeMap.getOverlays().add(eo);
		return earthquakeMap;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String[] projection = new String[] { EarthquakeProvider.KEY_ID,
				EarthquakeProvider.KEY_LOCATION_LAT,
				EarthquakeProvider.KEY_LOCATION_LNG, };

		Earthquake earthquakeActivity = (Earthquake) getActivity();
		String where = EarthquakeProvider.KEY_MAGNITUDE + " > "
				+ earthquakeActivity.minimumMagnitude;

		CursorLoader loader = new CursorLoader(getActivity(),
				EarthquakeProvider.CONTENT_URI, projection, where, null, null);

		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		eo.swapCursor(data);

	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		eo.swapCursor(null);

	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getLoaderManager().initLoader(0, null, this);
	}

}
