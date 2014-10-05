package com.example.waypal;

public class Waypoint {
	public String lat;
	public String lng;
	public String name;
	public String stopover;
	
	public Waypoint(String name, android.location.Location loc) {
		this.name = name;
		this.lat = String.format("%d", loc.getLatitude());
		this.lng = String.format("%d", loc.getLongitude());
		this.stopover = "true";
	}
	
	public String getLat() {
		return lat;
	}
	public void setLat(String lat) {
		this.lat = lat;
	}
	public String getLng() {
		return lng;
	}
	public void setLng(String lng) {
		this.lng = lng;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getStopover() {
		return stopover;
	}
	public void setStopover(String stopover) {
		this.stopover = stopover;
	}
	
	
	
	

}
