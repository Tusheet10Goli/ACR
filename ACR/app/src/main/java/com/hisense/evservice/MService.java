package com.hisense.evservice;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class MService extends Service{

    private static final String TAG = "MService";
    private UIMessageHandlerThread workingThread;
    private DBManager dbManager;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public void onCreate(){


        super.onCreate();
        dbManager = DBManager.getInstance(this);
        if (dbManager.checkACRStatus()[0] != 1){
            stopSelf();
        }else {
            Log.e(TAG, "service created");

            workingThread = new UIMessageHandlerThread("whitelist validation thread", this);
            workingThread.start();
            workingThread.registerObserver();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(dbManager.checkACRStatus()[0] != 1) {
            stopSelf();
        } else {
            workingThread.checkApps(this);
            AlarmManager alarms = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
            Intent someIntent = new Intent(this, MService.class); // intent to be launched
            PendingIntent alarmIntent = PendingIntent.getService(this, 1111, someIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            alarms.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 10 * 1000, alarmIntent);

            Log.e(TAG, "start the service alarm set");
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy(){

        super.onDestroy();

        Log.e(TAG, "ACR is turned off");

        //unregister the alarm
        if(isAlarmSet()){
            Log.d(TAG, "registered alarm found, cancel it.");
            AlarmManager alarms = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
            Intent someIntent = new Intent(this, MService.class); // intent to be launched
            PendingIntent alarmIntent = PendingIntent.getService(this, 1111, someIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            alarms.cancel(alarmIntent);
        }

        workingThread.unregisterObserver();
        //quit the thread's looper
        workingThread.quit();
        workingThread.interrupt();


        Log.e(TAG, "destroyed");

    }

    private boolean isAlarmSet(){
        Intent someIntent = new Intent(this, MService.class); // intent to be launched
        PendingIntent alarmIntent = PendingIntent.getService(this, 1111, someIntent, PendingIntent.FLAG_NO_CREATE);
        if(alarmIntent != null) {
            return true;
        }
        return false;
    }

}