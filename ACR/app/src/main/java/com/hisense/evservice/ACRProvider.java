package com.hisense.evservice;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

public class ACRProvider extends ContentProvider {
    private ACRDBHelper mOpenHelper;
    private final String TAG = "ACRProvider";

    @Override
    public boolean onCreate() {
        // TODO: Implement this to initialize your content provider on startup.

        Log.e(TAG, "provider created");
        mOpenHelper = new ACRDBHelper(getContext());
        Log.e(TAG, "get context");
        return true;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Implement this to handle requests to delete one or more rows.
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        switch (buildUriMatcher().match(uri)) {
            case ACR_SETTING:
                db.delete(ACRContract.TestEntry.ACR_SETTINGS_TABLE_NAME, null, null);
                break;
            default:
                Log.d(TAG, "uri: " + uri);
        }
        return 0;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        // TODO: Implement this to handle requests for the MIME type of the data
        // at the given URI.
        return null;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO: Implement this to handle requests to insert a new row.
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Uri returnUri;
        long _id;

        Log.e(TAG, "insert start");
        switch (buildUriMatcher().match(uri)) {
            case ACR_SETTING:
                Log.e(TAG, "test case matched and insert");
                _id = db.insert(ACRContract.TestEntry.ACR_SETTINGS_TABLE_NAME, null, values);
                if(_id > 0)
                    returnUri = ACRContract.TestEntry.buildUri(ACRContract.TestEntry.ACR_SETTINGS_URI, _id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            case ACR_PAQ:
                Log.e(TAG, "PQ/AQ case matched and insert");
                _id = db.insert(ACRContract.TestEntry.ACR_PAQ_TABLE_NAME, null, values);
                if(_id > 0)
                    returnUri = ACRContract.TestEntry.buildUri(ACRContract.TestEntry.ACR_PAQ_URI, _id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
//            case ACR_DEEP_LINK:
//                Log.e(TAG, "deep link case matched and insert");
//                _id = db.insert(ACRContract.TestEntry.ACR_DEEPLINK_TABLE_NAME, null, values);
//                if(_id > 0)
//                    returnUri = ACRContract.TestEntry.buildUri(ACRContract.TestEntry.ACR_DEEPLINK_URI, _id);
//                else
//                    throw new android.database.SQLException("Failed to insert row into " + uri);
//                break;
            case ACR_WHITELIST:
                Log.e(TAG, "white list case matched and insert");
                _id = db.insert(ACRContract.TestEntry.ACR_WHITELIST_TABLE_NAME, null, values);
                if(_id > 0)
                    returnUri = ACRContract.TestEntry.buildUri(ACRContract.TestEntry.ACR_WHITELIST_URI, _id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            case MISC:
                Log.e(TAG, "misc case matched and insert");
                _id = db.insert(ACRContract.TestEntry.MISC_TABLE_NAME, null, values);
                if(_id > 0)
                    returnUri = ACRContract.TestEntry.buildUri(ACRContract.TestEntry.MISC_URI, _id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            case TOKEN:
                Log.e(TAG, "token case matched and insert");
                _id = db.insert(ACRContract.TestEntry.TOKEN_TABLE_NAME, null, values);
                if(_id > 0)
                    returnUri = ACRContract.TestEntry.buildUri(ACRContract.TestEntry.TOKEN_URI, _id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            default:
                throw new android.database.SQLException("Unknown uri: " + uri);
        }

        return returnUri;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        // Implement this to handle query requests from clients.
        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        Cursor cursor = null;
        switch(buildUriMatcher().match(uri)) {
            case ACR_SETTING:
                cursor = db.query(ACRContract.TestEntry.ACR_SETTINGS_TABLE_NAME, projection, selection, selectionArgs, sortOrder, null, null);
                break;
            case ACR_PAQ:
                cursor = db.query(ACRContract.TestEntry.ACR_PAQ_TABLE_NAME, projection, selection, selectionArgs, sortOrder, null, null);
                break;
//            case ACR_DEEP_LINK:
//                cursor = db.query(ACRContract.TestEntry.ACR_DEEPLINK_TABLE_NAME, projection, selection, selectionArgs, sortOrder, null, null);
//                break;
            case TOKEN:
                cursor = db.query(ACRContract.TestEntry.TOKEN_TABLE_NAME, projection, selection, selectionArgs, sortOrder, null, null);
                break;
            case ACR_WHITELIST:
                cursor = db.query(ACRContract.TestEntry.ACR_WHITELIST_TABLE_NAME, projection, selection, selectionArgs, sortOrder, null, null);
                break;
            case MISC:
                cursor = db.query(ACRContract.TestEntry.MISC_TABLE_NAME, projection, selection, selectionArgs, sortOrder, null, null);
                break;
            default:
                throw new android.database.SQLException("Unknown uri: " + uri);

        }

        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        // TODO: Implement this to handle requests to update one or more rows.
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Uri settingsUri = ACRContract.TestEntry.buildUri(ACRContract.TestEntry.ACR_SETTINGS_URI, 1);
        int number;
        switch(buildUriMatcher().match(uri)) {
            case ACR_SETTING:
                number = db.update(ACRContract.TestEntry.ACR_SETTINGS_TABLE_NAME, values, selection, selectionArgs);
                getContext().getContentResolver().notifyChange(settingsUri, null);
                return number;
            case ACR_PAQ:
                number = db.update(ACRContract.TestEntry.ACR_PAQ_TABLE_NAME, values, selection, selectionArgs);
                return number;
//            case ACR_DEEP_LINK:
//                number = db.update(ACRContract.TestEntry.ACR_DEEPLINK_TABLE_NAME, values, selection, selectionArgs);
//                return number;
            case ACR_WHITELIST:
                number = db.update(ACRContract.TestEntry.ACR_WHITELIST_TABLE_NAME, values, selection, selectionArgs);
                return number;
            case MISC:
                number = db.update(ACRContract.TestEntry.MISC_TABLE_NAME, values, selection, selectionArgs);
                return number;
            case TOKEN:
                number = db.update(ACRContract.TestEntry.TOKEN_TABLE_NAME, values, selection, selectionArgs);
                return number;
            default:
                throw new IllegalArgumentException("Unknown uri " + uri);

        }
    }


    private final static int ACR           = 100;
    private final static int ACR_SETTING   = 200;
    private final static int ACR_PAQ       = 300;
    private final static int MISC          = 400;
//    private final static int ACR_DEEP_LINK = 500;
    private final static int ACR_WHITELIST = 600;
    private final static int TOKEN         = 700;

    static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = ACRContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, ACRContract.PATH_ACR, ACR);
        matcher.addURI(authority, "acr/settings", ACR_SETTING);
        matcher.addURI(authority, "acr/PAQ", ACR_PAQ);
//        matcher.addURI(authority, "acr/deeplink", ACR_DEEP_LINK);
        matcher.addURI(authority, "acr/whitelist", ACR_WHITELIST);
        matcher.addURI(authority, "acr/misc", MISC);
        matcher.addURI(authority, "acr/token", TOKEN);

        return matcher;
    }
}
