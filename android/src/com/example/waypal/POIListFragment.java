package com.example.waypal;

import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class POIListFragment extends ListFragment {
	
	@Override
    public View onCreateView(LayoutInflater inflater,ViewGroup container, Bundle savedInstanceState) {
		
        View view =inflater.inflate(R.layout.fragment_poi, container, false);
        System.out.println(view);
 
        return view;
    }
	
	@Override
	public void onActivityCreated(Bundle savedInstance){
		String[] wayPoints = {"BSO", "Fenway", "City Hall"};
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1, wayPoints);
        setListAdapter(adapter);
        System.out.println(adapter);
		
	}

	  @Override
	  public void onListItemClick(ListView l, View v, int position, long id) {
	    System.out.println(v);
	    System.out.println(l);
	  }
	
}