package org.hopto.ronstorrents.richgps.rest;

import org.hopto.ronstorrents.richgps.data.Report;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

public class JSONBuilder {
	
	public static JSONObject reportJSON(Report report, int vehicleID)
			throws JSONException {
		JSONObject js = new JSONObject();
		js.put("id", vehicleID);
		js.put("report_time", report.report_time);
		js.put("time", report.location.getTime());
		JSONObject status = new JSONObject();
		status.put("lat", report.location.getLatitude());
		status.put("lng", report.location.getLongitude());
		status.put("speed", report.location.getSpeed());
		js.put("status", status);
		return js;
	}
	
	public static JSONObject arrivalJSON(int vehicleID, int waypointID,
			long time) throws JSONException {
		JSONObject report = new JSONObject();
		report.put("id", vehicleID);
		report.put("waypoint", waypointID);
		report.put("arrival_time", time);
		return report;
	}
	
}
