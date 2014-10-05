package com.example.waypal;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
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

	public static final int QUERY_TIMER = 60000;

	ListView listView;
	MapFragment mapFragment;
	POIFragment poiFragment;
	GoogleMap map;
	Firebase myFirebaseRef;
    TextToSpeech ttobj;
	Trip trip;
    Location mCurrentLocation;
    boolean tripInitialized = false;
    LatLng start, dest;
    Timer timer;  // triggers a call to getPOIs every once in a while
    Thread speaker;  // thread that keeps talking
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if (savedInstanceState == null) {
			mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
			poiFragment = (POIFragment) getFragmentManager().findFragmentById(R.id.pois);
			setUpMap();
		}

		poiFragment.setListItems(new String[]{"TOAST", "ANNA", "BURRITO"});
		this.trip = new Trip();

		// Setup Firebase listener
		Firebase.setAndroidContext(this);
		Map<String, String> tripInfo = new HashMap<String, String>();
		tripInfo.put("id", trip.id);
		myFirebaseRef = new Firebase("https://waypal.firebaseio.com/").child(trip.id);
		myFirebaseRef.setValue(tripInfo);
		myFirebaseRef.child("waypoints").addValueEventListener(new ValueEventListener() {

			  @Override
			  public void onDataChange(DataSnapshot snapshot) {
				  Map<String, Object> data = (Map<String, Object>)snapshot.getValue();
				  if (data == null) {
					  return;
				  }

				  List<Waypoint> waypoints = new ArrayList<Waypoint>();
				  
				  for(String key : data.keySet()) {
					  Map<String, String> waypoint = (Map<String, String>) data.get(key);
					  waypoints.add(new Waypoint(waypoint.get("name"), waypoint.get("lat"), waypoint.get("lng")));
				  }
				  
				  computeRoute(waypoints);
			  }

			  @Override public void onCancelled(FirebaseError error) { }

		});

        ttobj = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR){
                    ttobj.setLanguage(Locale.ENGLISH);
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

        timer = new Timer();
        timer.scheduleAtFixedRate(new GetPOITask(), 5000, QUERY_TIMER);
        
        speaker = new Speaker();
        speaker.start();
	}
	
	private void initializeWaypoints() {
		if (tripInitialized) {
			return;
		}

		Map<String, Object> waypoints = new HashMap<String, Object>();
		
		waypoints.put("waypoint0", new Waypoint("current location", mCurrentLocation));
		waypoints.put("waypoint9", new Waypoint("destination", getDestination()));
		this.start = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
		this.dest = getDestination();

		myFirebaseRef.child("waypoints").setValue(waypoints);
		tripInitialized = true;
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
	
	public void computeRoute(List<Waypoint> waypoints) {
		HttpClient httpClient = new DefaultHttpClient();
		String url = "http://maps.googleapis.com/maps/api/directions/json?origin="
				+ start.latitude + "," + start.longitude + "&destination=" + dest.latitude
				+ dest.longitude;
		if (waypoints != null && waypoints.size() > 0) {
		    url += "&waypoints=";
		    int i = 0;
		    for (i = 0; i < waypoints.size() - 1; i++)
		    	url += waypoints.get(i).lat + "," + waypoints.get(i).lng + "|";
		    url += waypoints.get(i).lat + "," + waypoints.get(i).lng;
		}
		System.out.println(url);
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

    private void speak(String toSpeak) {
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
    
    class GetPOITask extends TimerTask {
    	public void run() {
    		if (mCurrentLocation != null)
    	        new SpeakPOITask().execute();
    	}
    }

    class SpeakPOITask extends AsyncTask<String, Void, List<Trip.POI>> {

		@Override
		protected List<Trip.POI> doInBackground(String... args) {
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
				List<Trip.POI> result = new ArrayList<Trip.POI>();
				for (int i = 0; i < POIs.length(); i++) {
					if (POIs.get(i) == null)
						continue;
					JSONObject POI = POIs.getJSONObject(i);
					JSONObject loc = POI.getJSONObject("location");
					Location loc_ = new Location(mCurrentLocation);
					loc_.setLatitude(loc.getDouble("lat"));
					loc_.setLongitude(loc.getDouble("lng"));
					POI POI_ = new POI(POI.getString("name"), loc_, POI.getString("summary"));
					result.add(POI_);
			    }
			    return result;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

        @Override
		protected void onPostExecute(List<POI> POIs) {
        	if (POIs != null)
        	    trip.setNewPOIs(POIs);
        }
    }
    
    class Speaker extends Thread {
		@Override
		public void run() {
			while (true) {
				if (!ttobj.isSpeaking() && !trip.pois.isEmpty()) {
					POI poi = trip.pois.remove(0);
					speak(poi.summary);
				}
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {}
			}
		}
    }
}
