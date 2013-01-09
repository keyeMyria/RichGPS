package org.hopto.ronstorrents.richgps.data;

import android.location.Location;

public class Report {
	public final Location location;
	public final long report_time;
	public Report(Location location, long report_time) {
		this.location = location;
		this.report_time = report_time;
	}
}
