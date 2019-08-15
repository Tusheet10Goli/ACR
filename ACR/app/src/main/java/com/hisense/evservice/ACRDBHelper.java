package com.hisense.evservice;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class ACRDBHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "aacr.db";

    public ACRDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        Log.e("ACRDBhelper", "create the table");

        final String SQL_CREATE_ACR_SETTINGS_TABLE = "CREATE TABLE " + ACRContract.TestEntry.ACR_SETTINGS_TABLE_NAME + "( "
                + ACRContract.TestEntry._ID + " TEXT PRIMARY KEY, "
                + ACRContract.TestEntry.ACR_SETTINGS_COLUMN_NAME[0] + " INTEGER NOT NULL, "
                + ACRContract.TestEntry.ACR_SETTINGS_COLUMN_NAME[1] + " INTEGER NOT NULL, "
                + ACRContract.TestEntry.ACR_SETTINGS_COLUMN_NAME[2] + " INTEGER NOT NULL, "
                + ACRContract.TestEntry.ACR_SETTINGS_COLUMN_NAME[3] + " INTEGER NOT NULL, "
                + ACRContract.TestEntry.ACR_SETTINGS_COLUMN_NAME[4] + " INTEGER NOT NULL, "
                + ACRContract.TestEntry.ACR_SETTINGS_COLUMN_NAME[5] + " TEXT NOT NULL);";

        db.execSQL(SQL_CREATE_ACR_SETTINGS_TABLE);

        final String SQL_CREATE_ACR_PAQ_TABLE = "CREATE TABLE " + ACRContract.TestEntry.ACR_PAQ_TABLE_NAME + "( "
                + ACRContract.TestEntry._ID + " TEXT PRIMARY KEY, "
                + ACRContract.TestEntry.ACR_PAQ_COLUMN_NAME[0] + " TEXT NOT NULL, "
                + ACRContract.TestEntry.ACR_PAQ_COLUMN_NAME[1] + " TEXT NOT NULL, "
                + ACRContract.TestEntry.ACR_PAQ_COLUMN_NAME[2] + " INTEGER NOT NULL,"
                + ACRContract.TestEntry.ACR_PAQ_COLUMN_NAME[3] + " INTEGER NOT NULL);";

        db.execSQL(SQL_CREATE_ACR_PAQ_TABLE);

////////////////////////////////////////////recommendation related///////////////////////////////////
//        final String SQL_CREATE_ACR_DEEPLINK_TABLE = "CREATE TABLE " + ACRContract.TestEntry.ACR_DEEPLINK_TABLE_NAME + "( "
//                + ACRContract.TestEntry._ID + " TEXT PRIMARY KEY, "
//                + ACRContract.TestEntry.ACR_DEEPLINK_COLUMN_NAME[0] + " TEXT NOT NULL, "
//                + ACRContract.TestEntry.ACR_DEEPLINK_COLUMN_NAME[1] + " TEXT NOT NULL, "
//                + ACRContract.TestEntry.ACR_DEEPLINK_COLUMN_NAME[2] + " TEXT NOT NULL, "
//                + ACRContract.TestEntry.ACR_DEEPLINK_COLUMN_NAME[3] + " TEXT NOT NULL);";
//
//        db.execSQL(SQL_CREATE_ACR_DEEPLINK_TABLE);
/////////////////////////////////////////////////////////////////////////////////////////////////////

        final String SQL_CREATE_MISC_TABLE = "CREATE TABLE " + ACRContract.TestEntry.MISC_TABLE_NAME + "( "
                + ACRContract.TestEntry._ID + " TEXT PRIMARY KEY, "
                + ACRContract.TestEntry.MISC_COLUMN_NAME[0] + " INTEGER NOT NULL, "
                + ACRContract.TestEntry.MISC_COLUMN_NAME[1] + " INTEGER NOT NULL);";

        db.execSQL(SQL_CREATE_MISC_TABLE);

        final String SQL_CREATE_TOKEN_TABLE = "CREATE TABLE " + ACRContract.TestEntry.TOKEN_TABLE_NAME + "( "
                + ACRContract.TestEntry._ID + " INTEGER PRIMARY KEY, "
                + ACRContract.TestEntry.TOKEN_COLUMN_NAME[0] + " TEXT NOT NULL, "
                + ACRContract.TestEntry.TOKEN_COLUMN_NAME[1] + " INTEGER,"
                + ACRContract.TestEntry.TOKEN_COLUMN_NAME[2] + " INTEGER);";

        db.execSQL(SQL_CREATE_TOKEN_TABLE);

        final String SQL_CREATE_ACR_WHITELIST_TABLE = "CREATE TABLE " + ACRContract.TestEntry.ACR_WHITELIST_TABLE_NAME + "( "
                + ACRContract.TestEntry._ID + " TEXT PRIMARY KEY, "
                + ACRContract.TestEntry.ACR_WHITELIST_COLUMN_NAME[0] + " TEXT NOT NULL, "
                + ACRContract.TestEntry.ACR_WHITELIST_COLUMN_NAME[1] + " INTEGER NOT NULL, "
                + ACRContract.TestEntry.ACR_WHITELIST_COLUMN_NAME[2] + " INTEGER NOT NULL, "
                + ACRContract.TestEntry.ACR_WHITELIST_COLUMN_NAME[3] + " INTEGER NOT NULL, "
                + ACRContract.TestEntry.ACR_WHITELIST_COLUMN_NAME[4] + " INTEGER NOT NULL);";
        db.execSQL(SQL_CREATE_ACR_WHITELIST_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + ACRContract.TestEntry.ACR_SETTINGS_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ACRContract.TestEntry.ACR_PAQ_TABLE_NAME);
//        db.execSQL("DROP TABLE IF EXISTS " + ACRContract.TestEntry.ACR_DEEPLINK_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ACRContract.TestEntry.MISC_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ACRContract.TestEntry.ACR_WHITELIST_TABLE_NAME);
        onCreate(db);
    }
}
