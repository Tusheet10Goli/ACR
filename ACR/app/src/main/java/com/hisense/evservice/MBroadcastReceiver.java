package com.hisense.evservice;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.os.SystemProperties;

import com.mediatek.twoworlds.tv.HisenseTvAPI;
import com.mediatek.twoworlds.tv.common.MtkTvConfigType;

import java.util.Locale;

import static java.lang.Thread.sleep;


/** Receive the TV boot up broadcast to determine whether to start the ACR service
 *  Listen to the under layer service broadcast to update the settings status
 */

public class MBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "MBroadcastReceiver";
    private DBManager dbManager;
    private static String pkg = null;
    private static final String TV_SCANNING_FLAG_PROPERTY = "sys.hisense.tv.scanning";
    @Override
    public void onReceive(Context context, Intent intent) {
        dbManager = DBManager.getInstance(context);
        Log.e(TAG, "getAction " + intent.getAction());
        switch (intent.getAction()){
            case "com.hisense.evservice.TEST":
                String genre = intent.getStringExtra("genre");
                String apq_off = intent.getStringExtra("apq");
                String aaq_off = intent.getStringExtra("aaq");

                LayoutInflater inflater = LayoutInflater.from(context);
                View layout = inflater.inflate(R.layout.toast_layout,null);

                TextView toastText = (TextView) layout.findViewById(R.id.paq_toast_text);

                if(genre != null) {
                    String text = "Optimizing for " + genre;
                    toastText.setText(text);
                }
                if(apq_off != null){
                    toastText.setText(apq_off);
                }
                if(aaq_off != null){
                    toastText.setText(aaq_off);
                }
                Toast toast = new Toast(context);
                toast.setGravity(Gravity.TOP | Gravity.RIGHT, 0, 0);
                toast.setDuration(Toast.LENGTH_LONG);
                toast.setView(layout);

                toast.show();
                break;
            case Intent.ACTION_BOOT_COMPLETED:
                Log.e(TAG, Intent.ACTION_BOOT_COMPLETED);
                Intent j = new Intent(context, mMessageService.class);
                context.startService(j);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            sleep(2000);                                                                  // avoid the double thread access to same table
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        int acrStats = dbManager.checkACRStatus()[0];
                        if(isConnected(context)) {
                            getHiCloudInfo(context);
                            int killSwitch = dbManager.checkKillSwitch();
                            if(killSwitch == 1 && dbManager.checkFTE() == 1) {
                                if (acrStats == -1) {                              	
                                    Log.d(TAG, "no input on the ACR service, show the ACR opt in UI");
                                    Log.d(TAG, "the tv is not scanning, show the consent message");
                                	acrInit(context);
                                    
                                } else if (acrStats == 1) {
                                	//if ACR consent is expired
                                	if (ifACRConsentExpired()) {
                                		Log.d(TAG, "the last ACR consent is expired, show the ACR opt in UI once again");
                                        Log.d(TAG, "the tv is not scanning, show the consent message");
                                    	acrInit(context);
                                		
                                	} else {
                                		//if ACR consent is not yet expired
                                		Log.d(TAG, "start checking foreground service");
                                		startACR(context);
                                	}
                                    
                                } else {
                                    Log.d(TAG, "acr service is turned off, no action");
                                }
                            }
                        } else {
                            networkRefresh(context);
                        }

                    }
                }).start();
                break;
            case Intent.ACTION_LOCALE_CHANGED:
                if(msgInterface.hsOptin) {
                    String lan = Locale.getDefault().getLanguage();
                    String locale = Locale.getDefault().toString();
                    HisenseTvAPI mHisenseTvAPI = HisenseTvAPI.getInstance(context);
                    String country = sysInterface.retrieveCountry(mHisenseTvAPI);
                    Log.d(TAG, "language: " + lan + ", country: " + country + ", locale:" + locale);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "start the thread");
                            int acrStats = dbManager.checkACRStatus()[0];
                            if (acrStats == 1) {
                                String updateParams = msgInterface.strUpdateParams(lan, country, locale);
                                if (miBinderManager.BinderIsExist()) {
                                    miBinderManager.SendMsg2iBinder(updateParams);
                                    Log.v(TAG, "handleMessage :=> update params message send: " + updateParams);
                                }
                            }
                        }
                    }).start();
                }
                break;
            case "com.hisense.evservice.Broadcast":
            //todo: also consider the language and country change
                String[] names = {"channel", "input","FTE", "g_system__acr_status", "g_system__acr_apic_status",
                        "g_system__acr_asound_status", "g_system__acr_cr_status", "g_system__acr_pop_status"};
                Log.d(TAG, "receive the broadcast from HQ");
                String stringInfo;
                int intInfo;
                for(String name: names)
                    switch (name) {
                        case "channel":
                            stringInfo = intent.getStringExtra(name);
                            if(stringInfo != null) {
                                Log.d(TAG, name + ": " + stringInfo);
                            }
                            break;
                        case "input":
                            stringInfo = intent.getStringExtra(name);
                            pkg = "com.mediatek.wwtv.tvcenter.";
                            if(stringInfo != null) {
                                Log.d(TAG, name + ": " + stringInfo);
                                pkg = pkg + stringInfo;
                                Log.d(TAG, "input change to: " + pkg);
                            }
                            break;
                        case "FTE":
                            stringInfo = intent.getStringExtra(name);
                            if(stringInfo != null) {
                                Log.d(TAG, name + ": " + stringInfo);
                                dbManager.updateFTE();
                                int killSwitch = dbManager.checkKillSwitch();
                                int acrStatus = dbManager.checkACRStatus()[0];
                                if(killSwitch == 1 && acrStatus == -1) {
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            acrInit(context);
                                        }
                                    }).start();
                                } else {
                                    Log.v(TAG, "kill switch is " + killSwitch + ", not starting the UI activity");
                                }
                            }
                            break;
                        case "g_system__acr_status":
                            intInfo = intent.getIntExtra(name, -1);
                            if(intInfo == 0){
                                Log.d(TAG, name + ": " + intInfo);
                                dbManager.updateACR(names[3], intInfo);
                            }
                            break;
                        case "g_system__acr_apic_status":
                            intInfo = intent.getIntExtra(name, -1);
                            if(intInfo != -1){
                                Log.d(TAG, name + ": " + intInfo);
                                dbManager.updateACR(names[4], intInfo);
                            }
                            break;
                        case "g_system__acr_asound_status":
                            intInfo = intent.getIntExtra(name, -1);
                            if(intInfo != -1){
                                Log.d(TAG, name + ": " + intInfo);
                                dbManager.updateACR(names[5], intInfo);
                            }
                            break;
                        default:

                    }
                break;
            default:
        }
    }

    /** Check if the TV can get access to the internet
     * @param context
     * @return
     */
    public boolean isConnected(Context context){
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnected();
        return isConnected;
    }

    /** Start the service to check the foreground application
     *  also keep bringing up the service every 1 hour to make sure the service is always there
     * @param context:  the current context which can create a service
     */
    public void networkRefresh(Context context){

        Log.e(TAG, "TV has no internet, the service will be functional after connecting to the internet");
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Intent someIntent = new Intent(context, RefreshService.class);
        PendingIntent activityIntent = PendingIntent.getService(context, 0, someIntent, 0);
        NetworkRequest networkRequest = new NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();
        cm.registerNetworkCallback(networkRequest, activityIntent);

    }
    /** get kill switch status and aacr mapping info
     * @param context: the current context to get all the related info
     */
    public void getHiCloudInfo(Context context){
        String token;
        String FKToken;
        int killSwitch = 0;
        int useMode = Settings.Global.getInt(context.getContentResolver(), "use_mode", 0);
        Log.e(TAG, "usage mode is " + useMode);
        HisenseTvAPI mHisenseTvAPI = HisenseTvAPI.getInstance(context);
        int country = mHisenseTvAPI.getTvCountry(MtkTvConfigType.CFG_SYSTEM_COUNTRY);

        Log.e(TAG, "the country is " + country);
//        SharedPreferences mSharedPreferences = context.getSharedPreferences("sp_timestamp", Context.MODE_APPEND);
        if(useMode == 0 && country == 59) {
            if (HiCloudTokenManager.getmInstance(context).getAppToken() != null) {
                token = dbManager.getLauncherToken();
                Log.i(TAG, "lancher token = " + token);
                killSwitch = sysInterface.getSwitch(mHisenseTvAPI, token);
                if (killSwitch == 1) {
                    HiCloudTokenManager.getmInstance(context).get4KNowToken();
                    FKToken = dbManager.get4KnowToken();
//            FKToken = "0plRmtnc0eiSYf4lznteJNLAgbClMqbX9ajSl3yVAd9d1y7zRJRczgVvOpsLhJlnJ5qrRC3-qUr_uflhv";
                    Log.i(TAG, "4k now token = " + FKToken);
                    sysInterface.getMap(mHisenseTvAPI, FKToken, context);
                }
            } else {
                Log.v(TAG, "device id is null");
                AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                Intent someIntent = new Intent(context, RefreshService.class); // intent to be launched
                PendingIntent alarmIntent = PendingIntent.getService(context, 0, someIntent, 0);
                alarms.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 60 * 1000, alarmIntent);
            }
        }
        dbManager.updateKillSwitch(killSwitch);

    }

    /**
     * Check if the ACR consent is expired
     * @return true if expired
     */
    public boolean ifACRConsentExpired() {
    	
    	String lastConsentDate = dbManager.getLastConsentDate();
    	String currentDate = dbManager.getInputDate();
    	
    	if (lastConsentDate == null) {
    		Log.d(TAG, "last consent date not in the mapping table");
    		return true;
    	}
    	
    	//get each year and day of year from last consent date and current date

    	String[] lastConsentDateTokens = lastConsentDate.split("/");
    	int consentYear = Integer.parseInt(lastConsentDateTokens[0]);
    	int consentDay = Integer.parseInt(lastConsentDateTokens[1]);
    	Log.d(TAG, "The year of last consent: " + consentYear);
    	Log.d(TAG, "The day of the last consent year " + consentDay);
    	    	
    	String[] currentDateTokens = currentDate.split("/");
    	int currentYear = Integer.parseInt(currentDateTokens[0]);
    	int currentDay = Integer.parseInt(currentDateTokens[1]);
    	Log.d(TAG, "The year of the current date: " + currentYear);
    	Log.d(TAG, "The day of the current date: " + currentDay);

    	//special case: current year is smaller than consent year,
    	//in this case, re-set the consent date to be current date to resolve
    	//the scenario
    	if (currentYear < consentYear) {
    		dbManager.updateACR("consent_date", 1);
    		Log.e(TAG, "Current year is smaller than consent year, update the consent date");
    		return false;
    	}
    	
    	int days = (currentYear - consentYear) * 365 - consentDay + currentDay;
    	Log.d(TAG, "Number of days between current date and consent date is " + days);
    	if (days >= 700) {
    		Log.d(TAG, "Number of days larger than 700 days, expired");
    		dbManager.updateACR("g_system__acr_status", -1);
    		dbManager.updateACR("g_system__acr_apic_status", -1);
    		dbManager.updateACR("g_system__acr_asound_status", -1);
    		return true;
    	} else {
    		Log.d(TAG, "Number of days smaller than 700 days, not expired");
    		return false;
    	}
    }
    
    /** Init the ACR function: bring up the Enhanced Viewing activity after either FTE or turn on the ACR switch
     *  Only start the activity when there is internet access
     * @param context:  the current context which can create an activity
     */
    public void acrInit(Context context){
        int useMode = Settings.Global.getInt(context.getContentResolver(), "use_mode", 0);
        Log.e(TAG, "usage mode is " + useMode);
        HisenseTvAPI mHisenseTvAPI = HisenseTvAPI.getInstance(context);
        int country = mHisenseTvAPI.getTvCountry(MtkTvConfigType.CFG_SYSTEM_COUNTRY);

        boolean isScanning = SystemProperties.get(TV_SCANNING_FLAG_PROPERTY, "false").equals("true");
        Log.e(TAG, "the country is " + country);
        if (useMode == 0 && country == 59) {
            if (isConnected(context)) {
                Log.e(TAG, "TV has internet, start ACR opt UI");
                Intent evConsent = new Intent(context, EVConsentActivity.class);
                evConsent.addFlags(Intent.FLAG_FROM_BACKGROUND | Intent.FLAG_ACTIVITY_NEW_TASK);
                while(isScanning){
                    try {
                        sleep(5 * 60 * 1000);                                                                  // avoid the double thread access to same table
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                context.startActivity(evConsent);
            } else {
                Log.e(TAG, "TV has no internet, register the alarm manager to bring up the activity after connecting the internet");
                ConnectivityManager cm =
                        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                Intent someIntent = new Intent(context, EVConsentActivity.class);
                PendingIntent activityIntent = PendingIntent.getActivity(context, 0, someIntent, 0);
                NetworkRequest networkRequest = new NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();
                cm.registerNetworkCallback(networkRequest, activityIntent);
            }
        }
    }

    /** Start the service to check the foreground application
     *  also keep bringing up the service every 1 hour to make sure the service is always there
     * @param context:  the current context which can create a service
     */
    public void startACR(Context context){
        //todo: check the hisense cloud and other settings
        int useMode = Settings.Global.getInt(context.getContentResolver(), "use_mode", 0);
        Log.e(TAG, "usage mode is " + useMode);
        HisenseTvAPI mHisenseTvAPI = HisenseTvAPI.getInstance(context);
        int country = mHisenseTvAPI.getTvCountry(MtkTvConfigType.CFG_SYSTEM_COUNTRY);
        Log.e(TAG, "the country is " + country);

        Log.d(TAG, "created the service ");
        if (useMode == 0 || country == 59) {
            if (isConnected(context)) {
                Intent service = new Intent(context, MService.class);
                context.startService(service);

            } else {
                Log.e(TAG, "TV has no internet, the service will be functional after connecting to the internet");
                ConnectivityManager cm =
                        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                Intent someIntent = new Intent(context, MService.class);
                PendingIntent activityIntent = PendingIntent.getService(context, 0, someIntent, 0);
                NetworkRequest networkRequest = new NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();
                cm.registerNetworkCallback(networkRequest, activityIntent);
            }
        }

    }

    public static String getPkg() {
        Log.d(TAG, "pkg = " + pkg);
        return pkg;
    }

}
