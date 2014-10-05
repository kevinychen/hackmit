package com.example.waypal;

import java.util.ArrayList;

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
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {

	}
	
	public void setListItems(String[] items) {
		if (!viewCreated) {
			return;
		}

		mAdapter.clear();
		data.clear();
		for (int i = 0; i < items.length; i++) {
		        mAdapter.add(items[i]);
		        data.add(items[i]);
		}
		mAdapter.notifyDataSetChanged();	
	}
}
