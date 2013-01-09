package org.hopto.ronstorrents.richgps.activities;

import static org.hopto.ronstorrents.richgps.rest.ASyncManager.getSendArrivalTask;
import static org.hopto.ronstorrents.richgps.rest.ASyncManager.getUpdateDestinationsTask;

import java.util.ArrayList;
import java.util.List;

import org.hopto.ronstorrents.richgps.R;
import org.hopto.ronstorrents.richgps.data.AppParameters;
import org.hopto.ronstorrents.richgps.data.Waypoint;
import org.hopto.ronstorrents.richgps.service.BackgroundService;
import org.hopto.ronstorrents.richgps.service.BackgroundService.GPSBinder;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TextView;

public class MainActivity extends TabActivity {
	
	private AppParameters params = new AppParameters();
	private TextView info;
	private Button setID;
	private Button toggleService;
	private Button reqDestinations;
	private boolean enableToggleForStart = false;
	private boolean enableToggleForStop = false;
	private Location currentLocation;
	private ArrayAdapter<Waypoint> destAdapter;
	private List<Waypoint> destinations = new ArrayList<Waypoint>();
	private Handler timer;
	private TextView status;
	private BackgroundService service;
	private ServiceConnection serviceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder binder) {
			service = ((GPSBinder)binder).getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			service = null;
		}

	};

	/** Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		setupTabs();

		info = (TextView) findViewById(R.id.info);
		status = (TextView)findViewById(R.id.status);

		setID = (Button)findViewById(R.id.setID);
		setID.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				setID();
			}
		});
		
		timer = new Handler();
		Runnable task = new Runnable() {

			@Override
			public void run() {
				if (service != null && service.isStarted()) {
					toggleService.setText("Stop Service");
					if (enableToggleForStop) {
						params = service.getAppParams();
						enableToggleForStop = false;
						toggleService.setEnabled(true);
					}
					updateLocation();
					updateStatus();
				} else {
					toggleService.setText("Start Service");
					if (enableToggleForStart) {
						enableToggleForStart = false;
						toggleService.setEnabled(true);
					}
					status.setText("Service not running");
				}
				timer.postDelayed(this, params.getGUIUpdate());
			}

		};
		timer.postDelayed(task, params.getGUIUpdate());

		toggleService = (Button)findViewById(R.id.toggleService);
		toggleService.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleService();
			}
		});
		toggleService.setEnabled(false);
		enableToggleForStop = true;
		
		reqDestinations = (Button)findViewById(R.id.fetch_destinations);
		reqDestinations.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				requestDestinations();
			}
		});
		
		ListView list = (ListView)findViewById(R.id.destination_list);
		destAdapter = new ArrayAdapter<Waypoint>(this, android.R.layout.simple_list_item_1,
				destinations);
		list.setAdapter(destAdapter);
		list.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapter, View itemView, int index,
					long id) {
				visitWaypoint(destinations.get(index));
			}
		});
	}

	private void visitWaypoint(Waypoint waypoint) {
		getSendArrivalTask(this).execute(params.getVID(), params.getKey(),
				waypoint, System.currentTimeMillis());
	}
	
	public void requestDestinations() {
		reqDestinations.setEnabled(false);
		getUpdateDestinationsTask(this).execute(params.getVID());
	}
	
	public void destinationUpdateFail() {
		reqDestinations.setEnabled(true);
	}
	
	public void updateWaypoints(List<Waypoint> waypoints) {
		destinations.clear();
		destinations.addAll(waypoints);
		destAdapter.notifyDataSetChanged();
		reqDestinations.setEnabled(true);
	}
	
	private void setupTabs() {
		TabHost host = (TabHost)findViewById(android.R.id.tabhost);
		host.setup();
		host.addTab(host.newTabSpec("Destinations").setIndicator("Destinations")
				.setContent(new TabContentFactory() {
			@Override
			public View createTabContent(String tag) {
				return findViewById(R.id.tab1);
			}
		}));
		host.addTab(host.newTabSpec("Debug").setIndicator("Debug")
				.setContent(new TabContentFactory() {
			
			@Override
			public View createTabContent(String tag) {
				return findViewById(R.id.tab2);
			}
		}));
		host.setCurrentTab(1);
		host.setCurrentTab(0);
	}
	
	private void setID() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Vehicle ID");
		alert.setMessage("Enter Vehicle ID:");
		final EditText input = new EditText(this);
		alert.setView(input);
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
			int value = Integer.parseInt(input.getText().toString());
			setAPIKey(value);
		  }
		});
		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int whichButton) {}
		});
		alert.show();
	}
	private void setAPIKey(final int vehicleID) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("API Key");
		alert.setMessage("Enter API Key:");
		final EditText input = new EditText(this);
		alert.setView(input);
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
			String key = input.getText().toString();
			if (service != null) {
				service.setID(vehicleID, key);
			}
		  }
		});
		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int whichButton) {}
		});
		alert.show();
	}
	
	private void updateInfo() {
		StringBuilder sb = new StringBuilder();
		if (currentLocation != null) {
			sb.append("Time: ").append(currentLocation.getTime());
			sb.append("\nLat: ").append(String.format("%.3f", currentLocation.getLatitude()));
			sb.append("\nLong: ").append(String.format("%.3f", currentLocation.getLongitude()));
			sb.append("\nSpeed: ").append(currentLocation.getSpeed());
			sb.append("\nBearing: ").append(currentLocation.getBearing());
			sb.append("\nAltitude: ").append(currentLocation.getAltitude());
			sb.append("\nAccuracy: ").append(currentLocation.getAccuracy());
			sb.append("\nProvider: ").append(currentLocation.getProvider());
		} else {
			sb.append("Unknown");
		}
		info.setText(sb.toString());
	}
	
	private void updateStatus() {
		StringBuilder sb = new StringBuilder();
		sb.append("Updates sent: ").append(service.getSent());
		sb.append("\nSeconds to next send: ").append(service.getTimeToNextSend());
		sb.append("\nCredentials: ");
		sb.append("ID: ").append(params.getVID()).append(", Key: ").append(params.getKey());
		sb.append("\nMost recent: ");
		long lastUpdate = service.getLastUpdate();
		if (lastUpdate == -1) {
			sb.append("never");
		} else {
			sb.append((System.currentTimeMillis() - lastUpdate) / 1000);
			sb.append(" seconds");
		}
		sb.append("\nQueue size: ").append(service.getQueueSize());
		status.setText(sb.toString());
	}
	
	private void updateLocation() {
		currentLocation = service.getLocation();
		updateInfo();
	}
	
	private void toggleService() {
		toggleService.setEnabled(false);
		
		if (service != null) {
			enableToggleForStart = true;
			setID.setEnabled(false);
			stopBackgroundService();
		} else {
			enableToggleForStop = true;
			setID.setEnabled(true);
			startBackgroundService();
		}
	}

	private void stopBackgroundService() {
		if (service != null) {
			if (service.isStarted()) {
				stopService(new Intent(MainActivity.this, BackgroundService.class));
			}
			unbindService(serviceConnection);
			service = null;
		}
	}

	private void startBackgroundService() {
		if (service == null)
			bindService(new Intent(this, BackgroundService.class),
					serviceConnection, Context.BIND_AUTO_CREATE);
		startService(new Intent(this, BackgroundService.class));
	}
	
	@Override
	public void onPause() {
		if (service != null) {
			unbindService(serviceConnection);
			service = null;
		}
		super.onPause();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		startBackgroundService();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
} 