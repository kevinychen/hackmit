package com.example.waypal;


import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.waypal.Trip.POI;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;

public class MainActivity extends FragmentActivity {

	MapFragment mapFragment;
	POIFragment poiFragment;
	GoogleMap map;
	Firebase myFirebaseRef;
    TextToSpeech ttobj;
	Trip trip;
    Location mCurrentLocation;
    boolean tripInitialized = false;
    LatLng start, dest;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if (savedInstanceState == null) {
			mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
			poiFragment = (POIFragment) getFragmentManager().findFragmentById(R.id.pois);
			setUpMap();
		}

		this.trip = new Trip();
		this.dest = getDestination();

		// Setup Firebase listener
		Firebase.setAndroidContext(this);
		Map<String, String> tripInfo = new HashMap<String, String>();
		tripInfo.put("id", trip.id);
		myFirebaseRef = new Firebase("https://waypal.firebaseio.com/").child(trip.id);
		myFirebaseRef.setValue(tripInfo);
		myFirebaseRef.addValueEventListener(new ValueEventListener() {

			  @Override
			  public void onDataChange(DataSnapshot snapshot) {
			    System.out.println(snapshot.getValue());  //prints "Do you have data? You'll love Firebase."
			  }

			  @Override public void onCancelled(FirebaseError error) { }

		});

        ttobj = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR){
                    ttobj.setLanguage(Locale.UK);
                }
            }
        });
        
        /* Use the LocationManager class to obtain GPS locations */
        LocationManager mlocManager = (LocationManager) 
                getSystemService(Context.LOCATION_SERVICE);
        LocationListener mlocListener = new CustomLocationListener(
                getApplicationContext());
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW);   
        String locationProvider = mlocManager.getBestProvider(criteria, true);
        mlocManager.requestLocationUpdates(locationProvider, 0, 0, mlocListener);

	}
	
	private void initializeWaypoints() {
		if (tripInitialized) {
			return;
		}

		Map<String, Object> waypoints = new HashMap<String, Object>();
		
		waypoints.put("waypoint1", new Waypoint("current location", mCurrentLocation));
		waypoints.put("waypoint2", new Waypoint("destination", getDestination()));

		myFirebaseRef.child("waypoints").setValue(waypoints);
		tripInitialized = true;
	}

	private void addWaypoint(final POI poi) {
		myFirebaseRef.child("waypoints").addListenerForSingleValueEvent(new ValueEventListener() {
		    @Override
		    public void onDataChange(DataSnapshot snapshot) {
		    	Map<String, Object> waypoints = (Map<String, Object>) snapshot.getValue();

		    	int len = waypoints.size();
		    	waypoints.put("waypoint" + (len + 1), new Waypoint(poi));
		    	
		    	myFirebaseRef.child("waypoints").setValue(waypoints);
		    }

		    @Override
		    public void onCancelled(FirebaseError firebaseError) {
		    }
		});
	}
	
	private void removeWaypoint(final String name) {
		myFirebaseRef.child("waypoints").addListenerForSingleValueEvent(new ValueEventListener() {
		    @Override
		    public void onDataChange(DataSnapshot snapshot) {
		    	Map<String, Object> waypoints = (Map<String, Object>) snapshot.getValue();
		    	
		    	if (waypoints.containsKey(name)) {
		    		waypoints.remove(name);
		    	}
		    	myFirebaseRef.child("waypoints").setValue(waypoints);
		    }

		    @Override
		    public void onCancelled(FirebaseError firebaseError) {
		    }
		});
	}

	private void setUpMap() {
	    // Do a null check to confirm that we have not already instantiated the map.
	    if (map == null) {
	        map = mapFragment.getMap();
	        // Check if we were successful in obtaining the map.
	        if (map != null) {
	            // The Map is verified. It is now safe to manipulate the map.
	        }
	    }
	}
		
	private LatLng getDestination() {
		Intent intent = getIntent();
		Address dest = intent.getParcelableExtra(HomeActivity.DESTINATION);
		return new LatLng(dest.getLatitude(), dest.getLongitude());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.home, menu);
        /* Use the LocationManager class to obtain GPS locations */
        LocationManager mlocManager = (LocationManager) 
                getSystemService(Context.LOCATION_SERVICE);
        LocationListener mlocListener = new CustomLocationListener(
                getApplicationContext());
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW);   
        String locationProvider = mlocManager.getBestProvider(criteria, true);
        mlocManager.requestLocationUpdates(locationProvider, 0, 0, mlocListener);
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

    public void speakText(View view) {
    	if (mCurrentLocation == null) {
    		speak("I don't know where I am.");
    		return;
    	}
    	
    	new SpeakPOITask().execute();
    }
    
    private void speak(String toSpeak) {
        Toast.makeText(getApplicationContext(), toSpeak,
        Toast.LENGTH_SHORT).show();
        ttobj.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
    }
    
    class CustomLocationListener implements LocationListener {

        private Context m_context;

        public CustomLocationListener(Context context) {
            m_context = context;
        }

        @Override
        public void onLocationChanged(Location location) {
            mCurrentLocation = location;
            
            // Idempotent function
            initializeWaypoints();

            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            String Text = latitude + " " + longitude;
            Toast.makeText(m_context, Text, Toast.LENGTH_SHORT).show();
        }

        @Override
		public void onProviderDisabled(String provider) {}

        @Override
		public void onProviderEnabled(String provider) {}

        @Override
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {}
    }
    
    class SpeakPOITask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... args) {
			HttpClient httpClient = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(
					"http://simple.mit.edu:8101/getPOIs?location="
							+ mCurrentLocation.getLatitude() + ","
							+ mCurrentLocation.getLongitude());
			try {
				HttpResponse response = httpClient.execute(httpGet);
				BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
				StringBuilder builder = new StringBuilder();
				for (String line = null; (line = reader.readLine()) != null;) {
				    builder.append(line).append("\n");
				}
				JSONTokener tokener = new JSONTokener(builder.toString());
				JSONObject finalResult = new JSONObject(tokener);
				JSONArray POIs = finalResult.getJSONArray("POIs");
				JSONObject POI = POIs.getJSONObject(0);
				return POI.getString("summary");
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "error";
		}

        @Override
		protected void onPostExecute(String result) {
        	speak(result);
        }
    }
    
    class SetWaypointTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... args) {
			HttpClient httpClient = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(
					"http://simple.mit.edu:8101/getPOIs?location="
							+ mCurrentLocation.getLatitude() + ","
							+ mCurrentLocation.getLongitude());
			try {
				HttpResponse response = httpClient.execute(httpGet);
				BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
				StringBuilder builder = new StringBuilder();
				for (String line = null; (line = reader.readLine()) != null;) {
				    builder.append(line).append("\n");
				}
				JSONTokener tokener = new JSONTokener(builder.toString());
				JSONObject finalResult = new JSONObject(tokener);
				JSONArray POIs = finalResult.getJSONArray("POIs");
				JSONObject POI = POIs.getJSONObject(0);
				return POI.getString("summary");
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "error";
		}

        @Override
		protected void onPostExecute(String result) {
        	speak(result);
        }
    }
}
