package com.cambeyer.shareltu;

import java.util.Calendar;
import java.util.Date;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.ContactsContract;
import android.provider.Settings.Secure;
import android.util.Log;

public class LocationService extends Service {

	WakeLock wakeLock;
	
	public static final String SERVER_URL = "http://betterdriving.riis.com:8080/ShareLTU/upload";
    String SENDER_ID = "894263816119";
	
    static final String TAG = "LocationService";
    
    public static Location lastLocation = null;
    public static Date lastSubmitted = null;
    
    public static ArrayList<String> uuids = new ArrayList<String>();
    public static ArrayList<String> names = new ArrayList<String>();
    public static ArrayList<Double> distances = new ArrayList<Double>();
	
	private static LocationManager locationManager = null;
	private static LocationListener locationListener = null;
	
    public static final String PROPERTY_REG_ID = "registration_id";		//used for storing shared prefs
    private static final String PROPERTY_APP_VERSION = "appVersion";	//used for storing shared prefs
    
    private GoogleCloudMessaging gcm;
    private static Context context;
    
    public static String regid;
    public static String uuid;
    public static String name;
    public static String model;
	
	@Override
	public IBinder onBind(Intent arg0) {
	    return null;
	}
	
	@Override
	public void onCreate() {
	    super.onCreate();
	    
	    Log.v(TAG, "Service Created");	    
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	
	    Log.v(TAG, "Service Started");
	    
	    Cursor c = getContentResolver().query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);
	    int count = c.getCount();
	    String[] columnNames = c.getColumnNames();
	    c.moveToFirst();
	    int position = c.getPosition();
	    if (count == 1 && position == 0) {
	        for (int j = 0; j < columnNames.length; j++) {
	            if (columnNames[j].equals("display_name"))
	            {
	            	name = c.getString(c.getColumnIndex(columnNames[j]));
	            	Log.v(TAG, "Name: " + name);
	            }
	        }
	    }
	    c.close();
	    
        context = getApplicationContext();

        model = getDeviceName();
        Log.v(TAG, "Model: " + model);
        
        uuid = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
	    wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DoNotSleep");
	    
        // Check device for Play Services APK. If check succeeds, proceed with
        //  GCM registration.
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId(context);

