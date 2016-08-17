package eu.faircode.netguard;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Micha on 13.08.16.
 */
public class DatabaseHandler {
    public SQLiteDatabase workAppRulesList;
    private SQLiteDatabase.CursorFactory factory;
    private Context context;

/*    public DatabaseHandler(Context context){
        this.context=context;
        factory = new SQLiteDatabase.CursorFactory();
    }

    public void createRulesList(){
        SQLiteDatabase workAppRulesList = SQLiteDatabase.openOrCreateDatabase("workAppRulesDaBa", factory ,null);
        workAppRulesList.execSQL("CREATE TABLE IF NOT EXISTS RulesList(currentMode VARCHAR);");
        workAppRulesList.execSQL("INSERT INTO RulesList VALUES('com.android.browser','false');");
    }

    //Todo: Create update rule, add rule etc.

    //Todo: Implement the query for sending in app name and getting back if app is allowed to run
    public boolean readRulesList(String appToCheck){
        workAppRulesList = SQLiteDatabase.openOrCreateDatabase("workAppRulesDaBa",factory,null);
        Cursor resultSet = workAppRulesList.rawQuery("Select * from RulesList",null);
        resultSet.moveToFirst();
        String app = resultSet.getString(1);
        //Todo: substitude 'false' by value of browser
        return false;
    }
    */
}
