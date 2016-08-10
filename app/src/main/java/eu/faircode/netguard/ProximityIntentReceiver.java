package eu.faircode.netguard;


/**
 * Created by Micha on 10.07.16.
 */


import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.LocationManager;
import android.widget.TextView;


public class ProximityIntentReceiver extends BroadcastReceiver {

    private static final int NOTIFICATION_ID = 1000;
    private static Context globalContext;

    private TextView textViewStatus;

    private LocationManager locationManager;
    private String provider;


    public void ProximityIntentReceiver(){
        return;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        globalContext=context;
        String key = LocationManager.KEY_PROXIMITY_ENTERING;

        Boolean entering = intent.getBooleanExtra(key, false);

        PendingIntent pendingIntent = PendingIntent.getActivity(globalContext.getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

/*
        //globalContext.getApplicationContext()
        if (entering) {
            //Create notifications
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            Notification notification = createNotification();
            //notification.setLatestEventInfo(context, "Proximity Alert!", "You are near your point of interest.", pendingIntent);
            notificationManager.notify(NOTIFICATION_ID, notification);
            setStatus("WORK MODE");
            //textViewStatus = (TextView) textViewStatus.findViewById(R.id.textViewStatus);
            //Toast.makeText(globalContext,"Entering Workplace. Time is: " + System.currentTimeMillis(), Toast.LENGTH_SHORT).show();
        } else {
            //Toast.makeText(globalContext,"exiting" + System.currentTimeMillis(), Toast.LENGTH_SHORT).show();
            setStatus("LEISURE MODE");
        }

        //globalContext.
        */
    }

    private Notification createNotification() {
        Notification notification = new Notification();

        //notification.icon = R.drawable.ic_menu_notifications;
        notification.when = System.currentTimeMillis();

        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.flags |= Notification.FLAG_SHOW_LIGHTS;

        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notification.defaults |= Notification.DEFAULT_LIGHTS;

        notification.ledARGB = Color.WHITE;
        notification.ledOnMS = 1500;
        notification.ledOffMS = 1500;
        return notification;
    }

    public void setStatus(String statusStr) {
        TextView textViewStatus = (TextView) ((Activity)globalContext).findViewById(R.id.textViewStatus);
       // textViewStatus.setText(statusStr);


    }


}
