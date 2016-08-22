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
import android.support.v4.app.Fragment;
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

    //Variables for fragments to avoid intent leaking
    private static final String TAG_TASK_FRAGMENT = "task_fragment";
    private FragmentForConfigChange mTaskFragment;

    private GoogleMap mMap;
    private LocationManager locationManager;
    private String provider;
    private Location location;

    private static Context globalContext;

    //Parameters for proximity alert to work location
    private static final long MINIMUM_DISTANCECHANGE_FOR_UPDATE = 3; // in Meters
    private static final long MINIMUM_TIME_BETWEEN_UPDATE = 3000; // in Milliseconds

    private static final long POINT_RADIUS = 3; // in Meters
    private static final long PROX_ALERT_EXPIRATION = -1;

    //home location button
    private Button homeButton;
    private Button saveWorkLocation;

    //Editable bar to change radius around work location
    private SeekBar radiusZoom;

    //Text field shows current mode (work or leisure)
    private TextView textViewStatus;

    //Coordinates
    private static final String POINT_LATITUDE_KEY = "POINT_LATITUDE_KEY";
    private static final String POINT_LONGITUDE_KEY = "POINT_LONGITUDE_KEY";

    private static int currentRadius=5;
    private String mode="Leisure";

    //PAS-Variables to switch mode only after a certain time
    private int ticks =0;
    private int leisureCnt=0;


    //Handler for background UI task and Runnable
    ProximityIntentReceiver PIR;

    //Global variable for name of Shared Preferences
    public static final String MyPREFERENCES = "MyPrefs" ;

    //Show circles at current position indicating work or leisure mode on map
    boolean showPosCircles=true;

    private static final String PROX_ALERT_INTENT = "com.example.micha.googlemapdemoapp.ProximityIntentReceiver";
    public static final String ACTION_RULES_CHANGED = "eu.faircode.netguard.ACTION_RULES_CHANGED";

    //Register broadcast receiver in order to be able to stop and pause it later
    android.content.Intent reg;
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

        //switchPref = (SwitchPreference)findViewById(R.layout.);
        /*locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MINIMUM_TIME_BETWEEN_UPDATE,
                MINIMUM_DISTANCECHANGE_FOR_UPDATE,
                (LocationListener) MyLocationListener()
        );*/

        //Avoid leaking of Activity
        /*
        FragmentManager fm = getFragmentManager();
        mTaskFragment = (FragmentForConfigChange) fm.findFragmentByTag(TAG_TASK_FRAGMENT);

        // If the Fragment is non-null, then it is currently being
        // retained across a configuration change.
        if (mTaskFragment == null) {
            mTaskFragment = new FragmentForConfigChange();
            fm.beginTransaction().add(mTaskFragment, TAG_TASK_FRAGMENT).commit();
        }*/

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
            }
        });



        //Get list of all installed apps
        final PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA);

        //Start intent in order to see check rules

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

        //Show list of running apps
       // callAlertBox("List of running apps: \n" + listOfAllApps);



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

    //Restore Activity after being destroyed
    //https://developer.android.com/training/basics/activity-lifecycle/recreating.html

    //When no lastknown location can be found, check for current location
    private void startListening(){
        //To implement
        //String locationProvider = LocationManager.NETWORK_PROVIDER;
        //locationManager.requestLocationUpdates(locationProvider, 0, 0, locationListener);
    }

    private void saveProximityAlertPoint() {

        Criteria criteria = new Criteria();
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        provider = locationManager.getBestProvider(criteria, true);

        //To implement: Check for permission (necessary for lollipop and higher)
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
        PIR = new ProximityIntentReceiver();
        registerReceiver(PIR, filter);



/*
        public static void scheduleTestAlarmReceiver(Context tempContext) {

            Intent receiverIntent = new Intent(tempContext, MapsActivity.class);
            PendingIntent sender = PendingIntent.getBroadcast(tempContext, 123456789, receiverIntent, 0);

            AlarmManager alarmManager = (AlarmManager)tempContext.getSystemService(Context.ALARM_SERVICE);
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()+200, 200, sender);

        }

        scheduleTestAlarmReceiver(globalContext);
        */

        //Retrieve location in an intervall of five seconds.
        //interval is currently fix, but should be made dynamic
        //in the final version of the app.
        new Timer(true).scheduleAtFixedRate(new TimerTask(){

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

                final DecimalFormat df = new DecimalFormat("0.00");
                final float tempLat=(float)location.getLatitude();
                final float tempLng=(float)location.getLongitude();
                final Context tempContext = getApplicationContext();

                SharedPreferences prefsDDA = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
                SharedPreferences.Editor prefsEditor = prefsDDA.edit();

                //Check what the prevailing mode is.
                //Do nothing if current mode == prevailing mode:
                //increment ticks if current mode and prevailing mode differ
                boolean prevailingMode = prefsDDA.getBoolean("workMode",true);

                //HIER WEITERMACHEN
                //Find out what current mode is (is user in radius of work location?)
                if(distance<currentRadius){
                    mode="Work";
                    prefsEditor.putBoolean("workMode", true);

                }else{
                    mode="Leisure";
                    prefsEditor.putBoolean("workMode", false);
                }
                prefsEditor.commit();

                //Only call broadcast when mode has changed
                if(prevailingMode!=prefsDDA.getBoolean("workMode", true)) {
                    System.out.println("MODE CHANGED. SENDING BROADCAST");
                    System.out.println("Prevailing mode: " + prevailingMode);
                    System.out.println("getBoolean" + prefsDDA.getBoolean("workMode", true));

                    //Update rules in NetGuard's list of apps:
                    //Send broadcast to ActivityMain
                    Intent updateRulesIntent = new Intent("MyBroadcast");
                    updateRulesIntent.setAction("MyBroadcast");
                    updateRulesIntent.putExtra("value", 1000);
                    sendBroadcast(updateRulesIntent);

                    Rule.getRules(true, getApplicationContext());
                }
                //Show current mode in user interface.
                //This has to run on a UI thread, because
                // normal threads cannot access UI elements
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateResults(mode, String.valueOf(df.format(distance)) + "m", tempLat, tempLng, tempContext);
                    }
                    });

                //Output for debug and logging
                System.out.println("Distance: " + String.valueOf(df.format(distance)) + "m" + ". LAT: " + location.getLatitude() + ". LNG: " + location.getLongitude());

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

        // The four methods below are called by the TaskFragment when new
        // progress updates or results are available. The MainActivity
        // should respond by updating its UI to indicate the change.

        /*@Override
        public void onPreExecute() {  }

        @Override
        public void onProgressUpdate(int percent) { ... }

        @Override
        public void onCancelled() { ... }

        @Override
        public void onPostExecute() { ... }
    */
    }

    public void updateResults(String textStr, String distanceStr, float tempLat, float tempLng, Context context) {
        Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
        int seconds = c.get(Calendar.SECOND);
        int minutes = c.get(Calendar.MINUTE);
        int hours = c.get(Calendar.HOUR);

        //Draw circles to show current position of user.
        //Color indicates the current mode (leisure or work)
        if(showPosCircles) {
            if (textStr == "Leisure") {
                Circle circle = mMap.addCircle(new CircleOptions()
                        .center(new LatLng(tempLat, tempLng))
                        .radius((float) 1)
                        .strokeColor(Color.BLACK)
                        .fillColor(0x550085FF));
            } else {
                Circle circle = mMap.addCircle(new CircleOptions()
                        .center(new LatLng(tempLat, tempLng))
                        .radius((float) 1)
                        .strokeColor(Color.BLACK)
                        .fillColor(Color.RED));
            }
        }

        TextView textViewStatus = (TextView) findViewById(R.id.textViewStatus);
        textViewStatus.setText(textStr + " Mode " + distanceStr);
    }

    //Create notifications (only needed in first development phase, but kept for possible later usage)
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
        //To implement: Ask for permissions at run time
        Location location = locationManager.getLastKnownLocation(provider);

        if (location!=null) {
            String tempLAT = "";
            String tempLON = "";

            tempLAT = String.valueOf(location.getLatitude());
            tempLON = String.valueOf(location.getLongitude());

            Toast.makeText(this, "NETWORK --- Latitude: " + tempLAT + " - Longitude: " + tempLON, Toast.LENGTH_SHORT).show();
        }
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

        //Enable Google Maps to retrieve my current location
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
        homeButton = (Button) findViewById(R.id.btn_home_location);

        saveWorkLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //populate coordinates for work location
                populateCoordinatesFromLastKnownLocation(true);
                saveProximityAlertPoint();
            }
        });

        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //setContentView(R.layout.home);
                //setContentView(R.layout.home);
                Intent intent = new Intent(MapsActivity.this, HomeActivity.class);

                startActivity(intent);

                //ToDo: Unregister receiver because of error message
                //getActivity().unregisterReceiver(this);
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

    //Method to provide alert box function on runtime
    //Especially helpful when called by thread
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

        Circle circle = mMap.addCircle(new CircleOptions()
                .center(new LatLng(location.getLatitude(), location.getLongitude()+0.1))
                .radius((float)POINT_RADIUS)
                .strokeColor(Color.BLACK)
                .fillColor(Color.BLUE));

        return 0;
    }




    public static class FragmentForConfigChange extends Fragment{
        interface TaskCallbacks {
            void onPreExecute();
            void onProgressUpdate(int percent);
            void onCancelled();
            void onPostExecute();
        }


        private TaskCallbacks mCallbacks;
        //private DummyTask mTask;

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }


        public void onAttach(Activity activity) {
            super.onAttach(activity);
            mCallbacks = (TaskCallbacks) activity;
        }

        public void FragmentForConfigChange(Context context){
            return;
        }


        @Override
        public void setRetainInstance(boolean retain) {
            setRetainInstance(true);
            return;
        }



    }



}
