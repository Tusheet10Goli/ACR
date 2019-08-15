package com.hisense.evservice;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.os.SystemProperties;

import android.os.SystemProperties;
import com.mediatek.twoworlds.tv.HisenseTvAPI;
import com.mediatek.twoworlds.tv.common.MtkTvConfigType;

import static java.lang.Thread.sleep;


public class RefreshService extends Service {
    private static final String TAG = "RefreshService";
    private DBManager dbManager;
    private static final String TV_SCANNING_FLAG_PROPERTY = "sys.hisense.tv.scanning";

    @Override
    public void onCreate() {
        super.onCreate();
        dbManager = DBManager.getInstance(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        new Thread(new Runnable() {
            @Override
            public void run() {
                int killSwitch = getHiCloudInfo(getApplicationContext());
                if(killSwitch == -1){
                    Log.d(TAG, "cannot get token, try again later");
                } else if(killSwitch == 1 && dbManager.checkFTE() == 1){
                    int acrStats = dbManager.checkACRStatus()[0];
                    
                	//get whether the tv is scanning for channels
                	//boolean isScanning = SystemProperties.get(TV_SCANNING_FLAG_PROPERTY, "false").equals("true");
                    
                    if (acrStats == -1) {
                        Log.e(TAG, "no input on the ACR service, show the ACR opt in UI");
                        Log.d(TAG, "the tv is not scanning, show the consent message");
                    	acrInit(getApplicationContext());

                    } else if (acrStats == 1) {
                    	
                    	if (ifACRConsentExpired(getApplicationContext())) {
                    		Log.d(TAG, "the last ACR consent is expired, show the ACR opt in UI once again");
                            Log.d(TAG, "the tv is not scanning, show the consent message");
                        	acrInit(getApplicationContext());
                            
                    	} else {                   	
                    		Log.e(TAG, "start checking foreground service");
                    		startACR(getApplicationContext());
                    	}
                    	
                    } else {
                        Log.e(TAG, "acr service is turned off, no action");
                    }
                } else {
                    Log.d(TAG, "kill switch is " + killSwitch + ", function cannot be enabled");
                }
            }
        }).start();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
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

    /** get kill switch status and aacr mapping info
     * @param context: the current context to get all the related info
     */
    public int getHiCloudInfo(Context context){
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
        return killSwitch;
    }

    /**
     * Check if the ACR consent is expired
     * @return true if expired
     */
    public boolean ifACRConsentExpired(Context context) {

        String lastConsentDate = dbManager.getLastConsentDate();
        String currentDate = dbManager.getInputDate();

        if (lastConsentDate == null) {
            Log.e(TAG, "last consent date not in the mapping table");
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
            Log.e(TAG, "TV has internet, start ACR opt UI");
            Intent evConsent = new Intent(context, EVConsentActivity.class);
            evConsent.addFlags(Intent.FLAG_FROM_BACKGROUND | Intent.FLAG_ACTIVITY_NEW_TASK);
            while(isScanning){
                try{
                    sleep(5 * 60 * 1000);                                                                  // avoid the double thread access to same table
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            context.startActivity(evConsent);
        }
    }

    /** Start the service to check the foreground application
     *  also keep bringing up the service every 1 hour to make sure the service is always there
     * @param context:  the current context which can create a service
     */
    public void startACR(Context context) {
        //todo: check the hisense cloud and other settings
        int useMode = Settings.Global.getInt(context.getContentResolver(), "use_mode", 0);
        Log.e(TAG, "usage mode is " + useMode);
        HisenseTvAPI mHisenseTvAPI = HisenseTvAPI.getInstance(context);
        int country = mHisenseTvAPI.getTvCountry(MtkTvConfigType.CFG_SYSTEM_COUNTRY);
        Log.e(TAG, "the country is " + country);

        Log.d(TAG, "created the service ");
        if (useMode == 0 || country == 59) {
            Intent service = new Intent(context, MService.class);
            context.startService(service);
        }
    }
}
