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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
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
import android.os.StrictMode;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
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
import com.google.android.gms.maps.model.PolylineOptions;

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
    Timer timer2;  // listens to user every once in a while
    Thread speaker;  // thread that keeps talking
    SpeechRecognizer sr;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		StrictMode.ThreadPolicy policy = new StrictMode.
				ThreadPolicy.Builder().permitAll().build();
				StrictMode.setThreadPolicy(policy); 
		setContentView(R.layout.activity_main);
		if (savedInstanceState == null) {
			mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
			poiFragment = (POIFragment) getFragmentManager().findFragmentById(R.id.pois);
			setUpMap();
		}

		this.trip = new Trip();

		// Setup Firebase listener
		Firebase.setAndroidContext(this);
		Map<String, String> tripInfo = new HashMap<String, String>();
		tripInfo.put("id", trip.id);
		myFirebaseRef = new Firebase("https://waypal.firebaseio.com/").child(trip.id);
		// Give a reference to the child fragment
		poiFragment.setFirebase(myFirebaseRef);
		poiFragment.setTrip(trip);
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
        timer2 = new Timer();
        timer2.scheduleAtFixedRate(new FeedbackTask(), 1000, 5000);
        
        speaker = new Speaker();
        speaker.start();
        
		ListView thisView = poiFragment.getListView();

        // Create a ListView-specific touch listener. ListViews are given special treatment because
        // by default they handle touches for their list items... i.e. they're in charge of drawing
        // the pressed state (the list selector), handling list item clicks, etc.
        SwipeDismissListViewTouchListener touchListener =
                new SwipeDismissListViewTouchListener(
                        thisView,
                        new SwipeDismissListViewTouchListener.DismissCallbacks() {
                            @Override
                            public boolean canDismiss(int position) {
                                return true;
                            }

                            @Override
                            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    poiFragment.removeItem(position);
                                }
                            }
                        });
        thisView.setOnTouchListener(touchListener);
        thisView.setOnScrollListener(touchListener.makeScrollListener());
        
        sr = SpeechRecognizer.createSpeechRecognizer(this);       
        sr.setRecognitionListener(new Listener());
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
	
