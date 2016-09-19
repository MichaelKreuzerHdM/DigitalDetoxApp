package eu.faircode.netguard;

import android.Manifest;
import android.app.Activity;
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static int currentRadius = 5;
    private String mode = "Leisure";

    //PAS-Variables to switch mode only after a certain time
    private int ticks = 0;
    private int leisureCnt = 0;

    //Handler for background UI task and Runnable
    ProximityIntentReceiver PIR;

    //Global variable for name of Shared Preferences
    public static final String MyPREFERENCES = "MyPrefs";

    //Show circles at current position indicating work or leisure mode on map
    boolean showPosCircles = true;

    private static final String PROX_ALERT_INTENT = "com.example.micha.googlemapdemoapp.ProximityIntentReceiver";

    //Callback for permission request
    int MY_PERMISSIONS_FINE_LOCATION, MY_PERMISSIONS_REQUEST_COARSE_LOCATION, MY_PERMISSIONS_REQUEST_INTERNET;

    //Rules table holds app rules and gets generated automatically
    private String[][] rulesTable;
    private int rulesTableCounter=0;
    public String[][] categoryList;
    int answerCount=0;

    //Register broadcast receiver in order to be able to stop and pause it later
    android.content.Intent reg;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //Check for permissions
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) ==
                        PackageManager.PERMISSION_GRANTED) {
        }else{
            //TO IMPLEMENT:
            // Exit app, because permissions are not granted by user
            //Ask user for fine location, coarse location and internet permission
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_FINE_LOCATION);
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_COARSE_LOCATION);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, MY_PERMISSIONS_REQUEST_INTERNET);
        }

        //Trick to be able to add control elements to layout
        LinearLayout linear = (LinearLayout) findViewById(R.id.linear);
        super.onCreate(savedInstanceState);

        //Set layout for this activity
        setContentView(R.layout.activity_maps);

        //Get buttons in order to be able to access them later on
        saveWorkLocation = (Button) findViewById(R.id.btn_work_location);
        homeButton = (Button) findViewById(R.id.btn_home_location);

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
                currentRadius = i*10;
                if (currentRadius <= 0) {
                    currentRadius = 10;
                }

                Criteria criteria = new Criteria();
                criteria.setPowerRequirement(Criteria.POWER_HIGH);
                criteria.setAccuracy(Criteria.ACCURACY_FINE);
                provider = locationManager.getBestProvider(criteria, false);
                Location location = locationManager.getLastKnownLocation(provider);

                LatLng tempLoc = new LatLng(location.getLatitude(), location.getLongitude());

                Location pointLocation = retrievelocationFromPreferences();
                mMap.clear();
                mMap.addCircle(new CircleOptions()
                        .center(new LatLng(location.getLatitude(), location.getLongitude()))
                        .radius((float)currentRadius)
                        .strokeColor(Color.RED)
                        .fillColor(0xAAD4F2));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //mMap.clear();
                Toast.makeText(MapsActivity.this, "Radius for work location set to " + String.valueOf(currentRadius) + "m.", Toast.LENGTH_SHORT).show();
            }
        });

        //Get list of all installed apps
        final PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA);

        //Build the rules table for all installed apps
        //(that can be categorized by Google Play Store)
        int x = apps.size();
        int y = 3; //packagename, sub category name, work

        rulesTable = new String [x][y];
        //Retrieve app rules (work / leisure)
        String listOfAllApps = "";

        //Tell user that analyzing / categorisation of apps is starting
        Toast.makeText(MapsActivity.this, "Analyzing your apps...", Toast.LENGTH_LONG).show();

        //Deactivate button until apps are analyzed
        homeButton.setText("Please wait...");
        homeButton.setEnabled(false);

        //Call URL in Google Play Store to retrieve the app's categories
        for (int i = 0; i <= apps.size() - 1; i++) {
            listOfAllApps += String.valueOf(apps.get(i).activityInfo.applicationInfo.packageName + "\n");

            //Call every URL in a single thread, so the main thread won't be blocked simultaneously
            String.valueOf(new GetCategory().execute(String.valueOf(apps.get(i).activityInfo.applicationInfo.packageName)));
        }
    }

    //Restore Activity after being destroyed
    //https://developer.android.com/training/basics/activity-lifecycle/recreating.html

    //When no lastknown location can be found, check for current location
    private void startListening() {
        //To implement
    }

    private void saveProximityAlertPoint() {
        Criteria criteria = new Criteria();
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        provider = locationManager.getBestProvider(criteria, true);

        Location location = locationManager.getLastKnownLocation(provider);

        if (location == null) {
            Toast.makeText(this, "No last known location. Aborting...", Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, "Your work location has been saved!", Toast.LENGTH_SHORT).show();
        saveCoordinates((float) location.getLatitude(), (float) location.getLongitude());
        addProximityAlert(location.getLatitude(), location.getLongitude());
        Circle circle = mMap.addCircle(new CircleOptions()
                .center(new LatLng(location.getLatitude(), location.getLongitude()))
                .radius((float) currentRadius)
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

        //Set marker in order to visualize position of current proximity alert
        LatLng currentPos2 = new LatLng(latitude, longitude);
        mMap.addMarker(new MarkerOptions().position(currentPos2).title("Work Position: " + latitude + " - " + longitude));

        IntentFilter filter = new IntentFilter(PROX_ALERT_INTENT);
        PIR = new ProximityIntentReceiver();
        registerReceiver(PIR, filter);


        //Retrieve location in an interval of five seconds.
        //Interval is currently fix, should be made dynamic in future versions
        new Timer(true).scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                Criteria criteria = new Criteria();
                criteria.setPowerRequirement(Criteria.POWER_HIGH);
                criteria.setAccuracy(Criteria.ACCURACY_FINE);
                provider = locationManager.getBestProvider(criteria, false);
                Location location = locationManager.getLastKnownLocation(provider);

                LatLng tempLoc = new LatLng(location.getLatitude(), location.getLongitude());

                Location pointLocation = retrievelocationFromPreferences();
                final float distance = location.distanceTo(pointLocation);

                final DecimalFormat df = new DecimalFormat("0.00");
                final float tempLat = (float) location.getLatitude();
                final float tempLng = (float) location.getLongitude();
                final Context tempContext = getApplicationContext();

                SharedPreferences prefsDDA = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
                SharedPreferences.Editor prefsEditor = prefsDDA.edit();

                //Check what the prevailing mode is.
                //Do nothing if current mode == prevailing mode:
                //increment ticks if current mode and prevailing mode differ
                boolean prevailingMode = prefsDDA.getBoolean("workMode", true);

                //Find out what current mode is (is user in radius around work location?)
                if (distance < currentRadius) {
                    mode = "Work";
                    prefsEditor.putBoolean("workMode", true);

                } else {
                    mode = "Leisure";
                    prefsEditor.putBoolean("workMode", false);
                }
                prefsEditor.commit();

                //Only call broadcast when mode has changed
                if (prevailingMode != prefsDDA.getBoolean("workMode", true)) {
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
            }

            int i = setUpMap();
        }, 0, 5000);
    }

    public void updateResults(String textStr, String distanceStr, float tempLat, float tempLng, Context context) {
        Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
        //Get time to be visually attached to locations (only for debugging purposes)
        int seconds = c.get(Calendar.SECOND);
        int minutes = c.get(Calendar.MINUTE);
        int hours = c.get(Calendar.HOUR);

        //Draw circles to show current position of user.
        //Color indicates the current mode (leisure or work)
        if (showPosCircles) {
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
        Location location = locationManager.getLastKnownLocation(provider);

        if (location != null) {
            String tempLAT = "";
            String tempLON = "";

            tempLAT = String.valueOf(location.getLatitude());
            tempLON = String.valueOf(location.getLongitude());

            Toast.makeText(this, provider + " - Latitude: " + tempLAT + " - Longitude: " + tempLON, Toast.LENGTH_SHORT).show();
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
            Toast.makeText(MapsActivity.this, "Distance from Point:" + distance, Toast.LENGTH_LONG).show();
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

        //Enable Google Maps to retrieve my current location
        mMap.setMyLocationEnabled(true);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);


        Criteria criteria = new Criteria();
        criteria.setPowerRequirement(Criteria.POWER_HIGH);
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        provider = locationManager.getBestProvider(criteria, false);
        Location location = locationManager.getLastKnownLocation(provider);

        if (location != null) {
            LatLng currentPos2 = new LatLng(location.getLatitude(), location.getLongitude());
        }

        //Set onClick events for buttons
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

    //Method to provide alert box function at runtime
    //Especially helpful when called by thread
    public void callAlertBox(String msgToDisplay) {
        AlertDialog alertDialog = new AlertDialog.Builder(MapsActivity.this).create();
        alertDialog.setTitle("List of installed apps");
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
                .center(new LatLng(location.getLatitude(), location.getLongitude()))
                .radius((float) POINT_RADIUS)
                .strokeColor(Color.BLACK)
                .fillColor(Color.BLUE));

        return 0;
    }

    public static class FragmentForConfigChange extends Fragment {
        interface TaskCallbacks {
            void onPreExecute();

            void onProgressUpdate(int percent);

            void onCancelled();

            void onPostExecute();
        }


        private TaskCallbacks mCallbacks;

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        public void onAttach(Activity activity) {
            super.onAttach(activity);
            mCallbacks = (TaskCallbacks) activity;
        }

        public void FragmentForConfigChange(Context context) {
            return;
        }

        @Override
        public void setRetainInstance(boolean retain) {
            setRetainInstance(true);
            return;
        }

    }

    //Create handler to display messages from async task
    Handler myHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    Toast.makeText(MapsActivity.this, "Your apps have been successfully analyzed!", Toast.LENGTH_LONG).show();
                    homeButton.setText("HOME");
                    homeButton.setEnabled(true);
                    break;
                case 1:
                    homeButton.setText("Analyzing app " + answerCount + "/" + rulesTable.length);
                    break;
                default:
                    break;
            }
        }
    };

    //Get category of a certain app by querying Google Play Store
    public class GetCategory extends AsyncTask<String , Void ,String> {
        String server_response;
        public String [][] categoryList;

        @Override
        protected String doInBackground(String... strings) {

            URL url;
            HttpURLConnection urlConnection = null;

            try {
                System.out.println("SUBMITTED APP NAME: " + strings[0]);

                //Retrieve app's web page from Google Play Store (in english)
                url = new URL("https://play.google.com/store/apps/details?id=" + strings[0] + "&hl=en");
                System.out.println("URL: " +url);
                urlConnection = (HttpURLConnection) url.openConnection();

                int responseCode = urlConnection.getResponseCode();
                answerCount++;
                if(responseCode == HttpURLConnection.HTTP_OK){
                    server_response = readStream(urlConnection.getInputStream(), strings[0]);
                    if(strings[0].equals("com.whatsapp")){
                        System.out.println(strings[0]);
                        System.out.println("server_response: " + server_response);
                    }
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                //No network connection etc.: Tell user to switch on network and to turn off firewall
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            System.out.println("rulesTableCounter" + rulesTableCounter);
            System.out.println("rulesTable.length" + rulesTable.length);

            //All apps have been categorized
            if(answerCount>=rulesTable.length){
                myHandler.sendEmptyMessage(0);
                System.out.println("GETTING CATEGORIES FINISHED");
            }else{
                myHandler.sendEmptyMessage(1);
            }

            Log.e("Response", "" + server_response);
        }
    }

    // Converting InputStream to String
    private String readStream(InputStream in, String packageName) {
        BufferedReader reader = null;
        StringBuffer response = new StringBuffer();
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = reader.readLine()) != null) {

                //Find line containing "category"
                if(line.contains("category")) {
                    //Cut out the specific categories
                    Pattern pattern = Pattern.compile("<a class=\"document-subtitle category\" href=\"/store/apps/category/(.*?)\">");
                    Matcher matcher = pattern.matcher(line);

                    while (matcher.find()) {
                        //Erase clutter around category
                        String temp=matcher.group(0);
                        line = matcher.group(0).replace("<a class=\"document-subtitle category\" href=\"/store/apps/category/", "");
                        line = line.replace("\">", "");
                        System.out.println("CATEGORY DETECTED FOR :" + packageName + " CATEGORY IS: " + line);

                        //Write the app's category to an array
                        //Data structure: packagename, sub category name, work
                        rulesTable[rulesTableCounter][0] = packageName;
                        rulesTable[rulesTableCounter][1] = line;

                        //Get category from sub category (e.g. GAME_ROLE_PLAYING --> GAMES)
                        String permissionMode= getPermissionMode(rulesTable[rulesTableCounter][1]);
                        System.out.println(permissionMode);

                        SharedPreferences prefsDDA = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
                        SharedPreferences.Editor prefsEditor = prefsDDA.edit();

                        prefsEditor.putString(packageName, permissionMode);
                        prefsEditor.commit();

                        rulesTableCounter++;
                    }
                }
                response.insert(0,line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return response.toString();
    }

    //List of all categories downloaded from Google Play Store and parametrized manually
    private String getPermissionMode(String category){
        String[][] categoryList = {
                {"Android Wear", "leisure"},
                {"Books & Reference", "leisure"},
                {"Business", "work"},
                {"Comics", "leisure"},
                {"Communication", "leisure"},
                {"Education", "leisure"},
                {"Entertainment", "leisure"},
                {"Finance", "leisure"},
                {"Health & Fitness", "leisure"},
                {"Libraries & Demo", "work"},
                {"Lifestyle", "leisure"},
                {"Media & Video", "leisure"},
                {"Medical", "leisure"},
                {"Music & Audio", "leisure"},
                {"News & Magazines", "leisure"},
                {"Personalization", "work"},
                {"Photography", "leisure"},
                {"Productivity", "work"},
                {"Shopping", "leisure"},
                {"Social", "leisure"},
                {"Sports", "leisure"},
                {"Tools", "work"},
                {"Transportation", "work"},
                {"Travel & Local", "work"},
                {"Weather", "leisure"},
                {"Games", "leisure"},
                {"Action", "leisure"},
                {"Adventure", "leisure"},
                {"Arcade", "leisure"},
                {"Board", "leisure"},
                {"Card", "leisure"},
                {"Casino", "leisure"},
                {"Casual", "leisure"},
                {"Educational", "leisure"},
                {"Music", "leisure"},
                {"Puzzle", "leisure"},
                {"Racing", "leisure"},
                {"Role Playing", "leisure"},
                {"Simulation", "leisure"},
                {"Sports", "leisure"},
                {"Strategy", "leisure"},
                {"Trivia", "leisure"},
                {"Word", "leisure"},
                {"Family", "leisure"},
                {"Ages 5 & Under", "leisure"},
                {"Ages 6-8", "leisure"},
                {"Ages 9 & Up", "leisure"},
                {"Popular Characters", "leisure"},
                {"Action & Adventure", "leisure"},
                {"Brain Games", "leisure"},
                {"Creativity", "leisure"},
                {"Education", "leisure"},
                {"Music & Video", "leisure"},
                {"Pretend Play", "leisure"}
        };

        System.out.println("category: " + category);

        for(int i=0;i<categoryList.length;i++) {
            if (categoryList[i][0].toUpperCase().equals(category)){
                System.out.println("PERMISSION IS: " + categoryList[i][1]);
                return categoryList[i][1];
            }
        }
        return "";
    }
}
