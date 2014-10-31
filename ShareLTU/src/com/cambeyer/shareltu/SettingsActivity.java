package com.cambeyer.shareltu;

import com.cambeyer.shareltu.R;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class SettingsActivity extends Activity {

    static final String TAG = "Settings";
    Context context;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);	
		
		getActionBar().setTitle("Settings"); 
		
		((TextView) findViewById(R.id.minTime)).setText(((Long.valueOf(getSharedPreferences(MainActivity.class.getSimpleName(), MODE_PRIVATE).getString("minTime", "180000"))) / 1000 / 60) + "");
		((TextView) findViewById(R.id.minDist)).setText(getSharedPreferences(MainActivity.class.getSimpleName(), MODE_PRIVATE).getString("minDistance", "500"));
				
        context = getApplicationContext();
	}
	
	public void save(View view) {
		
	    getSharedPreferences(MainActivity.class.getSimpleName(), MODE_PRIVATE).edit().putString("minTime", (Long.valueOf(((TextView) findViewById(R.id.minTime)).getText().toString()) * 1000 * 60) + "").commit();
	    getSharedPreferences(MainActivity.class.getSimpleName(), MODE_PRIVATE).edit().putString("minDistance", ((TextView) findViewById(R.id.minDist)).getText().toString()).commit();
	    LocationService.requestLocationUpdates();
	    finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
