package com.example.waypal;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

public class HomeActivity extends Activity {
	
	public final static String DESTINATION = "com.example.waypal.DESTINATION";
	
	Geocoder geocoder;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
			geocoder = new Geocoder(this, Locale.US);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.home, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void goToHome(View view) {
		Intent intent = new Intent(this, MainActivity.class);
		EditText editText = (EditText) findViewById(R.id.destination_message);
		String dest = editText.getText().toString();
		try {
			List<Address> results = geocoder.getFromLocationName(dest, 1);
			if (results.size() < 1) {
				Toast.makeText(this, "Could not find " + dest, Toast.LENGTH_SHORT).show();
				return;
			}
     		intent.putExtra(DESTINATION, results.get(0));
     		startActivity(intent);
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(this, "Connection error.", Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_home, container,
					false);
			
			return rootView;
		}
	}
	
}
