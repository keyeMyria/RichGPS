package org.hopto.ronstorrents.richgps.rest;

import static org.hopto.ronstorrents.richgps.rest.JSONBuilder.arrivalJSON;
import static org.hopto.ronstorrents.richgps.rest.JSONBuilder.reportJSON;
import static us.monoid.web.Resty.content;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.hopto.ronstorrents.richgps.data.Report;
import org.hopto.ronstorrents.richgps.data.Waypoint;

import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.json.JSONTokener;
import us.monoid.web.JSONResource;
import us.monoid.web.Resty;

public class RESTManager {
	
	private static String serviceRootURL = "http://ronstorrents.hopto.org/gps";
	
	public static List<Waypoint> getWaypoints(int vehicleID) throws IOException, JSONException {
		JSONResource response = new Resty().
				json(serviceRootURL + "/schedule/" + vehicleID);
		JSONArray dests = new JSONArray(new JSONTokener(
				new InputStreamReader(response.stream())));
		
		List<Waypoint> results = new ArrayList<Waypoint>(); 
		for (int i = 0; i < dests.length(); i++) {
			JSONObject waypoint = dests.getJSONObject(i);
			String name = waypoint.getString("waypoint_name");
			Integer id = waypoint.getInt("waypoint");
			results.add(new Waypoint(name, id));
		}
		return results;
	}
	
	public static void sendArrival(int vehicleID, String apiKey,
			Waypoint waypoint, long time) throws JSONException, IOException {
		JSONObject report = arrivalJSON(vehicleID,
				waypoint.id, time);
		JSONResource response = new Resty().json(serviceRootURL +
				"/visit_waypoint/" + apiKey, content(report));
		if (!response.status(200))
			throw new IOException("Bad response");
	}
	
	public static void sendLocation(Report report, int vehicleID, String apiKey)
			throws JSONException, IOException {
		JSONObject js = reportJSON(report, vehicleID);
		JSONResource r = new Resty().json(
				serviceRootURL + "/add/" + apiKey,
				content(js));
		if (!r.status(200))
			throw new IOException("Bad response");
	}
	
	
}
