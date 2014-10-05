package com.example.waypal;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

public class HomeActivity extends Activity {
	
	public final static String EXTRA_MESSAGE = "com.example.waypal.MESSAGE";
	
    TextToSpeech ttobj;
    Location mCurrentLocation;
	Firebase myFirebaseRef;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
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
        
        // Setup firebase
		Firebase.setAndroidContext(this);
		myFirebaseRef = new Firebase("https://waypal.firebaseio.com/");
		myFirebaseRef.child("trip1").addValueEventListener(new ValueEventListener() {

			  @Override
			  public void onDataChange(DataSnapshot snapshot) {
			    System.out.println(snapshot.getValue());  //prints "Do you have data? You'll love Firebase."
			  }

			  @Override public void onCancelled(FirebaseError error) { }

		});
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
	
	/*
	public void sendMessage(View view) {
		Intent intent = new Intent(this, DisplayMessageActivity.class);
		EditText editText = (EditText) findViewById(R.id.edit_message);
		String message = editText.getText().toString();
		intent.putExtra(EXTRA_MESSAGE, message);
		startActivity(intent);
	}*/

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
        
		Intent intent = new Intent(this, MainActivity.class);
//		EditText editText = (EditText) findViewById(R.id.destination_message);
//		String message = editText.getText().toString();
//		intent.putExtra(EXTRA_MESSAGE, message);
		startActivity(intent);
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
	
    class CustomLocationListener implements LocationListener {

        private Context m_context;

        public CustomLocationListener(Context context) {
            m_context = context;
        }

        @Override
        public void onLocationChanged(Location location) {
            mCurrentLocation = location;
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
