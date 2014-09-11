package com.cambeyer.shareltu;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.cambeyer.shareltu.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore.MediaColumns;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity {
	
	public static final String SERVER_URL = "http://betterdriving.riis.com:8080/ShareLTU/upload";

    public static final String PROPERTY_REG_ID = "registration_id";		//used for storing shared prefs
    private static final String PROPERTY_APP_VERSION = "appVersion";	//used for storing shared prefs
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    String SENDER_ID = "894263816119";

    static final String TAG = "ShareLTU";

    GoogleCloudMessaging gcm;
    SharedPreferences prefs;
    Context context;

    public String regid;
    public String uuid;
    
    public String type;
	public boolean[] itemsChecked;
    public String recipients = "";
    public Uri fileUri;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);	
		
        context = getApplicationContext();
        
    	uuid = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
    	
    	Log.v(TAG, "UUID: " + uuid);


        // Check device for Play Services APK. If check succeeds, proceed with
        //  GCM registration.
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId(context);

            if (regid.isEmpty()) {
                registerInBackground();
            } else
            {
            	Log.v(TAG, "Reminder RegID: " + regid);
            }
            
            startSend();
        }
	}
	
	// You need to do the Play Services APK check here too.
	@Override
	protected void onResume() {
	    super.onResume();
	    checkPlayServices();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
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

	                // You should send the registration ID to your server over HTTP,
	                // so it can use GCM/HTTP or CCS to send messages to your app.
	                // The request to your server should be authenticated if your app
	                // is using accounts.
	                sendRegistrationIdToBackend();

	                // Persist the regID - no need to register again.
	                storeRegistrationId(context, regid);
	            } catch (IOException ex) {
	                // If there is an error, don't just keep trying to register.
	                // Require the user to click a button again, or perform
	                // exponential back-off.
	            }
	            return null;
	        }
	    }.execute(null, null, null);
	}
	
	/**
	 * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP
	 * or CCS to send messages to your app. Not needed for this demo since the
	 * device sends upstream messages to a server that echoes back the message
	 * using the 'from' address in the message.
	 */
	private void sendRegistrationIdToBackend() {
	    // Your implementation here.*************************
		// Server needs to persist this information... UUID + RegID
    	Log.v(TAG, "Sent RegID: " + regid);
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
	        if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
	            GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
	        } else {
	            finish();
	        }
	        return false;
	    }
	    return true;
	}
	
	public class AsyncLoader extends AsyncTask<Void, Void, Void>
    {        
        public boolean hideLoadingScreen;
        public ProgressDialog pdLoading;
        public String filename;

        
        @Override
        protected void onPreExecute() {
            pdLoading = new ProgressDialog(MainActivity.this);
            pdLoading.setMessage("\tUploading...");
            
	  	    try {
	            pdLoading.show();
	  	    } catch (Exception ex) {
	  	    }
	  	    
            filename = "";
        }
        
        @Override
        protected Void doInBackground(Void... params) {
        	
	    	doUpload(getInputStream(fileUri));

	        return null;
        }
        
        @Override
        protected void onPostExecute(Void result) {
          	try {
          		pdLoading.dismiss();
          	} catch(Exception ex) {
          	}
        }
        
    	public void doUpload(InputStream input)
    	{            	
	    	Log.v(TAG, "Type: " + type);
	    	Log.v(TAG, "Recipients: " + recipients);
    		String result = "";
    		try
    		{
    	        HttpClient client = new DefaultHttpClient();          
    	        HttpPost post = new HttpPost(SERVER_URL);
    	        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
    	        entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
    			
    	    	entityBuilder.addBinaryBody("uploadFile", input, ContentType.create(type), uuid + "_" + filename);
    	        entityBuilder.addTextBody("fromuuid", uuid);
    	        entityBuilder.addTextBody("touuid", recipients);
    	        entityBuilder.addTextBody("type", type);
    	        
    	        HttpEntity entity = entityBuilder.build();
    	        post.setEntity(entity);
    	        HttpResponse response = client.execute(post);
    	        HttpEntity httpEntity = response.getEntity();
    	        result = EntityUtils.toString(httpEntity);
    	        
    		} catch (Exception ex) {
    		}
            
            Log.v("result", result);
    	}
    	
    	@SuppressLint("SimpleDateFormat")
		public InputStream getInputStream(final Uri uri) {  	    
    		try
    		{
	    	    String[] projection = { MediaColumns.DATA };
	    	    Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
	    	    if(cursor != null) {
	    	    	
	    	        cursor.moveToFirst();
	    	        int columnIndex = cursor.getColumnIndexOrThrow(MediaColumns.DATA);
	    	        String filePath = cursor.getString(columnIndex);
	    	        cursor.close();
	    			if (filePath != null)
	    			{
	    				filename = new File(filePath).getName();
	    				return new FileInputStream(filePath);
	    			}
	    			else
	    			{
	    				filename = new Date().getTime() + "." + type.split("/")[1];
	    				Log.v(TAG, "Fetching data from: " + uri);
	    				return getContentResolver().openInputStream(uri);
	    			}
	    	    }
	    	    else 
	    	    {
    				filename = new File(uri.getPath()).getName();
	    	    	return new FileInputStream(uri.getPath());
	    	    }
    		} catch (Exception ex)
    		{
    		}          	
    		return null;
    	}
    } 
	
	public void chooseRecipients() {
		//************ fetch from server
    	ArrayList<String> uuids = new ArrayList<String>();
    	uuids.add("353918058381696");
    	uuids.add("99000114946589");
    	
    	final CharSequence[] items = uuids.toArray(new CharSequence[uuids.size()]);
    	itemsChecked = new boolean[items.length];
    	
    	AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
    	builder.setTitle("Choose Recipients");
    	
    	builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                for (int i = 0; i < items.length; i++) {
	                if (itemsChecked[i]) {
	                	recipients = recipients + items[i] + ",";
	                    itemsChecked[i] = false;
	                }
                }
    	    	AsyncLoader myLoader = new AsyncLoader();
    			myLoader.execute();
            }
        });
    	
    	builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
               finish();
            }
        });
    	
    	builder.setMultiChoiceItems(items, itemsChecked, new DialogInterface.OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
					itemsChecked[which] = isChecked;	
			}
		});
    	builder.show();
	}
	
	public void startSend() {
        Intent intent = getIntent();
        String action = intent.getAction();
        type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
        	try
	        {
        	    fileUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        	    if (fileUri != null)
        	    {
        	    	chooseRecipients();
        	    }
        	    else
        	    {
        	    	AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        	    	builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	    	            public void onClick(DialogInterface dialog, int id) {
	    	               finish();
	    	            }
	    	        });
        	    	builder.setMessage("You tried to share from an application that doesn't have a file to send.");
        	        builder.setTitle("No file selected");
        	    	builder.show();
        	    }
	        }
	        catch(Exception e)
	        {
	            e.printStackTrace();
	        }
        }
	}
}
