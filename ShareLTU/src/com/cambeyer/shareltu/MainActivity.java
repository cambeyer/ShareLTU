package com.cambeyer.shareltu;

import java.io.File;
import java.io.IOException;

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
import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity {
	
    GoogleCloudMessaging gcm;
    String regid;
    String PROJECT_NUMBER = "894263816119";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(getApplicationContext());
                    }
                    regid = gcm.register(PROJECT_NUMBER);
                    msg = "Device registered, registration ID=" + regid;
                    Log.i("GCM",  msg);

                } catch (Exception ex) {
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
            	AsyncLoader myLoader = new AsyncLoader();
        		myLoader.execute();
            }
        }.execute(null, null, null);
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
	
	public class AsyncLoader extends AsyncTask<Void, Void, Void>
    {        
        public boolean hideLoadingScreen;
        public ProgressDialog pdLoading;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            
            pdLoading = new ProgressDialog(MainActivity.this);
            pdLoading.setMessage("\tUploading...");
            	    	            
	  	    try {
	            pdLoading.show();
	  	    } catch (Exception ex) {
	  	    }
        }
        
        @Override
        protected Void doInBackground(Void... params) {
	        Intent intent = getIntent();
	        String action = intent.getAction();
	        String type = intent.getType();

	        if (Intent.ACTION_SEND.equals(action) && type != null) {
	        	try
		        {
	        	    Uri fileUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
	        	    if (fileUri != null)
	        	    {
			            HttpClient client = new DefaultHttpClient();          
			            HttpPost post = new HttpPost("http://10.0.0.254:8080/ShareLTU/upload");
			            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
			            entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			            File file = new File(fileUri.getPath());

			            if(file != null)
			            {
			            	String uuid = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
			            	entityBuilder.addBinaryBody("uploadFile", file, ContentType.create(type), uuid + "_" + file.getName());
				            entityBuilder.addTextBody("UUID", uuid);
				            entityBuilder.addTextBody("fileName", file.getName());
				            
				            HttpEntity entity = entityBuilder.build();
				            post.setEntity(entity);
				            HttpResponse response = client.execute(post);
				            HttpEntity httpEntity = response.getEntity();
				            String result = EntityUtils.toString(httpEntity);
				            
				            Log.v("result", result);
			            }
	        	    }
		        }
		        catch(Exception e)
		        {
		            e.printStackTrace();
		        }
	        }
	        return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            
          	try {
          		pdLoading.dismiss();
          	} catch(Exception ex) {
          	}
        }
    } 
}
