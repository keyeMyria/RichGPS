package org.hopto.ronstorrents.richgps.data;

public class Waypoint {

	public final String name;
	public final int id;
	
	public Waypoint(String name, int id) {
		this.name = name;
		this.id = id;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
}
