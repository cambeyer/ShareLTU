package com.cambeyer.shareltu;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.cambeyer.shareltu.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.ContentsResult;
import com.google.android.gms.drive.MetadataChangeSet;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class DownloadActivity extends Activity implements ConnectionCallbacks, OnConnectionFailedListener {
	
	public static final String SERVER_URL = "http://betterdriving.riis.com:8080/ShareLTU/download";
	
    private static final int REQUEST_CODE_CREATOR = 2;
    private static final int REQUEST_CODE_RESOLUTION = 3;

    private GoogleApiClient mGoogleApiClient;

    static final String TAG = "Download";
    Context context;
    String uuid;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_download);	
		
        context = getApplicationContext();
    	uuid = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
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
     * Create a new file and save it to Drive.
     */
    private void saveFileToDrive(final byte[] bytesToSave, final String filename, final String type) {
        // Start by creating a new contents, and setting a callback.
        Log.i(TAG, "Creating new contents.");
        Drive.DriveApi.newContents(mGoogleApiClient).setResultCallback(new ResultCallback<ContentsResult>() {

            @Override
            public void onResult(ContentsResult result) {
                // If the operation was not successful, we cannot do anything
                // and must
                // fail.
                if (!result.getStatus().isSuccess()) {
                    Log.i(TAG, "Failed to create new contents.");
                    return;
                }
                // Otherwise, we can write our data to the new contents.
                Log.i(TAG, "New contents created.");
                // Get an output stream for the contents.
                OutputStream outputStream = result.getContents().getOutputStream();
                try {
                    outputStream.write(bytesToSave);
                } catch (IOException e1) {
                    Log.i(TAG, "Unable to write file contents.");
                }
                // Create the initial metadata - MIME type and title.
                // Note that the user will be able to change the title later.
                MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                        .setMimeType(type).setTitle(filename).build();
                // Create an intent for the file chooser, and start it.
                IntentSender intentSender = Drive.DriveApi
                        .newCreateFileActivityBuilder()
                        .setInitialMetadata(metadataChangeSet)
                        .setInitialContents(result.getContents())
                        .build(mGoogleApiClient);
                try {
                    startIntentSenderForResult(
                            intentSender, REQUEST_CODE_CREATOR, null, 0, 0, 0);
                } catch (SendIntentException e) {
                    Log.i(TAG, "Failed to launch file chooser.");
                }
            }
        });
    }

    @Override
    protected void onResume() {
    	super.onResume();
    	if (mGoogleApiClient == null) {
    		// Create the API client and bind it to an instance variable.
            // We use this instance as the callback for connection and connection
            // failures.
            // Since no account name is passed, the user is prompted to choose.
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
    	}
        // Connect the client. Once connected, the camera is launched.
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_CREATOR:
                // Called after a file is saved to Drive.
                if (resultCode == RESULT_OK) {
                    Log.i(TAG, "File successfully saved.");
                }
                break;
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Called whenever the API client fails to connect.
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // show the localized error dialog.
            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this, 0).show();
            return;
        }
        // The failure has a resolution. Resolve it.
        // Called typically when the app is not yet authorized, and an
        // authorization
        // dialog is displayed to the user.
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "API client connected.");
    	AsyncLoader myLoader = new AsyncLoader();
		myLoader.execute();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }
	
	public class AsyncLoader extends AsyncTask<Void, Void, Void>
    {        
        public boolean hideLoadingScreen;
        public ProgressDialog pdLoading;
        public String sendername;
        public String filename;
        public String type;
        
        @Override
        protected void onPreExecute() {
            pdLoading = new ProgressDialog(DownloadActivity.this);
            pdLoading.setMessage("\tDownloading...");
            
	  	    try {
	            pdLoading.show();
	  	    } catch (Exception ex) {
	  	    }
	  	    
	  	    sendername = "";
            filename = "";
            type = "";
        }
        
        @Override
        protected Void doInBackground(Void... params) {
        	
	        Intent intent = getIntent();
	        Bundle extras = intent.getExtras();
	        
	        if (extras != null) {
	        	
	        	sendername = extras.getString("sendername");
	        	filename = extras.getString("filename");
	            type = extras.getString("type");
	            
	            //**************** validate whether they actually want to download or not
	            
    	    	doDownload();
	        }
	        return null;
        }
        
        @Override
        protected void onPostExecute(Void result) {
          	try {
          		pdLoading.dismiss();
          	} catch(Exception ex) {
          	}
        }
        
    	public void doDownload()
    	{            	    	            	  	    
    		String result = "";
    		try
    		{
    	        HttpClient client = new DefaultHttpClient();          
    	        HttpPost post = new HttpPost(SERVER_URL);
    	        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
    	        entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
    			
    	        entityBuilder.addTextBody("uuid", uuid);
    	        entityBuilder.addTextBody("filename", filename);
    	        
    	        HttpEntity entity = entityBuilder.build();
    	        post.setEntity(entity);
    	        HttpResponse response = client.execute(post);
    	        HttpEntity httpEntity = response.getEntity();
    	        result = EntityUtils.toString(httpEntity);
    	            	        
    		} catch (Exception ex) {
    			ex.printStackTrace();
    		}
            
            Log.v("result", result);
            saveFileToDrive(result.getBytes(), filename, type);
    	}
    } 
}
