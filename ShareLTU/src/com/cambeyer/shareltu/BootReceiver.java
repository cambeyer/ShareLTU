package com.cambeyer.shareltu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver 
{
    public void onReceive(Context arg0, Intent arg1) 
    {
        Intent intent = new Intent(arg0, LocationService.class);
        arg0.startService(intent);
        Log.v("Autostart", "Started background service");
    }
}