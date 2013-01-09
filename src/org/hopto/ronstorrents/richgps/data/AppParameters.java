package org.hopto.ronstorrents.richgps.data;

public class AppParameters {

	private int vehicleID = 1;
	private String apiKey = "key1";
	private int guiUpdate = 1000;
	
	public AppParameters() {
		
	}
	
	public int getVID() {
		return vehicleID;
	}
	
	public void setVID(int vehicleID) {
		this.vehicleID = vehicleID; 
	}
	
	public int getGUIUpdate() {
		return guiUpdate;
	}
	
	public String getKey() {
		return apiKey;
	}	
	
	public void setKey(String apiKey) {
		this.apiKey = apiKey;
	}
	
}
