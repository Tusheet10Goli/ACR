package com.hisense.evservice;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

public class ACRProviderObserver extends ContentObserver {
    /**
     * Creates a content observer.
     *
     * @param handler The handler to run {@link #onChange} on, or null if none.
     */

    private static final String TAG = "ACRProviderObserver";

    private Context context;
    private ContentResolver resolver;
    private Handler workingHandler;
    private int acr, pq, aq, cntrcm, pop;


    public ACRProviderObserver(Context context, Handler handler) {
        super(handler);
        this.context = context;
        this.resolver = context.getContentResolver();
        this.workingHandler = handler;
    }

    public void observe(){
        Cursor cursor = resolver.query(ACRContract.TestEntry.ACR_SETTINGS_URI, null, null, null, null);
        try {
            Log.e(TAG, "all changed result = " + cursor.getCount());
            cursor.moveToFirst();
            acr = cursor.getInt(1);
            pq = cursor.getInt(2);
            aq = cursor.getInt(3);
            cntrcm = cursor.getInt(4);
            pop = cursor.getInt(5);
            Log.e(TAG, "acr = " + acr + " picture mode = " + pq + " sound mode = " + aq
                    + " content recommendation = " + cntrcm + " pop up = " + pop
                    + " date = " + cursor.getString(6));
        } finally {
            cursor.close();
        }
    }

    @Override
    public void onChange(boolean selfChange) {
        onChange(selfChange, null);
    }
    @Override
    public void onChange(boolean selfChange, Uri uri){

        Log.e(TAG, "there is a change occurred");
        Cursor cursor;
        Message message;
        if(uri == null) {

        } else if(uri.toString().equals("content://com.hisense.evservice/acr/settings/1")) {
            uri = ACRContract.TestEntry.ACR_SETTINGS_URI;
            cursor = resolver.query(uri, null, null, null, null);
            cursor.moveToFirst();
            Log.e(TAG, "all changed result = " + cursor.getCount());
            Log.e(TAG, "acr = " + cursor.getInt(1)
                    + " picture mode = " + cursor.getInt(2)
                    + " sound mode = " + cursor.getInt(3)
//                    + " content recognition = " + cursor.getInt(4)
//                    + " pop up = " + cursor.getInt(5)
                    + " date = " + cursor.getInt(6));

            Log.e(TAG, "acr = " + acr + ", pq = " + pq + ", aq= " + aq ) ;

            if(acr != cursor.getInt(1) && acr == 1){
                Log.d(TAG, "send message off");
                Log.d(TAG, "user turn acr " + cursor.getInt(1));
                acr = cursor.getInt(1);
                pq = cursor.getInt(2);
                aq = cursor.getInt(3);
                if (msgInterface.hsOptin) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String opt_out_string = msgInterface.strOptIn(false);
                            if(miBinderManager.BinderIsExist()) {
                                miBinderManager.SendMsg2iBinder(opt_out_string);
                                Log.v(TAG, "handleMessage :=> opt out message send: " + opt_out_string);
                            }
                            Log.e(TAG, "stop collecting");
                        }
                    }).start();
                }
            } else if ( pq != cursor.getInt(2)) {
                if (cursor.getInt(2) == 0) {
                    Log.v(TAG, "auto pq turned off");
                    String pqText = context.getResources().getString(R.string.toast_cancel_apq); //"The Picture mode adaptation function will be closed";
                    Intent broadcast = new Intent();
                    broadcast.putExtra("apq", pqText);
                    broadcast.setAction("com.hisense.evservice.TEST");
                    broadcast.setComponent(new ComponentName("com.hisense.evservice", "com.hisense.evservice.MBroadcastReceiver"));
                    context.sendBroadcast(broadcast);
                }
                Log.d(TAG, "user turn auto pq " + cursor.getInt(2));
                pq = cursor.getInt(2);
            } else if (aq != cursor.getInt(3)) {
                if (cursor.getInt(3) == 0) {
                    Log.v(TAG, "auto pq turned off");
                    String aqText = context.getResources().getString(R.string.toast_cancel_apq); //"The Audio mode adaptation function will be closed";
                    Intent broadcast = new Intent();
                    broadcast.putExtra("aaq", aqText);
                    broadcast.setAction("com.hisense.evservice.TEST");
                    broadcast.setComponent(new ComponentName("com.hisense.evservice", "com.hisense.evservice.MBroadcastReceiver"));
                    context.sendBroadcast(broadcast);
                }
                Log.d(TAG, "user turn auto aq " + cursor.getInt(3));
                aq = cursor.getInt(3);
            }
        } else {
            Log.e(TAG, uri.toString());
        }

    }
}
