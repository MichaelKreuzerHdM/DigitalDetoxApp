package eu.faircode.netguard;

import android.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.Image;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.gms.appindexing.AppIndexApi;

import java.util.Timer;
import java.util.TimerTask;

//MK: This is the home screen's class, hosting the buttons to call all the other Activites
public class HomeActivity extends AppCompatActivity {

    private Button setWorkPlaceBtn;
    private Button statisticsbtn;
    private Button blockedAppsBtn;
    private Button exitBtn;
    private Button blockedContactsBtn;

    private ImageView imageView2;
    //private ImageView background_imageView;

    //Variables to handle timer
    private Timer myTimer;

    //Access shared preferences in order to retrieve work mode
    public static final String MyPREFERENCES = "MyPrefs" ;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Define buttons
        setWorkPlaceBtn = (Button) findViewById(R.id.set_workplace_btn);
        statisticsbtn = (Button) findViewById(R.id.statistics_btn);
        blockedAppsBtn = (Button) findViewById(R.id.blocked_apps_btn);
        blockedContactsBtn = (Button) findViewById(R.id.blocked_callers_btn);
        exitBtn = (Button) findViewById(R.id.exit_btn);

//        background_imageView = (ImageView) findViewById(R.id.background_imageView);

        //Show 'set your workplace' layout on button click
        setWorkPlaceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Stop self repeating timer task on pressed back key
                myTimer.cancel();

                Intent myIntent = new Intent(HomeActivity.this, MapsActivity.class);
                HomeActivity.this.startActivity(myIntent);
            }
        });

        //Start statistics layout
        statisticsbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myTimer.cancel();

                Intent myIntent = new Intent(HomeActivity.this, StatisticsActivity.class);
                HomeActivity.this.startActivity(myIntent);
            }
        });

        //Show list of blocked contacts
        blockedContactsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Stop self repeating timer task on pressed back key
                myTimer.cancel();

                Intent myIntent = new Intent(HomeActivity.this, BlockedCallersActivity.class);
                HomeActivity.this.startActivity(myIntent);
            }
        });

        //Show app blocking rules layout on button click
        blockedAppsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Stop self repeating timer task on pressed back key
                myTimer.cancel();

                Intent myIntent = new Intent(HomeActivity.this, ActivityMain.class);
                myIntent.putExtra("RULES_SCREEN", true);
                HomeActivity.this.startActivity(myIntent);
            }

        });

        //Exit application on button click
        exitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Exit application
                //finishAndRemoveTask();

                //Minimize app
                finishAffinity();
            }
        });

        timerHandling();
    }

    private void timerHandling(){
        myTimer = new Timer();
        myTimer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run () {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        checkModeForPicture();
                    }
                });
            };


        }, 0, 5000);
    }

    private void checkModeForPicture() {
        //Query current mode ("work" or "leisure"?)
        SharedPreferences prefsDDA = this.getApplicationContext().getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = prefsDDA.edit();
        boolean workMode = prefsDDA.getBoolean("workMode", true);
        System.out.println("WORK MODE: " + workMode);

        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= android.os.Build.VERSION_CODES.LOLLIPOP){
            // lollipop and above versions
            imageView2 = (ImageView) findViewById(R.id.imageView2);

            if (workMode) {
                imageView2.setImageDrawable(getDrawable(R.drawable.activitycircleworkexport));
            }else {
                imageView2.setImageDrawable(getDrawable(R.drawable.activitycircleleisureexport));
            }
        } else{
            // Before lollipop
            //TODO: To implement for KitKat
        }

    }

    //Stop self repeating timer task on pressed back key
    public void onBackPressed() {
        super.onBackPressed();
        myTimer.cancel();
        return;
    }

    @Override
    protected void onResume() {
        super.onResume();
        timerHandling();
    }
}
