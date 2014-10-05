package com.example.waypal;

import java.util.ArrayList;
import java.util.Map;

import com.example.waypal.Trip.POI;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class POIFragment extends ListFragment {
	
	ListView waypoints;
	ArrayList<String> data = new ArrayList<String>();
	ArrayAdapter<String>  mAdapter;
	boolean viewCreated = false;
	Firebase myFirebaseRef;
	
	public POIFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		
		data.add("SUPER COOL");
		data.add("SUPER SWEET");
		mAdapter = new ArrayAdapter<String>(inflater.getContext(),
				android.R.layout.simple_list_item_1, data);
		setListAdapter(mAdapter);
		View rootView = super.onCreateView(inflater, container, savedInstanceState);
		viewCreated = true;
		
		
		setListItems(new String[]{"testing", "testing123"});

		return rootView;
	}
	
	public void setFirebase(Firebase firebaseRef) {
		this.myFirebaseRef = firebaseRef;
	}
	
	private void addWaypoint(final POI poi) {
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
		if (myFirebaseRef == null) {
			return;
		}
		System.out.println("position: " + position);
		removeItem(position);
	}
	
	public void removeItem(int i) {
		data.remove(i);
		mAdapter.notifyDataSetChanged();
	}
	
	public void setItem(int i, String value) {
		data.set(i, value);
		mAdapter.notifyDataSetChanged();
	}

	public void setListItems(String[] items) {
		if (!viewCreated) {
			return;
		}

		mAdapter.clear();
		for (int i = 0; i < items.length; i++) {
		        mAdapter.add(items[i]);
		}
		mAdapter.notifyDataSetChanged();	
	}
}
