package com.example.waypal;

public class POIobj {
	
	public String name;
	public String lat;
	public String lng;
	public String summary;
	
	public POIobj(String name, String lat, String lng, String summary) {
		this.name = name;
		this.lat = lat;
		this.lng = lng;
		this.summary = summary;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}
