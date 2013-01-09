package org.hopto.ronstorrents.richgps.rest;

import static org.hopto.ronstorrents.richgps.rest.RESTManager.getWaypoints;
import static org.hopto.ronstorrents.richgps.rest.RESTManager.sendArrival;

import java.io.IOException;
import java.util.List;

import org.hopto.ronstorrents.richgps.activities.MainActivity;
import org.hopto.ronstorrents.richgps.data.Waypoint;

import us.monoid.json.JSONException;
import android.os.AsyncTask;
import android.util.Log;

public class ASyncManager {

	public static AsyncTask<Object, Void, Void> getSendArrivalTask(
			final MainActivity source) {
		return new AsyncTask<Object, Void, Void>() {
			@Override
			protected Void doInBackground(Object... params) {
				int vehicleID = (Integer)params[0];
				String apiKey = (String)params[1];
				Waypoint waypoint = (Waypoint)params[2];
				long time = (Long)params[3];
				try {
					sendArrival(vehicleID, apiKey, waypoint, time);
					publishProgress();
					return null;
				} catch (JSONException e) {
					
				} catch (IOException e) {
					
				}
				Log.e("GPS", "Arrival report error");
				return null;
			}
			@Override
			protected void onProgressUpdate(Void ... progress) {
				source.requestDestinations();
			}
		};
	}

	public static AsyncTask<Integer, List<Waypoint>, Void> getUpdateDestinationsTask(
			final MainActivity source) {
		return new AsyncTask<Integer, List<Waypoint>, Void>() {
			@SuppressWarnings("unchecked")
			@Override
			protected Void doInBackground(Integer... params) {
				try {
					List<Waypoint> results = getWaypoints(params[0]);
					publishProgress(results);
					return null;
				} catch (IOException e) {
					
				} catch (JSONException e) {
					
				}
				Log.e("GPS", "Dest request error");
				source.destinationUpdateFail();
				return null;
			}
			@Override
			protected void onProgressUpdate(List<Waypoint> ... progress) {
				source.updateWaypoints(progress[0]);
			}
		};
	}
	
}
