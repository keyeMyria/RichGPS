package org.hopto.ronstorrents.richgps.service;

import static org.hopto.ronstorrents.richgps.rest.RESTManager.sendLocation;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import org.hopto.ronstorrents.richgps.R;
import org.hopto.ronstorrents.richgps.activities.MainActivity;
import org.hopto.ronstorrents.richgps.data.AppParameters;
import org.hopto.ronstorrents.richgps.data.Report;

import us.monoid.json.JSONException;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

public class BackgroundService extends Service implements LocationListener {
	
	private PhoneHardwareManager hardware;
	private AppParameters params;
	
	private final IBinder binder = new GPSBinder();
	private Handler timer;
	private int period = 5000;
	private int sent = 0;
	private int maxAcc = 100;
	private long lastUpdate = -1;
	private int updatesRunning = 0;
	
	private Location currentLocation = null;
	private Queue<Report> queue;
	private int maxQueueSize;
	private int failures = 0;
	private int backOff = 1;
	private boolean started = false;
	
	private Notification notification;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		if (started)
			return START_STICKY;
		params = new AppParameters();
		started = true;
		hardware = new PhoneHardwareManager(getApplicationContext());
		hardware.addLocationUpdateListener(1000, 0, this);
	    queue = new LinkedList<Report>();
	    maxQueueSize = 10000;
		
		timer = new Handler();
		timer.postDelayed(timerTask, period);
		
		createNotification();
		startForeground(1, notification);
		return START_STICKY;
	}
	
	private void createNotification() {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		builder.setSmallIcon(R.drawable.ic_launcher);
		builder.setContentTitle("GPS Service");
		builder.setContentText("Background update of GPS");
		TaskStackBuilder stack = TaskStackBuilder.create(this);
		stack.addParentStack(MainActivity.class);
		stack.addNextIntent(new Intent(this, MainActivity.class));
		PendingIntent pendingIntent = stack.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pendingIntent);
		notification = builder.build();
	}
	
	private Runnable timerTask = new Runnable() {

		@Override
		public void run() {
			if (updatesRunning == 0) {
				AsyncTask<Void, Void, Void> updateTask = new AsyncTask<Void, Void, Void>() {
					@Override
					protected Void doInBackground(Void... params) {
						updatesRunning++;
						update();
						updatesRunning--;
						return null;
					}
				};
				updateTask.execute();
			}
			timer.removeCallbacks(this);
			timer.postDelayed(this, period);
		}
		
	};
	
	public boolean isStarted() {
		return started;
	}
	
	public AppParameters getAppParams() {
		return params;
	}
	
	public void setID(int vehicleID, String apiKey) {
		params.setVID(vehicleID);
		params.setKey(apiKey);
		queue.clear();
	}

	private void update() {
		if (hardware.isNetworkAvailable()) {
			if (--backOff <= 0)
				sent += sendLocations();
		}
	}
	
	public int getTimeToNextSend() {
		return backOff;
	}
	
	public int getSent() {
		return sent;
	}
	
	public int getQueueSize() {
		return queue.size();
	}
	
	public Location getLocation() {
		return currentLocation;
	}
	
	public long getLastUpdate() {
		return lastUpdate;
	}

	private int sendLocations() {
		
		int count = 0;
		while (!queue.isEmpty()) {
			synchronized (queue) {
				if (!queue.isEmpty()) {
					try {
						Report rep = queue.peek();
						sendLocation(rep, params.getVID(), params.getKey());
						failures = 0;
						queue.poll();
						count++;
						lastUpdate = System.currentTimeMillis();
						continue;
					} catch (JSONException e) {
					} catch (IOException e) {
					}
					if (failures < 20)
						failures++;
					backOff = failures;
					Log.w("GPS", "Failed to report, backing off " + backOff);
					return count;
				}
			}
		}
		backOff = 1;
		return count;
	}
	
	private void addCurrentLocationToQueue() {
		Report r = new Report(currentLocation, System.currentTimeMillis());
		synchronized (queue) {
			queue.offer(r);
			if (queue.size() > maxQueueSize) {
				queue.poll();
			}
		}
	}
	
	@Override
	public void onLocationChanged(Location location) {
		if (!location.equals(currentLocation) && location.getAccuracy() < maxAcc &&
				(currentLocation == null || 
				location.getTime() - currentLocation.getTime() > 5000)) {
			currentLocation = location;
			addCurrentLocationToQueue();
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onProviderDisabled(String provider) {
	}
	
	
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public void onDestroy() {
		timer.removeCallbacks(timerTask);
		hardware.removeLocationUpdateListener(this);
		stopForeground(true);
		super.onDestroy();
		started = false;
	}
	
	public class GPSBinder extends Binder {
		
		public BackgroundService getService() {
			return BackgroundService.this;
		}
	}


}