//	public void computeRoute(List<Waypoint> waypoints) {
//		Document doc = md.getDocument(start, dest, waypoints);
//		ArrayList<LatLng> directionPoint = md.getDirection(doc);
//        PolylineOptions rectLine = new PolylineOptions().width(3).color(
//                Color.RED);
//
//        for (int i = 0; i < directionPoint.size(); i++) {
//            rectLine.add(directionPoint.get(i));
//        }
//        Polyline polylin = map.addPolyline(rectLine);
//	}
	
	public void computeRoute(List<Waypoint> waypoints) {
		HttpClient httpClient = new DefaultHttpClient();
		String url = "http://maps.googleapis.com/maps/api/directions/json?origin="
				+ start.latitude + "," + start.longitude + "&destination=" + dest.latitude
				+ "," + dest.longitude;
		if (waypoints != null && waypoints.size() > 2) {
		    url += "&waypoints=";
		    int i = 0;
		    for (i = 1; i < waypoints.size() - 2; i++)
		    	url += waypoints.get(i).lat + "," + waypoints.get(i).lng + "%7C";
		    url += waypoints.get(i - 1).lat + "," + waypoints.get(i - 1).lng;
		}
//		Toast.makeText(this, url, Toast.LENGTH_LONG).show();
//		System.out.println(url);
		try {
			HttpContext localContext = new BasicHttpContext();
			HttpPost httpPost = new HttpPost(url);
			HttpResponse response = httpClient.execute(httpPost, localContext);
			BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
			StringBuilder builder = new StringBuilder();
			for (String line = null; (line = reader.readLine()) != null;) {
			    builder.append(line).append("\n");
			}
			JSONTokener tokener = new JSONTokener(builder.toString());
			JSONObject finalResult = new JSONObject(tokener);
//			System.out.println(finalResult);
			JSONArray routes = finalResult.getJSONArray("routes");
			if (routes.length() < 1) {
				Toast.makeText(this, "No route found.", Toast.LENGTH_SHORT).show();
				return;
			}
//			Toast.makeText(this, "SUCCESS", Toast.LENGTH_SHORT).show();
			
			PolylineOptions polyline = new PolylineOptions();
			JSONObject route = routes.getJSONObject(0);
			JSONArray legs = route.getJSONArray("legs");
			for (int i = 0; i < legs.length(); i++) {
				JSONArray steps = legs.getJSONObject(i).getJSONArray("steps");
				for (int j = 0; j < steps.length(); j++) {
					JSONObject start = steps.getJSONObject(j).getJSONObject("start_location");
					polyline.add(parseLatLng(start));
				}
			}
//			System.out.println(polyline);
			map.addPolyline(polyline.geodesic(true));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private LatLng parseLatLng(JSONObject obj) {
	    try {
			return new LatLng(obj.getDouble("lat"), obj.getDouble("lng"));
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private List<LatLng> decodePoly(String encoded) {

		List<LatLng> poly = new ArrayList<LatLng>();
		int index = 0, len = encoded.length();
		int lat = 0, lng = 0;

		while (index < len) {
			int b, shift = 0, result = 0;
			do {
				b = encoded.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			} while (b >= 0x20);
			int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lat += dlat;

			shift = 0;
			result = 0;
			do {
				b = encoded.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			} while (b >= 0x20);
			int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lng += dlng;

			LatLng p = new LatLng((int) (((double) lat / 1E5) * 1E6),
				 (int) (((double) lng / 1E5) * 1E6));
			poly.add(p);
		}

		return poly;
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
    
	private void notifyPOIsChanged() {
		String[] items = new String[trip.pois.size()];
		for (int i = 0; i < items.length; i++) {
			if (trip.pois.get(i) == null)
				continue;
			items[i] = trip.pois.get(i).name;
		}
		poiFragment.setListItems(items);
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
//            Toast.makeText(m_context, Text, Toast.LENGTH_SHORT).show();
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

    class FeedbackTask extends TimerTask {
		public void run() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
			        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
			        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
			        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "voice.recognition.test");
			        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
			        sr.startListening(intent);
				}
			});
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
        	if (POIs != null) {
        	    trip.setNewPOIs(POIs);
        	    notifyPOIsChanged();
        	}
        }
    }
    
    class Speaker extends Thread {
		@Override
		public void run() {
	        POI poi = null;
			while (true) {
				if (!ttobj.isSpeaking() && !trip.pois.isEmpty()) {
					if (trip.pois.get(0) == poi)
					    trip.pois.remove(0);
					if (trip.pois.isEmpty())
						continue;
					poi = trip.pois.get(0);
					boolean inQueue = false;
					for(String name : poiFragment.data){
						if (name.equals(poi.name)) {
							inQueue = true;
						}
					}
					if (!inQueue) {
						continue;
					}
					runOnUiThread(new Runnable() {  
	                    @Override
	                    public void run() {
	                    	notifyPOIsChanged();
	                    }
	                });
					speak(poi.summary);
				}
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {}
			}
		}
    }

	class Listener implements RecognitionListener {
		public void onReadyForSpeech(Bundle params) {}
		public void onBeginningOfSpeech() {}
		public void onRmsChanged(float rmsdB) {}
		public void onBufferReceived(byte[] buffer) {}
		public void onEndOfSpeech() {}
		public void onError(int error) {}
		public void onPartialResults(Bundle partialResults) {}
		public void onEvent(int eventType, Bundle params) {}

		public void onResults(Bundle results) {
			List<String> data = results
					.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
			String yousaid = data.get(0);
			if (yousaid.equalsIgnoreCase("cool")) {
				ttobj.stop();
				speak("You're interested? Ok let's go!");
				poiFragment.pickWaypoint(0);
			} else if (yousaid.equalsIgnoreCase("next")) {
				ttobj.stop();
				speak("Bored? Ok next one then");
				poiFragment.removeItem(0);
			}
		}
	}
}
