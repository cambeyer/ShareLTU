package com.cambeyer.shareltu;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.cambeyer.shareltu.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class DownloadActivity extends Activity {
	
	public static final String SERVER_URL = "http://betterdriving.riis.com:8080/ShareLTU/download";

    static final String TAG = "Download";
    Context context;
    String uuid;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_download);	
		
        context = getApplicationContext();
    	uuid = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
    	
    	AsyncLoader myLoader = new AsyncLoader();
		myLoader.execute();
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

	            Log.v(TAG, "Got filename " + filename + " from intent");
	            
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
    	        
    	        final File output = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "ShareLTU", filename.split("_", 2)[1]);
    	        
    	        FileUtils.writeByteArrayToFile(output, EntityUtils.toByteArray(httpEntity));
    	            	        
    	        Log.v(TAG, "Saving temp file at " + output.length() + " " + Uri.fromFile(output).toString());   
    	        
    	        Intent media = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
    	        media.setData(Uri.fromFile(output));
    	        sendBroadcast(media);
    	        
    	        final AlertDialog.Builder builder = new AlertDialog.Builder(DownloadActivity.this);
    	        final Intent intent = new Intent().setDataAndType(Uri.fromFile(output), type);
    	        final DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
    	            @Override
    	            public void onClick(DialogInterface dialog, int which) {
    	                switch (which){
    	                case DialogInterface.BUTTON_POSITIVE:
    	                    intent.setAction(Intent.ACTION_VIEW);
    	        	        startActivity(Intent.createChooser(intent, "View your file"));
    	        	        break;

    	                case DialogInterface.BUTTON_NEGATIVE:
    	                    intent.setAction(Intent.ACTION_SEND);
    	        	        startActivity(Intent.createChooser(intent, "Save your file"));
    	                    break;
    	                }
    	    	        finish();
    	            }
    	        };
    	        
    	        runOnUiThread(new Runnable() 
    	        {
    	            public void run() 
    	            {
    	    	        builder.setTitle("Save or View?").setMessage("You will be presented with options for which application you would like to use in either case.").setPositiveButton("View", dialogClickListener).setNegativeButton("Save", dialogClickListener).show();
    	            }
    	        });
    	            	            	        
    		} catch (Exception ex) {
    			ex.printStackTrace();
    		}
            
            Log.v("result", result);
    	}
    } 
}
