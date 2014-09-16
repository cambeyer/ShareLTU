package com.cambeyer.shareltu;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.view.Menu;
import android.view.View;

public class MainActivity extends Activity {
	
	public static final String SERVER_URL = "http://betterdriving.riis.com:8080/ShareLTU/upload";

    static final String TAG = "ShareLTU";
    
    public String type;
	public boolean[] itemsChecked;
    public String recipients = "";
    public Uri fileUri;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);	
		        
        startService(new Intent(MainActivity.this, LocationService.class));
        
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
        	    	(new AlertDialog.Builder(MainActivity.this))
	        	    	.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		    	            public void onClick(DialogInterface dialog, int id) {
		    	               finish();
		    	            }
		    	        })
		    	        .setMessage("You tried to share from an application that doesn't have a file to send.")
	        	        .setTitle("No file selected")
	        	    	.show();
        	    }
	        }
	        catch(Exception e)
	        {
	            e.printStackTrace();
	        }
        }
	}
	
	public void chooseRecipients()
	{
		//************** we're relying on the fact that the locationservice has gotten a location, posted it, and got a response of nearby candidates
    	final CharSequence[] namelist = LocationService.names.toArray(new CharSequence[LocationService.names.size()]);
    	final CharSequence[] uuidlist = LocationService.uuids.toArray(new CharSequence[LocationService.uuids.size()]);
    	itemsChecked = new boolean[namelist.length];
    	
    	AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
    	builder.setTitle("Choose Recipients");
    	
    	builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                for (int i = 0; i < namelist.length; i++) {
	                if (itemsChecked[i]) {
	                	recipients = recipients + uuidlist[i] + ",";
	                    itemsChecked[i] = false;
	                }
                }
    	    	new AsyncLoader().execute();
            }
        });
    	
    	builder.setNeutralButton("Refresh", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
               chooseRecipients();
            }
        });
    	
    	builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
               finish();
            }
        });
    	
    	builder.setMultiChoiceItems(namelist, itemsChecked, new DialogInterface.OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
					itemsChecked[which] = isChecked;	
			}
		});
    	builder.show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}
	
	public void downloadPage(View view) {
	    startActivity(new Intent(this, DownloadActivity.class));
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
            pdLoading.setCancelable(false);
            pdLoading.setCanceledOnTouchOutside(false);
            
	  	    try {
	            pdLoading.show();
	  	    } catch (Exception ex) {
	  	    }
	  	    
            filename = "";
        }
        
        @Override
        protected Void doInBackground(Void... params) {
        	
	    	doUpload(getInputStream(fileUri));
	    	finish();

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
    			
    	    	entityBuilder.addBinaryBody("uploadFile", input, ContentType.create(type), LocationService.uuid + "_" + filename);
    	        entityBuilder.addTextBody("fromuuid", LocationService.uuid);
    	        entityBuilder.addTextBody("touuid", recipients);
    	        entityBuilder.addTextBody("type", type);
    	        
    	        HttpEntity entity = entityBuilder.build();
    	        post.setEntity(entity);
    	        HttpResponse response = client.execute(post);
    	        HttpEntity httpEntity = response.getEntity();
    	        result = EntityUtils.toString(httpEntity);
    	        
    		} catch (Exception ex) {
    			ex.printStackTrace();
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
}
