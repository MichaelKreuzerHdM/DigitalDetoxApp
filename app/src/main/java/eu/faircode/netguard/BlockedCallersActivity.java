package eu.faircode.netguard;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

//MK: Class to filter and retrieve data from address book
//In the Digital Detox App, the app filters contacts depending on if the current mode is "work" or "leisure".
//It automatically finds out, if a contact is work-related
public class BlockedCallersActivity extends AppCompatActivity {
    private ListView list;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> arrayList;

    int MY_PERMISSIONS_REQUEST_CONTACTS;

    //Variables to handle timer
    Timer myTimer;

    //Access shared preferences in order to retrieve work mode
    public static final String MyPREFERENCES = "MyPrefs" ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked_callers);

        //Set title of Action Bar
        final ActionBar myActionBar = getSupportActionBar();
        if (myActionBar != null) {
            myActionBar.setTitle("BLOCKED CALLERS");
        }

        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_CONTACTS}, MY_PERMISSIONS_REQUEST_CONTACTS);

        //Check for permissions
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED) {
            System.out.println("Permission granted");

            //Call method every five seconds (to check if work or leisure mode)
            myTimer = new Timer();
            myTimer.scheduleAtFixedRate(new TimerTask() {

                @Override
                public void run () {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Get contacts from address book
                            fetchContacts();
                        }
                    });
                };


            }, 0, 5000);
        }else{
            // Exit app, because permissions are not granted by user
            System.out.println("Permission denied");

        }
    }

    //Stop self repeating timer task on pressed back key
    public void onBackPressed() {
        super.onBackPressed();
        myTimer.cancel();
        return;
    }

    public void fetchContacts() {
        ContentResolver cr = this.getContentResolver();
        //Get list that will show our data in the layout
        list = (ListView) findViewById(R.id.lvContacts);

        //Create new list to input and buffer contact list data
        arrayList = new ArrayList<String>();

        //Create adapter for interaction between data structure (array list) and layout element (ListView)
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, arrayList);

        //Link data structure (array list) and layout element (ListView) to each other
        list.setAdapter(adapter);

        //Get data from address book (phone and email have to be accessed separately)
        Cursor contacts = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        Cursor contactsMail = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, null, null, null);

        //Set cursors back to start
        contacts.moveToFirst();
        contactsMail.moveToFirst();

        String name = "", email = "";

        //Query current mode ("work" or "leisure"?)
        SharedPreferences prefsDDA = this.getApplicationContext().getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = prefsDDA.edit();
        boolean workMode = prefsDDA.getBoolean("workMode", true);
        System.out.println("WORK MODE: " + workMode);

        //Step over all entries of the address book one after another
        for (int i = 0; i < contacts.getCount(); i++) {

            //Retrieve name and email address from address book
            name = contacts.getString(contacts.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            email = contactsMail.getString(contactsMail.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
            contacts.moveToNext();
            contactsMail.moveToNext();

            //1. Check if current mode is work mode
            //2. In this case: select all the contacts that contain a "business" or similar domain (@daimler.com, @hdm-stuttgart.de)
            //3. Then: Block all of these contacts while in work mode
            if(!workMode) {
                list.setSelection(1);
                if(email.contains("@hdm-stuttgart.de")) {
                    //Write user data to array list that will be used in ListView
                    arrayList.add("NAME: " + name + " - BLOCKED - \nEMAIL: " + email);
                }else{
                    arrayList.add("NAME: " + name + " - NOT BLOCKED - \nEMAIL: " + email);
                }
            }else{
                arrayList.add("NAME: " + name + " - NOT BLOCKED - \nEMAIL: " + email);
            }

            // Notify the adapter that values have changed. ListView then updates its user interface.
            adapter.notifyDataSetChanged();
        }

        contacts.close();
    }


}
