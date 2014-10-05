package com.example.waypal;

import com.google.android.gms.maps.model.LatLng;

public class Waypoint {
	public String lat;
	public String lng;
	public String name;
	public String stopover;
	
	public Waypoint(String name, android.location.Location loc) {
		this.name = name;
		this.lat = "" + loc.getLatitude();
		this.lng = "" + loc.getLongitude();
		this.stopover = "true";
	}
	
	public Waypoint(String name, LatLng latlng) {
		this.name = name;
		this.lat = "" + latlng.latitude;
		this.lng = "" + latlng.longitude;
		this.stopover = "true";
	}
	
	public Waypoint(POIobj o) {
		this.name = o.name;
		this.lat = o.lat;
		this.lng = o.lng;
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
