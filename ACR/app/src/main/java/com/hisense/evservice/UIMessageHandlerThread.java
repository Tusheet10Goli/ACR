package com.hisense.evservice;

import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import com.mediatek.twoworlds.tv.HisenseTvAPI;


public final class UIMessageHandlerThread extends HandlerThread implements Handler.Callback {

    private static final String TAG = "MService-UIMessageHandlerThread";

    public static final int COLLECT_THE_AUDIO   = 100;
    public static final int IGNORE_THE_AUDIO    = 101;
    public static final int UPDATE_SOURCE       = 200;

    private static UIMessageHandlerThread uiMessageHandlerThread;

    private Handler workingHandler;
    private Context context;
    private ACRProviderObserver observer;
    private DBManager dbManager;
    public static boolean acr = true;
    public static boolean isAutoAQ;
    public static boolean isAutoPQ;
    public static boolean checkState = false;
    private String previousApp = null;

    public UIMessageHandlerThread(String name, Context context) {

        super(name);

        this.context = context;
        this.dbManager = DBManager.getInstance(context);
        int[] acrStatus = dbManager.checkACRStatus();
        isAutoAQ = (acrStatus[2] == 1);
        isAutoPQ = (acrStatus[1] == 1);
        Log.d(TAG, "handler thread created");

    }

//    public synchronized static UIMessageHandlerThread getInstance (String name, Context context){
//        if(uiMessageHandlerThread == null){
//            uiMessageHandlerThread = new UIMessageHandlerThread(name, context);
//        }
//
//        return uiMessageHandlerThread;
//    }

    @Override
    protected void onLooperPrepared() {
        workingHandler = new Handler(getLooper(), this);
        Log.e(TAG, "working handler created");

        HisenseTvAPI mHisenseTvAPI = HisenseTvAPI.getInstance(context);

        if(!msgInterface.checkInitStatus()) {
            String [] props = sysInterface.getProps(mHisenseTvAPI);
            String init_message = msgInterface.strInit(props);
            Log.v(TAG, "going to send init message: " + init_message);
            if (init_message != null) {
                miBinderManager.SendMsg2iBinder(init_message);
            }
        }

        if(!dbManager.getAppConfig("com.jamdeo.tv.mediacenter")) {
            dbManager.updateAppConfig("com.jamdeo.tv.mediacenter", "true");
        }
        if(!dbManager.getAppConfig("com.mediatek.wwtv.tvcenter.TV")){
            dbManager.updateAppConfig("com.mediatek.wwtv.tvcenter.TV", "true");
        }
        if(!dbManager.getAppConfig("com.hisense.evservice")){
            dbManager.updateAppConfig("com.hisense.evservice", "true");
        }
        if(!dbManager.getAppConfig("com.android.tv.settings")){
            dbManager.updateAppConfig("com.android.tv.settings", "true");
        }

    }


    public void registerObserver(){
        Log.e(TAG, "create the observer");
        this.observer = new ACRProviderObserver(context, workingHandler);
        context.getContentResolver().registerContentObserver(ACRContract.TestEntry.ACR_SETTINGS_URI, true, observer);
        observer.observe();
    }

    public void unregisterObserver(){
        context.getContentResolver().unregisterContentObserver(observer);
        Log.e(TAG, "observer unregistered");
    }

    public void checkApps(Context context){
        if(msgInterface.checkInitStatus()) {
            String pkg = checkForeApp(context);
            if (pkg != null && pkg.equals("com.mediatek.wwtv.tvcenter")) {
                pkg = MBroadcastReceiver.getPkg();
            }



            Log.d(TAG, "check pkg : " + pkg);
            whitelistValidation(pkg);
        } else {
            Log.e(TAG, "hamservice not up, check later");
//            msgInterface.hsOptin = true;
//            String opt_in_string = msgInterface.strOptIn(true);
//            if (miBinderManager.BinderIsExist()) {
//                miBinderManager.SendMsg2iBinder(opt_in_string);
//                Log.v(TAG, "handleMessage :=> opt in message send: " + opt_in_string);
//            }
        }
    }

