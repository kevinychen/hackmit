package com.example.waypal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.example.waypal.Trip.POI;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

public class POIFragment extends ListFragment {
	
	ListView waypoints;
	ArrayList<String> data = new ArrayList<String>();
	ArrayAdapter<String>  mAdapter;
	boolean viewCreated = false;
	HashSet<String> alreadySeen = new HashSet<String>();
	Firebase myFirebaseRef;
	Trip trip;
	
	public POIFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		
		/*
		data.add("SUPER COOL");
		data.add("SUPER SWEET");
		data.add("SUPER SWET");
		data.add("SUPER SWT");
		data.add("SUPER ST");
		*/
		mAdapter = new ArrayAdapter<String>(inflater.getContext(),
				android.R.layout.simple_list_item_1, data);
		setListAdapter(mAdapter);
		View rootView = super.onCreateView(inflater, container, savedInstanceState);
		viewCreated = true;
		

		
		return rootView;
	}
	
	public void setFirebase(Firebase firebaseRef) {
		this.myFirebaseRef = firebaseRef;
	}
	
	public void setTrip(Trip trip) {
		this.trip = trip;
	}
	
	private void addWaypoint(final POI poi) {
		if (myFirebaseRef == null || trip == null) {
			return;
		}
		myFirebaseRef.child("waypoints").addListenerForSingleValueEvent(new ValueEventListener() {
		    @Override
		    public void onDataChange(DataSnapshot snapshot) {
		    	Map<String, Object> waypoints = (Map<String, Object>) snapshot.getValue();

		    	int len = waypoints.size();
		    	waypoints.put("waypoint" + (len - 1), new Waypoint(poi));
		    	
		    	myFirebaseRef.child("waypoints").setValue(waypoints);
		    }

		    @Override
		    public void onCancelled(FirebaseError firebaseError) {
		    }
		});
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		pickWaypoint(position);
	}
	
	public void pickWaypoint(int position) {
		System.out.println("position: " + position);
		if (myFirebaseRef == null || trip == null || position >= data.size()) {
			return;
		}
		String name = data.get(position);
        removeItem(position);
        for(POI poi : trip.pois){
        	if (poi.name.equals(name)) {
        		addWaypoint(poi);
        		Toast.makeText(getActivity(), "You've added a new waypoint: " + name, Toast.LENGTH_LONG).show();
        		return;
        	}
        }
	}
	
	public void removeItem(int i) {
		if (myFirebaseRef == null || trip == null || i >= data.size()) {
			return;
		}
		alreadySeen.add(data.get(i));
		data.remove(i);
		mAdapter.notifyDataSetChanged();
	}
	
	public void setItem(int i, String value) {
		if (myFirebaseRef == null || trip == null) {
			return;
		}
		if (alreadySeen.contains(value)) {
			return;
		}
		data.set(i, value);
		mAdapter.notifyDataSetChanged();
	}

	public void setListItems(String[] items) {
		if (myFirebaseRef == null || trip == null) {
			return;
		}
		if (!viewCreated) {
			return;
		}

		mAdapter.clear();
		for (int i = 0; i < items.length; i++) {

			if (alreadySeen.contains(items[i])) {
				continue;
			}
			mAdapter.add(items[i]);
		}
		mAdapter.notifyDataSetChanged();	
	}
}
