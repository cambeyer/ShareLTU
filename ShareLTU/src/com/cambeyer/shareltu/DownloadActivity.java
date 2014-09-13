package com.cambeyer.shareltu;

import java.io.File;
import java.util.ArrayList;

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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ListActivity;
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
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.ListView;

@SuppressLint("DefaultLocale")
public class DownloadActivity extends ListActivity {
	
	public static final String SERVER_URL = "http://betterdriving.riis.com:8080/ShareLTU/download";
	public static final String DOWNLOADS_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator;

    static final String TAG = "Download";
    Context context;
    String uuid;
    
    public String sendername;
    public String filename;
    public String type;
    
    ArrayList<String> listItems = new ArrayList<String>();
    ArrayAdapter<String> adapter;
    
	DialogInterface.OnClickListener acceptDownloadClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
            case DialogInterface.BUTTON_POSITIVE:
            	AsyncLoader myLoader = new AsyncLoader();
        		myLoader.execute();
    	        break;
            case DialogInterface.BUTTON_NEGATIVE:
            	finish();
                break;
            }
        }
	};
	
	@Override
	protected void onNewIntent(Intent newintent) {
		kickoff(newintent);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_download);	
		
		getActionBar().setTitle("Previous Downloads");  
		
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listItems);
	    setListAdapter(adapter);
	    buildFileList();
		
        context = getApplicationContext();
    	uuid = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
    	
    	kickoff(getIntent());
	}
	
	public void kickoff(Intent intent) {
        Bundle extras = intent.getExtras();
        
        if (extras != null) {
        	
        	sendername = extras.getString("sendername");
        	filename = extras.getString("filename");
            type = extras.getString("type");

            new AlertDialog.Builder(DownloadActivity.this)
	        .setTitle("Accept Download?")
	        .setMessage("Would you like to download the file \"" + filename.split("_", 2)[1] + "\" from " + sendername + "?")
	        .setPositiveButton("Accept", acceptDownloadClickListener)
	        .setNegativeButton("Decline", acceptDownloadClickListener)
	        .show();
        }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
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
	
	@Override
	 public void onListItemClick(ListView l, View v, int position, long id) {
		File selected = new File(DOWNLOADS_PATH + "ShareLTU", (String) getListView().getItemAtPosition(position));
	    presentOptions(selected, MimeTypeMap.getSingleton().getMimeTypeFromExtension(selected.getName().substring(selected.getName().lastIndexOf('.') + 1).toLowerCase()));
	 }
	
	public void buildFileList() {
        String path = DOWNLOADS_PATH + "ShareLTU";
		File f = new File(path);        
		File file[] = f.listFiles();
		for (int i = 0; i < file.length; i++)
		{
			adapter.add(file[i].getName());
		}
	}
	
	public void refreshList() {
        adapter.clear();
        buildFileList();
	}
	
	public void presentOptions(final File output, final String type) {
		
        runOnUiThread(new Runnable() 
        {
            public void run() 
            {
            	refreshList();
    	        
            	new AlertDialog.Builder(DownloadActivity.this)
    	        .setTitle("Send, Delete, or View?")
    	        .setMessage("Which action would you like to perform on this file?")
    	        .setPositiveButton("View", dialogClickListener)
    	        .setNeutralButton("Delete", dialogClickListener)
    	        .setNegativeButton("Send", dialogClickListener)
    	        .show();
            }
            
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent();
                    switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                    	intent.setDataAndType(Uri.fromFile(output), type);
                        intent.setAction(Intent.ACTION_VIEW);
            	        startActivity(Intent.createChooser(intent, "View your file"));
            	        break;
                    case DialogInterface.BUTTON_NEUTRAL:
                    	output.delete();
                    	refreshList();
                    	break;
                    case DialogInterface.BUTTON_NEGATIVE:
                    	intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(output));
                    	intent.setType(type);
                        intent.setAction(Intent.ACTION_SEND);
            	        startActivity(Intent.createChooser(intent, "Send your file"));
                        break;
                    }
                }
            };
        });
	}

	public class AsyncLoader extends AsyncTask<Void, Void, Void>
    {        
        public boolean hideLoadingScreen;
        public ProgressDialog pdLoading;
        
        @Override
        protected void onPreExecute() {
            pdLoading = new ProgressDialog(DownloadActivity.this);
            pdLoading.setMessage("\tDownloading...");
            pdLoading.setCancelable(false);
            pdLoading.setCanceledOnTouchOutside(false);
            
	  	    try {
	            pdLoading.show();
	  	    } catch (Exception ex) {
	  	    }
        }
        
        @Override
        protected Void doInBackground(Void... params) {
    	    doDownload();
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
    	        
    	        File output = new File(DOWNLOADS_PATH + "ShareLTU", filename.split("_", 2)[1]);
    	        
    	        FileUtils.writeByteArrayToFile(output, EntityUtils.toByteArray(httpEntity));
    	            	        
    	        Log.v(TAG, "Saving file with size " + output.length() + " at " + Uri.fromFile(output).toString());   
    	        
    	        Intent media = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
    	        media.setData(Uri.fromFile(output));
    	        sendBroadcast(media);
    	        
    	        presentOptions(output, type);
    	        
    		} catch (Exception ex) {
    			ex.printStackTrace();
    		}
            
            Log.v("result", result);
    	}
    } 
}
