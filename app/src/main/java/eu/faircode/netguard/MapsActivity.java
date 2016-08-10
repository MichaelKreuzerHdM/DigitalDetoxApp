package eu.faircode.netguard;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

//Linear layout in order to be able to add control elements to layout
//Intents for proximity alert

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LocationManager locationManager;
    private String provider;
    private Location location;

    private Button saveWorkLocation;
    private SeekBar radiusZoom;

    private static Context globalContext;

    //Parameters for proximity alert to work location
    private static final long MINIMUM_DISTANCECHANGE_FOR_UPDATE = 3; // in Meters
    private static final long MINIMUM_TIME_BETWEEN_UPDATE = 3000; // in Milliseconds

    private static final long POINT_RADIUS = 3; // in Meters
    private static final long PROX_ALERT_EXPIRATION = -1;

    //home location button and coordinates
    private Button saveHomeLocation;
    private static final String POINT_LATITUDE_KEY = "POINT_LATITUDE_KEY";
    private static final String POINT_LONGITUDE_KEY = "POINT_LONGITUDE_KEY";

    private final Handler handler = new Handler();

    private static int currentRadius=5;

    private TextView textViewStatus;


    //private static final String PROX_ALERT_INTENT = "com.example.micha.googlemapdemoapp.ProximityIntentReceiver";
    private static final String PROX_ALERT_INTENT = "com.example.micha.googlemapdemoapp.ProximityIntentReceiver";
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Trick to be able to add control elements to layout
        LinearLayout linear = (LinearLayout) findViewById(R.id.linear);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //add buttons dynamically
        /*
        Button myButton = new Button(MapsActivity.this);
        myButton.setId(123);
        myButton.setText("Push Me");

        LinearLayout ll = (LinearLayout)findViewById(R.id.linear);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ll.addView(myButton, lp);
        */

        //HIER WEITERMACHEN
        //switchPref = (SwitchPreference)findViewById(R.layout.);
        /*locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MINIMUM_TIME_BETWEEN_UPDATE,
                MINIMUM_DISTANCECHANGE_FOR_UPDATE,
                (LocationListener) MyLocationListener()
        );*/

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        radiusZoom = (SeekBar) findViewById(R.id.radiusZoom);
        radiusZoom.setProgress(0);
        radiusZoom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                //Toast.makeText(globalContext, "Left is: " + seekBar.getLeft() + ". Max is: " + seekBar.getMax(), Toast.LENGTH_SHORT).show();
                currentRadius=i;
                if(currentRadius<=0){
                    currentRadius=1;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(MapsActivity.this, "Radius for work location set to " + String.valueOf(currentRadius) + "m." , Toast.LENGTH_SHORT).show();
                //callAlertBox(String.valueOf(currentRadius));
            }
        });


        //Get list of all installed apps
        final PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA);

        //Start intent in order to see check rules
        //Intent appRulesIntent = new Intent(this, SettingsActivity2.class);
        //MapsActivity.this.startActivity(appRulesIntent);

        String listOfAllApps="";
        for(int i = 0; i<=apps.size()-1; i++){
            //Filter out apps that are not allowed to be running

            //--> is app in AppRules table
            listOfAllApps+= String.valueOf(apps.get(i).activityInfo.applicationInfo.packageName + "\n");
        }
        String packageName=apps.get(0).activityInfo.applicationInfo.packageName;
        ActivityManager am = (ActivityManager)getSystemService(Activity.ACTIVITY_SERVICE);
        am.killBackgroundProcesses(packageName);

        //Save running app to shared preferences in order to later show it in SettingsActivity
        //Button buttonRemove = (Button)addView.findViewById(R.id.remove);

        //callAlertBox(listOfAllApps);

        am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfo = am.getRunningAppProcesses();

        listOfAllApps="";
        for(int i = 0; i<=runningAppProcessInfo.size()-1; i++){
            //Filter out apps that are not allowed to be running

            //--> is app in AppRules table
            listOfAllApps+= String.valueOf(String.valueOf(runningAppProcessInfo.get(i).processName) + "\n");
        }

        //callAlertBox(String.valueOf(runningAppProcessInfo.get(0)));
        callAlertBox("List of running apps: \n" + listOfAllApps);



        //android.os.Process.killProcess(android.os.Process.myPid());

        /*
        try {
            Process process = Runtime.getRuntime().exec("ps");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            InputStream is = process.getInputStream();
            //os.writeBytes("kill " + 1000 + "\n");
            int readed = 0;
            byte[] buff = new byte[4096];
            os.writeBytes("exit\n");
            os.flush();
            process.destroy();

        } catch (IOException e) {
            e.printStackTrace();
        }
        */
    }

    private void saveProximityAlertPoint() {

        Criteria criteria = new Criteria();
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        provider = locationManager.getBestProvider(criteria, true);
        Location location = locationManager.getLastKnownLocation(provider);
        //Toast.makeText(this, "Providers are: " +  String.valueOf(locationManager.getAllProviders()), Toast.LENGTH_SHORT).show();

        if (location==null) {
            Toast.makeText(this, "No last known location. Aborting...", Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, "Proximity alert saved", Toast.LENGTH_SHORT).show();
        saveCoordinates((float) location.getLatitude(), (float) location.getLongitude());
        addProximityAlert(location.getLatitude(),  location.getLongitude());
        Circle circle = mMap.addCircle(new CircleOptions()
                .center(new LatLng(location.getLatitude(), location.getLongitude()))
                .radius((float)currentRadius)
                .strokeColor(Color.BLACK)
                .fillColor(0x5500ff00));
    }

    private void addProximityAlert(double latitude, double longitude) {
        Intent intent = new Intent(PROX_ALERT_INTENT);
        PendingIntent proximityIntent = PendingIntent.getBroadcast(this, 0, intent, 0);

        locationManager.removeUpdates(proximityIntent);

        Criteria criteria = new Criteria();
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        provider = locationManager.getBestProvider(criteria, true);

        locationManager.requestLocationUpdates(
                provider,
                MINIMUM_TIME_BETWEEN_UPDATE,
                MINIMUM_DISTANCECHANGE_FOR_UPDATE,
                proximityIntent
        );

        provider = locationManager.getBestProvider(criteria, false);
        Location location = locationManager.getLastKnownLocation(provider);

        locationManager.addProximityAlert(
                location.getLatitude(), // the latitude of the central point of the alert region
                location.getLongitude(), // the longitude of the central point of the alert region
                currentRadius, // the radius of the central point of the alert region, in meters
                PROX_ALERT_EXPIRATION, // time for this proximity alert, in milliseconds, or -1 to indicate no expiration
                proximityIntent // will be used to generate an Intent to fire when entry to or exit from the alert region is detected
        );

        //Remove all markers
        //mMap.clear();

        //Set marker in order to visualize position of current proximity alert
        LatLng currentPos2 = new LatLng(latitude, longitude);
        mMap.addMarker(new MarkerOptions().position(currentPos2).title("Work Position: " + latitude + " - " + longitude));

        IntentFilter filter = new IntentFilter(PROX_ALERT_INTENT);
        registerReceiver(new ProximityIntentReceiver(), filter);


        new Timer().scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run(){
                Criteria criteria = new Criteria();
                criteria.setPowerRequirement(Criteria.POWER_HIGH);
                criteria.setAccuracy(Criteria.ACCURACY_FINE);
                provider = locationManager.getBestProvider(criteria, false);
                Location location = locationManager.getLastKnownLocation(provider);

                LatLng tempLoc = new LatLng(location.getLatitude(), location.getLongitude());

                Location pointLocation = retrievelocationFromPreferences();
                final float distance = location.distanceTo(pointLocation);

                /*System.out.println("1 - Latitude: " + location.getLatitude() + ". Longitude: " + location.getLongitude());
                System.out.println("2 - Latitude: " + pointLocation.getLatitude() + ". Longitude: " + pointLocation.getLongitude());

                System.out.println("Distance: " + String.valueOf(distance));
                */

                final String mode;
                final DecimalFormat df = new DecimalFormat("0.00");
                final float tempLat=(float)location.getLatitude();
                final float tempLng=(float)location.getLongitude();
                final Context tempContext = getApplicationContext();

                if(distance<currentRadius){
                    mode="Work";
                }else{
                    mode="Leisure";
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateResults(mode, String.valueOf(df.format(distance)) + "m", tempLat, tempLng, tempContext);

                    }
                });

                //System.out.println("Distanz: " + location.getLatitude() + ". Longitude: " + location.getLongitude());

                /*
                Circle circle = mMap.addCircle(new CircleOptions()
                        .center(new LatLng(location.getLatitude(), location.getLongitude()))
                        .radius((float)POINT_RADIUS)
                        .strokeColor(Color.BLACK)
                        .fillColor(0xAAD4F2));
                        */
            }
            int i = setUpMap();
        },0,5000);
        TextView textViewStatus = (TextView) findViewById(R.id.textViewStatus);
        //textViewStatus.setText("WORK MODE");
    }

    public void updateResults(String textStr, String distanceStr, float tempLat, float tempLng, Context context) {
        Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
        int seconds = c.get(Calendar.SECOND);
        int minutes = c.get(Calendar.MINUTE);
        int hours = c.get(Calendar.HOUR);
        if(textStr=="Leisure") {
            Circle circle = mMap.addCircle(new CircleOptions()
                    .center(new LatLng(tempLat, tempLng))
                    .radius((float) 1)
                    .strokeColor(Color.BLACK)
                    .fillColor(0x550085FF));
        }else{
            Circle circle = mMap.addCircle(new CircleOptions()
                    .center(new LatLng(tempLat, tempLng))
                    .radius((float) 1)
                    .strokeColor(Color.BLACK)
                    .fillColor(Color.RED));
            System.out.println("2 - Latitude: " + tempLat + ". Longitude: " + tempLng + ". Time: " + String.valueOf(hours) + ":" + String.valueOf(minutes) + ":" + String.valueOf(seconds));
            System.out.println("Distance: " + distanceStr);
        }
        /*
        NotificationManager notificationManager = (NotificationManager) globalContext.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = createNotification();

        int NOTIFICATION_ID = 1000;
        notificationManager.notify(NOTIFICATION_ID, notification);

        */
        TextView textViewStatus = (TextView) findViewById(R.id.textViewStatus);
        textViewStatus.setText(textStr + " " + distanceStr);

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

    private void populateCoordinatesFromLastKnownLocation(boolean work) {
        Criteria criteria = new Criteria();
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        provider = locationManager.getBestProvider(criteria, false);
        Location location = locationManager.getLastKnownLocation(provider);

        if (location!=null) {
            String tempLAT = "";
            String tempLON = "";

            tempLAT = String.valueOf(location.getLatitude());
            tempLON = String.valueOf(location.getLongitude());

            Toast.makeText(this, "NETWORK --- Latitude: " + tempLAT + " - Longitude: " + tempLON, Toast.LENGTH_SHORT).show();
        }
    }

    private void saveCoordinatesInPreferences(float latitude, float longitude) {
        SharedPreferences prefs =
                this.getSharedPreferences(getClass().getSimpleName(),
                        Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putFloat(POINT_LATITUDE_KEY, latitude);
        prefsEditor.putFloat(POINT_LONGITUDE_KEY, longitude);
        prefsEditor.commit();
    }

    public Location retrievelocationFromPreferences() {
        SharedPreferences prefs = this.getSharedPreferences(getClass().getSimpleName(), Context.MODE_PRIVATE);
        Location location = new Location("POINT_LOCATION");
        location.setLatitude(prefs.getFloat(POINT_LATITUDE_KEY, 0.0f));
        location.setLongitude(prefs.getFloat(POINT_LONGITUDE_KEY, 0.0f));

        return location;
    }

    public class MyLocationListener implements LocationListener {
        public void onLocationChanged(Location location) {
            Location pointLocation = retrievelocationFromPreferences();
            float distance = location.distanceTo(pointLocation);
            Toast.makeText(MapsActivity.this, "Distance from Point:"+distance, Toast.LENGTH_LONG).show();
        }
        public void onStatusChanged(String s, int i, Bundle b) {
        }
        public void onProviderDisabled(String s) {
        }
        public void onProviderEnabled(String s) {
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        /*
        // Dummy markers
        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Home Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

        LatLng currentPos = new LatLng(-33, 150);
        mMap.addMarker(new MarkerOptions().position(currentPos).title("Work Location"));

        */

        //Enable location
        mMap.setMyLocationEnabled(true);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        provider = locationManager.getBestProvider(criteria, false);
        Location location = locationManager.getLastKnownLocation(provider);

        if(location!=null) {
            LatLng currentPos2 = new LatLng(location.getLatitude(), location.getLongitude());
            /*mMap.addMarker(new MarkerOptions().position(currentPos2).title("NEW Location: " + location.getLatitude() + " - " + location.getLongitude()));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentPos2));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(18));
            */
        }

        //Get buttons
        saveWorkLocation = (Button) findViewById(R.id.btn_work_location);
        saveHomeLocation = (Button) findViewById(R.id.btn_home_location);



        saveWorkLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //populate coordinates for work location
                populateCoordinatesFromLastKnownLocation(true);
                saveProximityAlertPoint();
            }
        });
    }

    private void saveCoordinates(float latitude, float longitude) {
        SharedPreferences prefs = this.getSharedPreferences(getClass().getSimpleName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putFloat(String.valueOf(POINT_LATITUDE_KEY), latitude);
        prefsEditor.putFloat(String.valueOf(POINT_LONGITUDE_KEY), longitude);

        prefsEditor.putFloat(String.valueOf(POINT_LATITUDE_KEY), latitude);
        prefsEditor.putFloat(String.valueOf(POINT_LONGITUDE_KEY), longitude);

        prefsEditor.commit();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();

        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Maps Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://eu.faircode.netguard/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Maps Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://eu.faircode.netguard/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    public void callAlertBox(String msgToDisplay) {
        AlertDialog alertDialog = new AlertDialog.Builder(MapsActivity.this).create();
        alertDialog.setTitle("Alert");
        alertDialog.setMessage(msgToDisplay);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    public int setUpMap() {
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, false);
        Location location = locationManager.getLastKnownLocation(provider);

        LatLng tempLoc = new LatLng(location.getLatitude(), location.getLongitude());

        Circle circle = mMap.addCircle(new CircleOptions()
                .center(new LatLng(location.getLatitude(), location.getLongitude()+0.1))
                .radius((float)POINT_RADIUS)
                .strokeColor(Color.BLACK)
                .fillColor(Color.BLUE));

        return 0;
    }


}