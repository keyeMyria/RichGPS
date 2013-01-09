package org.hopto.ronstorrents.richgps.service;

import android.content.Context;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class PhoneHardwareManager {

	private LocationManager locationManager;
    private ConnectivityManager connectivityManager;
    private Context context;
    
	public PhoneHardwareManager(Context context) {
		 this.context = context;
		 initialise();
	}
	
	private void initialise() {
		locationManager = (LocationManager)context.
				getSystemService(Context.LOCATION_SERVICE);
		 connectivityManager = (ConnectivityManager)context.
				 getSystemService(Context.CONNECTIVITY_SERVICE);
	}
	
	public void addLocationUpdateListener(long minPeriod, int minDistance,
			LocationListener listener) {
		removeLocationUpdateListener(listener);
		for (String provider : locationManager.getAllProviders())
			locationManager.requestLocationUpdates(provider,
					minPeriod, minDistance, listener);
	}
	
	public void removeLocationUpdateListener(LocationListener listener) {
		locationManager.removeUpdates(listener);		
	}
	
	public boolean isNetworkAvailable() {
		NetworkInfo activeInfo = connectivityManager.getActiveNetworkInfo();
		if (activeInfo == null)
			return false;
		if (!activeInfo.isConnected() || !activeInfo.isAvailable())
			return false;
	    return  true;
	}
	
}
