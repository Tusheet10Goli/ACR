package com.hisense.evservice;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.util.Calendar;


public class DBManager {
    private static final String TAG = "DBManager";
    private Context context;

    private static DBManager dbManager;

    private DBManager(Context context) {
        this.context = context;
        if (dbManager != null){
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }
    public synchronized static DBManager getInstance(Context context){
        if (dbManager == null){ //if there is no instance available... create new one
            dbManager = new DBManager(context);
        }

        return dbManager;
    }

    public boolean getAppConfig(String pkg){
        String selection = ACRContract.TestEntry.ACR_WHITELIST_COLUMN_NAME[0] + "=\'" + pkg + "\'";
        Cursor cursor = context.getContentResolver().query(ACRContract.TestEntry.ACR_WHITELIST_URI, null, selection, null, null);
        if(cursor.getCount() > 0){
            cursor.moveToFirst();
            if (cursor.getInt(2) == 1){
                return true;
            }
        }
        return false;
    }
    
    public boolean ifInsideWhiteList(String pkg) {
    	String selection = ACRContract.TestEntry.ACR_WHITELIST_COLUMN_NAME[0] + "=\'" + pkg + "\'";
    	Cursor cursor = context.getContentResolver().query(ACRContract.TestEntry.ACR_WHITELIST_URI, null, selection, null, null);
    	if (cursor.getCount() == 0) {
    		//not inside whitelist
    		return false;
    	} else {
    		//inside whitelist
    		return true;
    	}
    }

    public void updateAppConfig(String pkg, String acr){
        int audioCollect = 0;
        ContentValues contentValues = new ContentValues();
        String selection = ACRContract.TestEntry.ACR_WHITELIST_COLUMN_NAME[0] + "=\'" + pkg + "\'";
        Cursor cursor = context.getContentResolver().query(ACRContract.TestEntry.ACR_WHITELIST_URI, null, selection, null, null);
        if(acr.equals("true")){ audioCollect = 1;}
        if(cursor.getCount() < 1){
            contentValues.put(ACRContract.TestEntry._ID, System.currentTimeMillis());
            contentValues.put(ACRContract.TestEntry.ACR_WHITELIST_COLUMN_NAME[0], pkg );
            contentValues.put(ACRContract.TestEntry.ACR_WHITELIST_COLUMN_NAME[1], audioCollect);
            contentValues.put(ACRContract.TestEntry.ACR_WHITELIST_COLUMN_NAME[2], 0);
            contentValues.put(ACRContract.TestEntry.ACR_WHITELIST_COLUMN_NAME[3], 0);
            contentValues.put(ACRContract.TestEntry.ACR_WHITELIST_COLUMN_NAME[4], 0);
            insertAppConfig(contentValues);
        } else {
            while(cursor.moveToNext()) {
                if(cursor.getInt(1) != audioCollect){
                    contentValues.put(ACRContract.TestEntry._ID, System.currentTimeMillis());
                    contentValues.put(ACRContract.TestEntry.ACR_WHITELIST_COLUMN_NAME[1], audioCollect);
                    context.getContentResolver().update(ACRContract.TestEntry.ACR_WHITELIST_URI, contentValues, selection, null);
                }
            }
        }
    }

    public void insertAppConfig(ContentValues contentValues){
        context.getContentResolver().insert(ACRContract.TestEntry.ACR_WHITELIST_URI, contentValues);
    }

    public int[] getPAQ(String type, String genre){
        Cursor cursor;
        int pq;
        int aq;
        String selection = ACRContract.TestEntry.ACR_PAQ_COLUMN_NAME[0] + "=\'" + type + "\' AND " + ACRContract.TestEntry.ACR_PAQ_COLUMN_NAME[1] + "=\'" + genre + "\'";
        cursor = this.context.getContentResolver().query(ACRContract.TestEntry.ACR_PAQ_URI, null,
                    selection, null, null);
        if(cursor.getCount() > 0){
            cursor.moveToFirst();
            pq = cursor.getInt(3);
            aq = cursor.getInt(4);
            return new int[] {pq, aq};
        } else {
            Log.v(TAG, "show type " + type + " and genre " + genre + " is not in the mapping table");
        }

        return new int[] {1, 0};
    }

    public void insertPAQ(ContentValues contentValues) {
        context.getContentResolver().insert(ACRContract.TestEntry.ACR_PAQ_URI, contentValues);
    }

    public void updatePAQ(String type, String genre, int pq, int aq) {
        ContentValues contentValues = new ContentValues();
//        String [] projection = ACRContract.TestEntry.ACR_PAQ_COLUMN_NAME;
        String selection = ACRContract.TestEntry.ACR_PAQ_COLUMN_NAME[0] + "=\'" + type + "\' AND " + ACRContract.TestEntry.ACR_PAQ_COLUMN_NAME[1] + "=\'" + genre + "\'";

        Cursor cursor = context.getContentResolver().query(ACRContract.TestEntry.ACR_PAQ_URI, null, selection, null, null);
        if(cursor.getCount() < 1){
            contentValues.put(ACRContract.TestEntry._ID, System.currentTimeMillis());
            contentValues.put(ACRContract.TestEntry.ACR_PAQ_COLUMN_NAME[0], type);
            contentValues.put(ACRContract.TestEntry.ACR_PAQ_COLUMN_NAME[1], genre);
            contentValues.put(ACRContract.TestEntry.ACR_PAQ_COLUMN_NAME[2], pq);
            contentValues.put(ACRContract.TestEntry.ACR_PAQ_COLUMN_NAME[3], aq);
            insertPAQ(contentValues);
        } else {
            while(cursor.moveToNext()) {
                if (pq != cursor.getInt(3)) {
                    Log.v(TAG, cursor.getString(1));
                    Log.v(TAG, cursor.getString(2));
                    Log.v(TAG, "pq = " + pq + ", database = " + cursor.getInt(3));
                    contentValues.put(ACRContract.TestEntry._ID, System.currentTimeMillis());
                    contentValues.put(ACRContract.TestEntry.ACR_PAQ_COLUMN_NAME[2], pq);
                    context.getContentResolver().update(ACRContract.TestEntry.ACR_PAQ_URI, contentValues, selection, null);
                }
                if (aq != cursor.getInt(4)) {
                    Log.v(TAG, cursor.getString(1));
                    Log.v(TAG, cursor.getString(2));
                    Log.v(TAG, "pq = " + pq + ", database = " + cursor.getInt(4));
                    contentValues.put(ACRContract.TestEntry._ID, System.currentTimeMillis());
                    contentValues.put(ACRContract.TestEntry.ACR_PAQ_COLUMN_NAME[3], aq);
                    context.getContentResolver().update(ACRContract.TestEntry.ACR_PAQ_URI, contentValues, selection, null);
                }
            }
        }
    }


    public String getInputDate(){
        int day  = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        int year = Calendar.getInstance().get(Calendar.YEAR);
        return year + "/" + day;
    }
    
    /**
     * get the last consent date
     */
    public String getLastConsentDate() {
        
        String consentDate;
        Cursor cursor = context.getContentResolver().query(ACRContract.TestEntry.ACR_SETTINGS_URI, null, null, null, null);
        int num = cursor.getCount();
        if(num < 1) {
            Log.d(TAG, "no acr settings record, init the table");
            insertACR(-1);
            consentDate = getLastConsentDate();
        } else if(num > 1){
            Log.d(TAG, "more than one row found, delete and init again");
            context.getContentResolver().delete(ACRContract.TestEntry.ACR_SETTINGS_URI, null, null);
            insertACR(-1);
            consentDate = getLastConsentDate();
        } else {
            Log.e(TAG, "total data number = " + cursor.getCount());
            cursor.moveToFirst();
            Log.d(TAG, "Consent date is " + cursor.getString(6));
            consentDate = cursor.getString(6);
        }
        return consentDate;
    }

    public Uri insertACR(int acr) {
        String date = getInputDate();
        Log.e(TAG, "the date: " + date);
        ContentValues updateValues = new ContentValues();
        updateValues.put(ACRContract.TestEntry._ID, System.currentTimeMillis());
        updateValues.put(ACRContract.TestEntry.ACR_SETTINGS_COLUMN_NAME[0], acr);
        updateValues.put(ACRContract.TestEntry.ACR_SETTINGS_COLUMN_NAME[1], acr);
        updateValues.put(ACRContract.TestEntry.ACR_SETTINGS_COLUMN_NAME[2], acr);
        updateValues.put(ACRContract.TestEntry.ACR_SETTINGS_COLUMN_NAME[3], acr);
        updateValues.put(ACRContract.TestEntry.ACR_SETTINGS_COLUMN_NAME[4], acr);
        updateValues.put(ACRContract.TestEntry.ACR_SETTINGS_COLUMN_NAME[5], date);

        Uri uri = context.getContentResolver().insert(ACRContract.TestEntry.ACR_SETTINGS_URI, updateValues);
        Cursor cursor = context.getContentResolver().query(ACRContract.TestEntry.ACR_SETTINGS_URI, null, null, null, null);
        Log.e(TAG, "total data number = " + cursor.getCount());

        cursor.moveToFirst();
        Log.e(TAG, "acr = " + cursor.getInt(1)
                + " picture mode = " + cursor.getInt(2)
                + " sound mode = " + cursor.getInt(3)
//                + " content recognition = " + cursor.getInt(4)
//                + " pop up = " + cursor.getInt(5)
                + " date = " + cursor.getString(6));

        return uri;
    }
    public int[] checkACRStatus(){
        int acr[] = {-2, -2, -2};
        Cursor cursor = context.getContentResolver().query(ACRContract.TestEntry.ACR_SETTINGS_URI, null, null, null, null);
        int num = cursor.getCount();
        if(num < 1) {
            Log.d(TAG, "no acr settings record, init the table");
            insertACR(-1);
            acr = checkACRStatus();
        } else if(num > 1){
            Log.d(TAG, "more than one row found, delete and init again");
            context.getContentResolver().delete(ACRContract.TestEntry.ACR_SETTINGS_URI, null, null);
            insertACR(-1);
            acr = checkACRStatus();
        } else {
            Log.e(TAG, "total data number = " + cursor.getCount());
            cursor.moveToFirst();
            Log.e(TAG, "acr = " + cursor.getInt(1)
                    + " picture mode = " + cursor.getInt(2)
                    + " sound mode = " + cursor.getInt(3)
//                    + " content recognition = " + cursor.getInt(4)
//                    + " pop up = " + cursor.getInt(5)
                    + " date = " + cursor.getString(6));
            acr[0] = cursor.getInt(1);
            acr[1] = cursor.getInt(2);
            acr[2] = cursor.getInt(3);
        }
        Log.e(TAG, "acr = " + acr[0]);
        return acr;
    }

    public void updateACR(String entry, int set){
        int acr = -1;
        Cursor cursor = context.getContentResolver().query(ACRContract.TestEntry.ACR_SETTINGS_URI, null, null, null, null);
        if(cursor.getCount() < 1) {
            insertACR(acr);
        }

        String date = getInputDate();
        Log.e(TAG, "the date: " + date);
        ContentValues updateValues = new ContentValues();
        updateValues.put(ACRContract.TestEntry._ID, System.currentTimeMillis());
        switch(entry) {
            case "g_system__acr_status":
                updateValues.put(ACRContract.TestEntry.ACR_SETTINGS_COLUMN_NAME[0], set);
//                updateValues.put(ACRContract.TestEntry.ACR_SETTINGS_COLUMN_NAME[1], set);
//                updateValues.put(ACRContract.TestEntry.ACR_SETTINGS_COLUMN_NAME[2], set);
//                updateValues.put(ACRContract.TestEntry.ACR_SETTINGS_COLUMN_NAME[3], set);
//                updateValues.put(ACRContract.TestEntry.ACR_SETTINGS_COLUMN_NAME[4], set);
                break;
            case "g_system__acr_apic_status":
                updateValues.put(ACRContract.TestEntry.ACR_SETTINGS_COLUMN_NAME[1], set);
                break;
            case "g_system__acr_asound_status":
                updateValues.put(ACRContract.TestEntry.ACR_SETTINGS_COLUMN_NAME[2], set);
                break;
//            case "g_system__acr_cr_status":
//                updateValues.put(ACRContract.TestEntry.ACR_SETTINGS_COLUMN_NAME[3], set);
//                break;
//            case "g_system__acr_pop_status":
//                updateValues.put(ACRContract.TestEntry.ACR_SETTINGS_COLUMN_NAME[4], set);
//                break;
            case "consent_date":
                updateValues.put(ACRContract.TestEntry.ACR_SETTINGS_COLUMN_NAME[5], date);
                break;
            default:

        }

//        updateValues.put(ACRContract.TestEntry.ACR_SETTINGS_COLUMN_NAME[5], date);
        context.getContentResolver().update(ACRContract.TestEntry.ACR_SETTINGS_URI, updateValues, null, null);

        cursor = context.getContentResolver().query(ACRContract.TestEntry.ACR_SETTINGS_URI, null, null, null, null);

        try {
            Log.e(TAG, "total data number = " + cursor.getCount());
            cursor.moveToFirst();
            Log.e(TAG, "acr = " + cursor.getInt(1)
                    + " picture mode = " + cursor.getInt(2)
                    + " sound mode = " + cursor.getInt(3)
//                    + " content recognition = " + cursor.getInt(4)
//                    + " pop up = " + cursor.getInt(5)
                    + " date = " + cursor.getString(6));
        } finally {
            cursor.close();
        }
    }

    public void initToken(){
        ContentValues insertValue = new ContentValues();
        for(int i = 1; i < 3; i++) {
            insertValue.put(ACRContract.TestEntry._ID, i);
            insertValue.put(ACRContract.TestEntry.TOKEN_COLUMN_NAME[0], "null");
            insertValue.put(ACRContract.TestEntry.TOKEN_COLUMN_NAME[1], 0);
            insertValue.put(ACRContract.TestEntry.TOKEN_COLUMN_NAME[2], 0);

            context.getContentResolver().insert(ACRContract.TestEntry.TOKEN_URI, insertValue);
        }
    }

    public void updateToken(int app, String token, long create, int expire){
        Cursor cursor = context.getContentResolver().query(ACRContract.TestEntry.TOKEN_URI, null, null, null, null);
        if(cursor.getCount() < 1){
            initToken();
        }
        ContentValues updateValue = new ContentValues();
        String selection = ACRContract.TestEntry._ID + "=" + app;
        updateValue.put(ACRContract.TestEntry.TOKEN_COLUMN_NAME[0], token);
        updateValue.put(ACRContract.TestEntry.TOKEN_COLUMN_NAME[1], create);
        updateValue.put(ACRContract.TestEntry.TOKEN_COLUMN_NAME[2], expire);

        context.getContentResolver().update(ACRContract.TestEntry.TOKEN_URI, updateValue, selection, null);
        Log.v(TAG, "update " + app  + " with token " + token);
    }

    public String getLauncherToken(){
        String selection = ACRContract.TestEntry._ID + "=1";
        Cursor cursor = context.getContentResolver().query(ACRContract.TestEntry.TOKEN_URI, null, selection, null, null);
        if(cursor.getCount() < 1){
            initToken();
            return null;
        }
        cursor.moveToFirst();
        return cursor.getString(1);
    }

    public String get4KnowToken(){
        String selection = ACRContract.TestEntry._ID + "=2";
        Cursor cursor = context.getContentResolver().query(ACRContract.TestEntry.TOKEN_URI, null, selection, null, null);
        if(cursor.getCount() < 1){
            initToken();
            return null;
        }
        cursor.moveToFirst();
        return cursor.getString(1);
    }

    public Uri initMisc() {
        ContentValues insertValue = new ContentValues();
        insertValue.put(ACRContract.TestEntry._ID, System.currentTimeMillis());
        insertValue.put(ACRContract.TestEntry.MISC_COLUMN_NAME[0], 0);
        insertValue.put(ACRContract.TestEntry.MISC_COLUMN_NAME[1], 0);

        Uri uri = context.getContentResolver().insert(ACRContract.TestEntry.MISC_URI, insertValue);
        return uri;
    }

    public int checkFTE(){
        int FTE = 0;
        Cursor cursor = context.getContentResolver().query(ACRContract.TestEntry.MISC_URI, null, null, null, null);
        if(cursor.getCount() < 1) {
            initMisc();
        } else {
            cursor.moveToFirst();
            FTE = cursor.getInt(1);
        }
        return FTE;
    }

    public int checkKillSwitch(){
        int killSwitch = 0;
        Cursor cursor = context.getContentResolver().query(ACRContract.TestEntry.MISC_URI, null, null, null, null);
        if(cursor.getCount() < 1) {
            initMisc();
        } else {
            cursor.moveToFirst();
            killSwitch = cursor.getInt(2);
        }
        return killSwitch;
    }

    public void updateFTE() {
        Cursor cursor = context.getContentResolver().query(ACRContract.TestEntry.MISC_URI, null, null, null, null);
        if(cursor.getCount() < 1) {
            initMisc();
        }
        ContentValues updateValues = new ContentValues();
        updateValues.put(ACRContract.TestEntry._ID, System.currentTimeMillis());
        updateValues.put(ACRContract.TestEntry.MISC_COLUMN_NAME[0], 1);
        context.getContentResolver().update(ACRContract.TestEntry.MISC_URI, updateValues, null, null);

    }

    public void updateKillSwitch(int kill) {
        Cursor cursor = context.getContentResolver().query(ACRContract.TestEntry.MISC_URI, null, null, null, null);
        if(cursor.getCount() < 1) {
            initMisc();
        }
        ContentValues updateValues = new ContentValues();
        updateValues.put(ACRContract.TestEntry._ID, System.currentTimeMillis());
        updateValues.put(ACRContract.TestEntry.MISC_COLUMN_NAME[1], kill);
        context.getContentResolver().update(ACRContract.TestEntry.MISC_URI, updateValues, null, null);
        Log.v(TAG, "update kill swich to: " + kill);

    }

}
