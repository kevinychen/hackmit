package com.example.waypal;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;

public class POIListActivity extends ListActivity {
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		String[] wayPoints = {"BSO", "Fenway", "City Hall"};
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, wayPoints);
        setListAdapter(adapter);
	}
}