            if (regid.isEmpty()) {
                registerInBackground();
            } else
            {
            	doCommunication();
            }
        }
	    
	    return START_STICKY;
	}
	
	public String getDeviceName() {
	  String manufacturer = Build.MANUFACTURER;
	  String model = Build.MODEL;
	  if (model.startsWith(manufacturer)) {
	    return capitalize(model);
	  } else {
	    return capitalize(manufacturer) + " " + model;
	  }
	}


	private String capitalize(String s) {
	  if (s == null || s.length() == 0) {
	    return "";
	  }
	  char first = s.charAt(0);
	  if (Character.isUpperCase(first)) {
	    return s;
	  } else {
	    return Character.toUpperCase(first) + s.substring(1);
	  }
	} 
	
	private void doCommunication()
	{		
		Log.v(TAG, "regid: " + regid);
		
		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

    	startLocationBackgroundTask(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));

    	requestLocationUpdates();	
	}
	
	public static void requestLocationUpdates() {

		long minTimeBetweenUpdatesms = Long.valueOf(context.getSharedPreferences(MainActivity.class.getSimpleName(), MODE_PRIVATE).getString("minTime", "180000"));
		float minDistanceBetweenUpdatesMeters = Float.valueOf(context.getSharedPreferences(MainActivity.class.getSimpleName(), MODE_PRIVATE).getString("minDistance", "500"));
		
		Log.v(TAG, "Time: " + minTimeBetweenUpdatesms + " and Distance: " + minDistanceBetweenUpdatesMeters);
		
		if (locationListener == null) {
			locationListener = new LocationListener() {
				@Override
				public void onLocationChanged(Location location) {
					startLocationBackgroundTask(location);						
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
		}
		else
		{
			locationManager.removeUpdates(locationListener);
		}
			
    	Criteria criteria = new Criteria();
        
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        
        criteria.setAltitudeRequired(false);
        criteria.setSpeedRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(false);
		
		String provider = locationManager.getBestProvider(criteria, true);
		Log.v(TAG, "Best provider: " + provider);
		
		locationManager.requestLocationUpdates(provider, minTimeBetweenUpdatesms, minDistanceBetweenUpdatesMeters, locationListener);
	}
	
	public static long calcMinutes(Date d1, Date d2) {
		if (d1 == null)
		{
			return 0;
		}
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(d1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(d2);
        long minutesBetween = 0;
        while (cal1.before(cal2))
        {
            cal1.add(Calendar.MINUTE, 1);
            minutesBetween++;
        }
        return minutesBetween;
	}
	
	public static void fetchCandidateRecipientsFromServer(Location location) {
        Log.v(TAG, "Location Changed");
    	
        if (location == null)
        {
        	Log.v(TAG, "It was NULL");
            return;
        }
        
        lastLocation = location;
        
        Log.v(TAG, "Minutes since last location update: " + calcMinutes(lastSubmitted, new Date()));
        lastSubmitted = new Date();
        
        Log.v(TAG, "lat: " + String.valueOf(lastLocation.getLatitude()) + ", lon: " + String.valueOf(lastLocation.getLongitude()));

        if (isConnectedToInternet(context)) {
            try {
				Log.v(TAG, "About to contact server");
				
		        HttpClient client = new DefaultHttpClient();          
		        HttpPost post = new HttpPost(SERVER_URL);
		        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
		        entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
				
		        entityBuilder.addTextBody("name", name);
		        entityBuilder.addTextBody("model", model);
		        entityBuilder.addTextBody("uuid", uuid);
		        entityBuilder.addTextBody("regid", regid);
		        entityBuilder.addTextBody("lat", String.valueOf(lastLocation.getLatitude()));
		        entityBuilder.addTextBody("lon", String.valueOf(lastLocation.getLongitude()));
		        
		        HttpEntity entity = entityBuilder.build();
		        post.setEntity(entity);
		        HttpResponse response = client.execute(post);
		        HttpEntity httpEntity = response.getEntity();
		        String result = EntityUtils.toString(httpEntity);
		        
		        Log.v(TAG, "Received: " + result);
		        
		        names.clear();
		        uuids.clear();
		        distances.clear();
		        
		        if (!result.isEmpty())
		        {
			        String[] chunks = result.split(",");
			        for (int i = 0; i < chunks.length; i++)
			        {
			        	uuids.add(chunks[i].split("_", 2)[0]);
			        	String tempName = chunks[i].split("_", 2)[1];
			        	names.add(tempName.split("\\|", 2)[0] + " (" + tempName.split("\\|", 2)[1].split("\\|", 2)[0] + ")");
			        	double distance = distFrom(Double.valueOf(tempName.split("\\|", 2)[1].split("\\|", 2)[1].split("\\|", 2)[0]), Double.valueOf(tempName.split("\\|", 2)[1].split("\\|", 2)[1].split("\\|", 2)[1]), lastLocation.getLatitude(), lastLocation.getLongitude());
			        	distances.add(distance);
			        	Log.v(TAG, "Distance: " + distance);
			        }
		        }
            } catch (Exception e) {
            }
        }
	}
	
	private static double distFrom(double lat1, double lng1, double lat2, double lng2) {
		double earthRadius = 6371; //kilometers
	    //double earthRadius = 3958.75; //miles
	    double dLat = Math.toRadians(lat2-lat1);
	    double dLng = Math.toRadians(lng2-lng1);
	    double a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng/2) * Math.sin(dLng/2);
	    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
	    return earthRadius * c * 1000; //kilometers to meters
	}
	
	public static void startLocationBackgroundTask(final Location location) {
	    new AsyncTask<Void, Void, Void>() {
	        @Override
	        protected Void doInBackground(Void... params) {
	        	fetchCandidateRecipientsFromServer(location);
	            return null;
	        }
	    }.execute();
	}
	
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
	
	/**
	 * Gets the current registration ID for application on GCM service.
	 * <p>
	 * If result is empty, the app needs to register.
	 *
	 * @return registration ID, or empty string if there is no existing
	 *         registration ID.
	 */
	private String getRegistrationId(Context context) {
	    final SharedPreferences prefs = getGCMPreferences(context);
	    String registrationId = prefs.getString(PROPERTY_REG_ID, "");
	    if (registrationId.isEmpty()) {
	        Log.i(TAG, "Registration not found.");
	        return "";
	    }
	    // Check if app was updated; if so, it must clear the registration ID
	    // since the existing regID is not guaranteed to work with the new
	    // app version.
	    int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
	    int currentVersion = getAppVersion(context);
	    if (registeredVersion != currentVersion) {
	        Log.i(TAG, "App version changed.");
	        return "";
	    }
	    return registrationId;
	}

	/**
	 * @return Application's {@code SharedPreferences}.
	 */
	private SharedPreferences getGCMPreferences(Context context) {
	    // This sample app persists the registration ID in shared preferences, but
	    // how you store the regID in your app is up to you.
	    return getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
	}
	
	/**
	 * @return Application's version code from the {@code PackageManager}.
	 */
	private static int getAppVersion(Context context) {
	    try {
	        PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
	        return packageInfo.versionCode;
	    } catch (NameNotFoundException e) {
	        // should never happen
	        throw new RuntimeException("Could not get package name: " + e);
	    }
	}
	
	/**
	 * Registers the application with GCM servers asynchronously.
	 * <p>
	 * Stores the registration ID and app versionCode in the application's
	 * shared preferences.
	 */
	private void registerInBackground() {
	    new AsyncTask<Void, Void, Void>() {
	        @Override
	        protected Void doInBackground(Void... params) {
	            try {
	                if (gcm == null) {
	                    gcm = GoogleCloudMessaging.getInstance(context);
	                }
	                regid = gcm.register(SENDER_ID);
	                
	                // Persist the regID - no need to register again.
	                storeRegistrationId(context, regid);

	            } catch (Exception ex) {
	                // If there is an error, don't just keep trying to register.
	                // Require the user to click a button again, or perform
	                // exponential back-off.
	            }
	            return null;
	        }
	        @Override
	        protected void onPostExecute(Void result) {
	        	doCommunication();
	        }
	    }.execute();
	}
	
	/**
	 * Stores the registration ID and app versionCode in the application's
	 * {@code SharedPreferences}.
	 *
	 * @param context application's context.
	 * @param regId registration ID
	 */
	private void storeRegistrationId(Context context, String regId) {
	    final SharedPreferences prefs = getGCMPreferences(context);
	    int appVersion = getAppVersion(context);
	    Log.i(TAG, "Saving regId on app version " + appVersion);
	    SharedPreferences.Editor editor = prefs.edit();
	    editor.putString(PROPERTY_REG_ID, regId);
	    editor.putInt(PROPERTY_APP_VERSION, appVersion);
	    editor.commit();
	}
	
	/**
	 * Check the device to make sure it has the Google Play Services APK. If
	 * it doesn't, display a dialog that allows users to download the APK from
	 * the Google Play Store or enable it in the device's system settings.
	 */
	private boolean checkPlayServices() {
	    int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
	    if (resultCode != ConnectionResult.SUCCESS) {
	        return false;
	    }
	    return true;
	}
}