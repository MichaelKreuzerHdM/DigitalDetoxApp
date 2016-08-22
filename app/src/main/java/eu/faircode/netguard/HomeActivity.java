package eu.faircode.netguard;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class HomeActivity extends AppCompatActivity {

    private Button setWorkPlaceBtn;
    private Button statisticsbtn;
    private Button blockedAppsBtn;
    private Button exitBtn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setWorkPlaceBtn = (Button) findViewById(R.id.set_workplace_btn);
        statisticsbtn = (Button) findViewById(R.id.statistics_btn);
        blockedAppsBtn = (Button) findViewById(R.id.blocked_apps_btn);
        exitBtn = (Button) findViewById(R.id.exit_btn);

        //Show 'set your workplace' layout on button click
        setWorkPlaceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(HomeActivity.this, MapsActivity.class);
                HomeActivity.this.startActivity(myIntent);
            }
        });

        //Start statistics layout on button click
        statisticsbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //To implement: Start statistics layout
            }
        });

        //Show app blocking rules layout on button click
        blockedAppsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // setContentView(R.layout.main);

                Intent myIntent = new Intent(HomeActivity.this, ActivityMain.class);
                myIntent.putExtra("RULES_SCREEN", true);
                HomeActivity.this.startActivity(myIntent);
            }

        });

        //Exit application on button click
        exitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //To implement: Exit application
            }
        });
    }
}
