package com.cambeyer.shareltu;

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
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;

public class LocationService extends Service {

	WakeLock wakeLock;
	
	public static final String SERVER_URL = "http://betterdriving.riis.com:8080/ShareLTU/upload";
    String SENDER_ID = "894263816119";
	
    static final String TAG = "LocationService";
    
    public static String recentLat = "";
    public static String recentLon = "";
    
    public static ArrayList<String> uuids = new ArrayList<String>();
    public static ArrayList<String> names = new ArrayList<String>();
	
	private LocationManager locationManager;
	
    public static final String PROPERTY_REG_ID = "registration_id";		//used for storing shared prefs
    private static final String PROPERTY_APP_VERSION = "appVersion";	//used for storing shared prefs
    
    private GoogleCloudMessaging gcm;
    private static Context context;
    
    public static String regid;
    public static String uuid;
    public static String name;
	
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
	    uuid = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
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
	
	public void doCommunication()
	{
		Log.v(TAG, "regid: " + regid);
		
	    locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
	    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5 * 60 * 1000, 500, listener);
	    
	    locChanged(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
	}
	
	public static void locChanged(Location location) {
        Log.v(TAG, "Location Changed");
    	
        if (location == null)
        {
        	Log.v(TAG, "It was NULL");
            return;
        }
        
        recentLat = location.getLatitude() + "";
        recentLon = location.getLongitude() + "";
        
        Log.v(TAG, "lat: " + recentLat + ", lon: " + recentLon);

        if (isConnectedToInternet(context)) {
            try {
        	    new AsyncTask<Void, Void, Void>() {
        	        @Override
        	        protected Void doInBackground(Void... params) {
        				
        				Log.v(TAG, "About to contact server");
        				
        				try {
        			        HttpClient client = new DefaultHttpClient();          
        			        HttpPost post = new HttpPost(SERVER_URL);
        			        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
        			        entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        					
        			        entityBuilder.addTextBody("name", name);
        			        entityBuilder.addTextBody("uuid", uuid);
        			        entityBuilder.addTextBody("regid", regid);
        			        entityBuilder.addTextBody("lat", recentLat);
        			        entityBuilder.addTextBody("lon", recentLon);
        			        
        			        HttpEntity entity = entityBuilder.build();
        			        post.setEntity(entity);
        			        HttpResponse response = client.execute(post);
        			        HttpEntity httpEntity = response.getEntity();
        			        String result = EntityUtils.toString(httpEntity);
        			        
        			        Log.v(TAG, "Received: " + result);
        			        
        			        if (!result.isEmpty())
        			        {
            			        names.clear();
            			        uuids.clear();
        			        
	        			        String[] chunks = result.split(",");
	        			        for (int i = 0; i < chunks.length; i++)
	        			        {
	        			        	uuids.add(chunks[i].split("_", 2)[0]);
	        			        	names.add(chunks[i].split("_", 2)[1]);
        			        }
        			        }
        				}
        				catch (Exception ex)
        				{
        					ex.printStackTrace();
        				}
        				
        			    return null;
        	        }
        	    }.execute();
            } catch (Exception e) {
            }
        }
	}
	
	private LocationListener listener = new LocationListener() {
	
	    @Override
	    public void onLocationChanged(Location location) {
	    	locChanged(location);
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