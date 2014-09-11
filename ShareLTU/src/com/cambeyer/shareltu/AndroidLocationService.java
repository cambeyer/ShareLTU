package com.cambeyer.shareltu;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class AndroidLocationService extends Service {

	WakeLock wakeLock;
	
	public static final String SERVER_URL = "http://betterdriving.riis.com:8080/ShareLTU/do";
	
    static final String TAG = "LocationService";
	
	private LocationManager locationManager;
	
	@Override
	public IBinder onBind(Intent arg0) {
	    return null;
	}
	
	@Override
	public void onCreate() {
	    super.onCreate();
	
	    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
	
	    wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DoNotSleep");
	
	    Log.v(TAG, "Service Created");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	
	    Log.v(TAG, "Service Started");
	
	    locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
	    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1 * 60 * 1000, 10, listener);
	    
	    return START_STICKY;
	}
	
	private LocationListener listener = new LocationListener() {
	
	    @Override
	    public void onLocationChanged(Location location) {
	
	        Log.v(TAG, "Location Changed");
	
	        if (location == null)
	            return;
	
	        if (isConnectedToInternet(getApplicationContext())) {
	            JSONArray jsonArray = new JSONArray();
	            JSONObject jsonObject = new JSONObject();
	
	            try {
	                Log.e("latitude", location.getLatitude() + "");
	                Log.e("longitude", location.getLongitude() + "");
	
	                jsonObject.put("latitude", location.getLatitude());
	                jsonObject.put("longitude", location.getLongitude());
	
	                jsonArray.put(jsonObject);
	
	                Log.e("request", jsonArray.toString());
	
	                new LocationWebService().execute(new String[] { SERVER_URL, jsonArray.toString() });
	            } catch (Exception e) {
	            }
	        }
	    }
	
	    @Override
	    public void onProviderDisabled(String provider) {
	    }
	
	    @Override
	    public void onProviderEnabled(String provider) {
	    }
	
	    @Override
	    public void onStatusChanged(String provider, int status, Bundle extras) {
	    }
	};
	
	@Override
	public void onDestroy() {
	    super.onDestroy();
	
	    wakeLock.release();
	}
	
	public static boolean isConnectedToInternet(Context context) {
	    ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    if (connectivity != null) {
	        NetworkInfo[] info = connectivity.getAllNetworkInfo();
	        if (info != null)
	        {
	            for (int i = 0; i < info.length; i++)
	            {
	                if (info[i].getState() == NetworkInfo.State.CONNECTED) {
	                    return true;
	                }
	            }
	        }
	
	    }
	    return false;
	}

	public class LocationWebService extends AsyncTask<String, String, Boolean> {

		public LocationWebService() {
		}

		@Override
		protected Boolean doInBackground(String... args) {
			
			Log.v(TAG, "About to contact server");
//
//		    ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
//		    nameValuePairs.add(new BasicNameValuePair("location", args[1]));
//
//		    HttpClient httpclient = new DefaultHttpClient();
//		    HttpPost httppost = new HttpPost(args[0]);
//		    HttpParams httpParameters = new BasicHttpParams();
//
//		    httpclient = new DefaultHttpClient(httpParameters);
//
//		    try {
//		        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
//
//		        HttpResponse response;
//		        response = httpclient.execute(httppost);
//		        StatusLine statusLine = response.getStatusLine();
//		        if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
//
//		            Log.v(TAG, "Server Responded OK");
//
//		        } else {
//
//		            response.getEntity().getContent().close();
//		            throw new IOException(statusLine.getReasonPhrase());
//		        }
//		    } catch (Exception e) {
//		        e.printStackTrace();
//		    }
		    return null;
		}
	}
}