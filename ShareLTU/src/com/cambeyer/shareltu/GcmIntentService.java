package com.cambeyer.shareltu;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class GcmIntentService extends IntentService {
	
    static final String TAG = "IntentService";

    public static int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
    	
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't
             * recognize.
             */
        	if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {

        		// Post notification of received message.
        		String sendername = extras.getString("sendername");
        		String fromuuid = extras.getString("fromuuid");
        		String filename = extras.getString("filename");
        		String type = extras.getString("type");
        		
        		if (!getSharedPreferences(MainActivity.class.getSimpleName(), MODE_PRIVATE).getString("blocked", "").contains(fromuuid)) {
        			sendNotification(sendername + " sends " + filename.split("_", 2)[1], sendername, fromuuid, filename, type);
        		}
                
                Log.i(TAG, "Received: " + extras.toString());
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    // Put the message into a notification and post it.
    private void sendNotification(String msg, String sendername, String fromuuid, String filename, String type) {
        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent i = new Intent(this, DownloadActivity.class);
        i.putExtra("sendername", sendername);
        i.putExtra("fromuuid", fromuuid);
        i.putExtra("filename", filename);
        i.putExtra("type", type);
        
        Log.v(TAG, "Adding filename " + filename + " to intent");
        
        PendingIntent contentIntent = PendingIntent.getActivity(this, NOTIFICATION_ID, i, PendingIntent.FLAG_CANCEL_CURRENT);

        Notification notification = new NotificationCompat.Builder(this)
	        .setSmallIcon(R.drawable.ic_launcher)
	        .setContentTitle("Incoming file transfer")
	        .setStyle(new NotificationCompat.BigTextStyle()
	        .bigText(msg))
	        .setContentText(msg)
	        .setContentIntent(contentIntent)
	        .setAutoCancel(true)
	        .build();
        
        notification.defaults |= Notification.DEFAULT_ALL;

        mNotificationManager.notify(NOTIFICATION_ID++, notification);
    }
}