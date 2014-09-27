package com.cambeyer.shareltu;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.LinearLayout;
import android.widget.ListView;

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
        	    	checkLocationStatus();
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
	
	public void checkLocationStatus()
	{
		if (LocationService.lastLocation == null)
		{
			Log.v(TAG, "The location is null.  Seems like a GPS issue");
	    	AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
	    	builder.setTitle("No GPS Data");
	    	
	    	builder.setMessage("It appears as though the GPS has not given us any information to locate you!  Check your GPS settings to ensure the ShareLTU app can properly triage your file!");
	    	
	    	builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int id) {
	               finish();
	            }
	        });
	    	
	    	builder.setNegativeButton("Refresh", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int id) {
	               checkLocationStatus();
	            }
	        });
	    	
	    	builder.show();
	    	return;
		}
		else //if (LocationService.calcMinutes(LocationService.lastSubmitted, new Date()) > 5)
		{
			Log.v(TAG, "The location isn't brand new. Requesting a new list of people from the server");
			
		    new AsyncTask<Void, Void, Void>() {
		    	
		        public ProgressDialog pdLoading;
		    	
		        @Override
		        protected void onPreExecute() {
		            pdLoading = new ProgressDialog(MainActivity.this);
		            pdLoading.setMessage("\tFetching Potential Recipients...");
		            pdLoading.setCancelable(false);
		            pdLoading.setCanceledOnTouchOutside(false);
		            
			  	    try {
			            pdLoading.show();
			  	    } catch (Exception ex) {
			  	    }
		        }
		        @Override
		        protected Void doInBackground(Void... params) {
					LocationService.fetchCandidateRecipientsFromServer(LocationService.lastLocation);
		            return null;
		        }
		        @Override
		        protected void onPostExecute(Void result) {
		          	try {
		          		pdLoading.dismiss();
		          	} catch(Exception ex) {
		          	}
					chooseRecipients();
		        }
		    }.execute();
		}
//		else
//		{
//			Log.v(TAG, "The location isn't null and isn't old, so we should be good to go");
//			chooseRecipients();
//		}
	}
	
	public void chooseRecipients()
	{
		if (LocationService.uuids.size() > 0) {
									
	        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
	        final List<ListItemWithIndex> allItems = new ArrayList<ListItemWithIndex>();
	        final List<ListItemWithIndex> filteredItems = new ArrayList<ListItemWithIndex>();

	        for (int i = 0; i < LocationService.names.size(); i++) {
	            final ListItemWithIndex listItemWithIndex = new ListItemWithIndex(i, LocationService.names.get(i));
	            allItems.add(listItemWithIndex);
	            filteredItems.add(listItemWithIndex);
	        }

	        dialogBuilder.setTitle("Choose Recipients");
	        	        
	        final ArrayAdapter<ListItemWithIndex> objectsAdapter = new ArrayAdapter<ListItemWithIndex>(this, android.R.layout.simple_list_item_multiple_choice, filteredItems) {
	            @Override
	            public Filter getFilter() {
	                return new Filter() {
	                    @SuppressWarnings("unchecked")
	                    @Override
	                    protected void publishResults(final CharSequence constraint, final FilterResults results) {
	                        filteredItems.clear();
	                        filteredItems.addAll((List<ListItemWithIndex>) results.values);
	                        notifyDataSetChanged();
	                    }

	                    @Override
	                    protected FilterResults performFiltering(final CharSequence constraint) {
	                        final FilterResults results = new FilterResults();

	                        final String filterString = constraint.toString();
	                        final ArrayList<ListItemWithIndex> list = new ArrayList<ListItemWithIndex>();
	                        for (final ListItemWithIndex obj : allItems) {
	                            final String objStr = obj.toString();
	                            if ("".equals(filterString) || objStr.toLowerCase(Locale.getDefault()).contains(filterString.toLowerCase(Locale.getDefault()))) {
	                                list.add(obj);
	                            }
	                        }

	                        results.values = list;
	                        results.count = list.size();
	                        return results;
	                    }
	                };
	            }
	        };
	        
	        final ListView listView = new ListView(this);
	        listView.addOnLayoutChangeListener(new OnLayoutChangeListener() {
				@Override
				public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
	                for (int i = 0; i < listView.getChildCount(); i++) {
	                	((CheckedTextView) listView.getChildAt(i)).setChecked(allItems.get(filteredItems.get(i).index).selected);
	                }
				}
	        });
	        listView.setAdapter(objectsAdapter);

	        final EditText searchEditText = new EditText(this);
	        searchEditText.addTextChangedListener(new TextWatcher() {
	            @Override
	            public void onTextChanged(final CharSequence arg0, final int arg1, final int arg2, final int arg3) {
	            }

	            @Override
	            public void beforeTextChanged(final CharSequence arg0, final int arg1, final int arg2, final int arg3) {
	            }

	            @Override
	            public void afterTextChanged(final Editable arg0) {
	                objectsAdapter.getFilter().filter(searchEditText.getText());
	            }
	        });

	        final LinearLayout linearLayout = new LinearLayout(this);
	        linearLayout.setOrientation(LinearLayout.VERTICAL);
	        linearLayout.addView(searchEditText, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
	        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0);
	        layoutParams.weight = 1;
	        linearLayout.addView(listView, layoutParams);
	        dialogBuilder.setView(linearLayout);
	        
	    	dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int which) {
	            	for (int i = 0; i < allItems.size(); i++) {
	            		if (allItems.get(i).selected){
	            			recipients += LocationService.uuids.get(i) + ",";
	            		}
	            	}
	    	    	new AsyncLoader().execute();
	            }
	    	});
	    	dialogBuilder.setNeutralButton("Refresh", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int id) {
	               checkLocationStatus();
	            }
	    	});
	    	dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int id) {
	               finish();
	            }
	    	});
	    	
	        final AlertDialog dialog = dialogBuilder.create();
	        listView.setOnItemClickListener(new OnItemClickListener() {
	            @Override
	            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
	                CheckedTextView check = (CheckedTextView) view;
	                boolean goingToSet = !check.isChecked();
	                check.setChecked(goingToSet);
	                allItems.get(filteredItems.get(position).index).selected = goingToSet;
	            }
	        });
	        dialog.show();
		}
		else
		{
	    	AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
	    	builder.setTitle("No Recipients Nearby");
	    	
	    	builder.setMessage("It appears as though there is no one nearby!");
	    	
	    	builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int id) {
	               finish();
	            }
	        });
	    	
	    	builder.setNegativeButton("Refresh", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int id) {
	               checkLocationStatus();
	            }
	        });
	    	
	    	builder.show();
		}
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
	        return null;
        }
        
        @Override
        protected void onPostExecute(Void result) {
          	try {
          		pdLoading.dismiss();
          	} catch(Exception ex) {
          	}
          	
	    	finish();
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
	
	private static final class ListItemWithIndex {
        public final int index;
        public final String value;
        public boolean selected;

        public ListItemWithIndex(final int index, final String value) {
            super();
            selected = false;
            this.index = index;
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