    private String checkForeApp(Context context) {

        String currentApp = null;
        if (Build.VERSION.SDK_INT >= 21) {
            UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            List<UsageStats> applist = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000* 1000, time);

            if (applist != null && applist.size() > 0) {
                Log.d(TAG, "creating the applist");
                SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>();
                for (UsageStats usageStats : applist) {
                    Log.d("Executed app", "usage stats executed : " +usageStats.getPackageName());
                    mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
                }
                if (mySortedMap != null && !mySortedMap.isEmpty()) {
                    Log.d(TAG, "get the current app");
                    currentApp = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
                }
            }
            Log.e(TAG, "use sdk>=21 Current App in foreground is: " + currentApp);
        }else {

            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            currentApp=(manager.getRunningTasks(1).get(0)).topActivity.getPackageName();
            Log.e(TAG, "use sdk<21 Current App in foreground is: " + currentApp);
        }
        return currentApp;
    }

    public void validate(String app){
    	
    	//first check if the app is outside of apps list
    	
    	boolean insideList = dbManager.ifInsideWhiteList(app);
    	boolean audioCollect = false;
    	
    	Log.d(TAG, "validate started");
    	
    	if (!insideList) {
    		//if outside whitelist, use default value from database
    		Log.d(TAG, "Outside whitelist, value insideList: " + insideList);
    		Log.d(TAG, "App is outside the whitelist");
    		audioCollect = dbManager.getAppConfig("defaults");
    		Log.d(TAG, "Default value outside whitelist: " + audioCollect);
    	} else {
    		//if inside whitelist, use value from the whitelist
    		Log.d(TAG, "Inside whitelist, value insideList: " + insideList);
    		Log.d(TAG, "app is inside the whitelist");
    		audioCollect = dbManager.getAppConfig(app);
    		Log.d(TAG, "ACR Value for the app in the whitelist: " + audioCollect);
    	}
    		
        if (audioCollect) {
        	if (!msgInterface.audioCollect) {
                Log.d(TAG, "audio collection changed to " + audioCollect);
                Message message = Message.obtain(null, COLLECT_THE_AUDIO);
                workingHandler.sendMessage(message);
            } else {
                Log.d(TAG, "audio collection stays " + audioCollect);
            }

        } else {
            if (msgInterface.audioCollect) {
                Log.d(TAG, "audio collection changed to " + audioCollect);
                Message message = Message.obtain(null, IGNORE_THE_AUDIO);
                workingHandler.sendMessage(message);
            } else {
                Log.d(TAG, "audio collection stays " + audioCollect);
            }
        }
    	
        Message message = Message.obtain(null, UPDATE_SOURCE);
        Bundle bundle = new Bundle();
        bundle.putString("app", app);
        message.setData(bundle);
        workingHandler.sendMessage(message);
    }

    public void whitelistValidation(String app) {
        if(app != null) {
            if (previousApp == null) {
                Log.e(TAG, "first time collection, validate the app");
                Log.e(TAG, "the current app is: " + app);
                validate(app);
                previousApp = app;
            } else if (previousApp != null && !previousApp.equals(app)) {
                Log.e(TAG, "app changed, start validation");
                Log.e(TAG, "the current app is: " + app);
                validate(app);
                previousApp = app;
            } else {
                Log.e(TAG, "app stays the same, no more validation.");
                Log.e(TAG, "the current app is: " + app);
            }
        } else {
            Log.e(TAG, "current app has not been changed for a long time so that the the check time is expired.");
            Log.e(TAG, "the actual running application is: " + previousApp);
        }
    }


    @Override
    public boolean handleMessage(Message msg) {
        if(dbManager.checkACRStatus()[0] != 1) {
            Log.e(TAG, "receive acr turned off");
            if(msgInterface.hsOptin) {
                String opt_out = msgInterface.strOptIn(false);
//                msgInterface.hsOptin = false;
                acr = false;
                isAutoAQ = false;
                isAutoPQ = false;
                if (miBinderManager.BinderIsExist()) {
                    miBinderManager.SendMsg2iBinder(opt_out);
                    Log.v(TAG, "handleMessage :=> opt out message send: " + opt_out);
                }
            }

            context.stopService(new Intent(context, MService.class));
            quit();
        } else {
            if(!msgInterface.hsOptin) {
                Log.v(TAG, "hamservice opt in");
                String opt_in_string = msgInterface.strOptIn(true);
                String ignore_audio = msgInterface.strAudioStart(false);
                if (miBinderManager.BinderIsExist()) {
                    miBinderManager.SendMsg2iBinder(opt_in_string);
                    Log.v(TAG, "handleMessage :=> opt in message send: " + opt_in_string);
                    miBinderManager.SendMsg2iBinder(ignore_audio);
                    Log.v(TAG, "handleMessage :=> opt in message send: " + ignore_audio);
                }
            }
            switch(msg.what) {
                case COLLECT_THE_AUDIO:
                    //send opt-in
                   if(!msgInterface.audioCollect){
                        Log.v(TAG, "hamservice already opt in, start collect audio");
                        String collect_audio = msgInterface.strAudioStart(true);
                        if(miBinderManager.BinderIsExist()) {
                            miBinderManager.SendMsg2iBinder(collect_audio);
                            Log.v(TAG, "handleMessage :=> opt in message send: " + collect_audio);
                        }
                    }
                    Log.e(TAG, "start collect audio");
                    break;
                case IGNORE_THE_AUDIO:
                    //send opt-out
                    if(msgInterface.audioCollect){
                        String ignore_audio = msgInterface.strAudioStart(false);
                        if(miBinderManager.BinderIsExist()) {
                            miBinderManager.SendMsg2iBinder(ignore_audio);
                            Log.v(TAG, "handleMessage :=> opt in message send: " + ignore_audio);
                        }
                    }
                    Log.e(TAG, "stop collecting");
                    break;
                case UPDATE_SOURCE:
                    String app = msg.getData().getString("app");
                    String sourceUpdate = msgInterface.strUpdateSource(app);
                    if (miBinderManager.BinderIsExist()) {
                        miBinderManager.SendMsg2iBinder(sourceUpdate);
                        Log.v(TAG, "handleMessage :=> change source message send: " + sourceUpdate);
                    }
                    break;
                default:
                    Log.e(TAG, "the message is " + msg.what);
            }

        }
        return false;
    }

}
