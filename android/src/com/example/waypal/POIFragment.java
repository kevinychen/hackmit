package com.example.waypal;

import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class POIFragment extends ListFragment {
	
	ListView waypoints;
	String[] data = new String[] {"one", "two", "three"};
	
	public POIFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(inflater.getContext(),
				android.R.layout.simple_list_item_1, data);
		setListAdapter(adapter);
		View rootView = super.onCreateView(inflater, container, savedInstanceState);

		return rootView;
	}
}
