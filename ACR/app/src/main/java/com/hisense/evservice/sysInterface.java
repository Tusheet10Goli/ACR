package com.hisense.evservice;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.os.SystemProperties;

import com.mediatek.twoworlds.tv.HisenseTvAPI;
import com.mediatek.twoworlds.tv.HisenseTvAPIBase;
import com.mediatek.twoworlds.tv.MtkTvConfig;
import com.mediatek.twoworlds.tv.common.MtkTvConfigTypeBase;
import com.mediatek.twoworlds.tv.common.MtkTvConfigType;

//è¦�ä½¿ç”¨MenuConfigManagerçš„æŽ¥å�£ï¼Œéœ€è¦�importå¦‚ä¸‹çš„æ–‡ä»¶
//import com.mediatek.wwtv.setting.util.MenuConfigManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/* ***********************
 *  This class is designed as a static class.
 *  The member functions can be called to:
 *    1. retrieve system info
 *    2. generate message and push it to internal message queue
 *    3. retrieve and update PQ/AQ
 * ************************/
public class sysInterface {
    private static final String TAG = "sysInterface";
    private static final Map<Integer, String> brandMap = createBrandMap();
    private static final Map<String, Integer> PQMap = createPQMap();
    private static final Map<String, Integer> AQMap = createAQMap();

    //private mHisenseTvAPI = HisenseTvAPI.getInstance(getApplicationContext());
    //public MtkTvConfig mTvConfig;
    private static Map<Integer, String> createBrandMap() {
        //TODO: here I use fake value
        Map<Integer, String> result = new HashMap<Integer, String>();
        result.put(1, "Hisense");
        result.put(2, "Sharp");
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, Integer> createPQMap() {
        //TODO: here I use fake value
        Map<String, Integer> result = new HashMap<String, Integer>();
        result.put("vivid", 0);
        result.put("standard", 1);
        result.put("energy-saving", 2);
        result.put("theater",3);
        result.put("theater day",3);
        result.put("game",4);
        result.put("Sports",5);
        result.put("calibrated",6);
        result.put("theater night",6);
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, Integer> createAQMap() {
        //TODO: here I use fake value
        Map<String, Integer> result = new HashMap<String, Integer>();
        result.put("standard", 0);
        result.put("movie", 1);
        result.put("Sports",2);
        result.put("concert",3);
        result.put("music",4);
        result.put("speech",5);
        result.put("night",6);
        result.put("user",7);

        return Collections.unmodifiableMap(result);
    }

    public static boolean initHamService(HisenseTvAPI mHisenseTvAPI){//mHisenseTvAPI has to be passed in

        //step 0: send the callback first!
        miBinderManager.SendCallback2iBinder();

        //step 1: get brand name
        int brand = mHisenseTvAPI.getBootUpLogo(MtkTvConfigType.CFG_FACTORY_FAC_BOOTUP_LOGO);
        //Settings.Global.getInt(mContext.getContentResolver(), MtkTvConfigType.CFG_FACTORY_FAC_BOOTUP_LOGO, brand);//alternative method
        if(!brandMap.containsKey(brand)){
            //TODO: what is the value here? How we map those value to brands?
            //TODO: If not retrieved successfully, return false or use a default value
            return false;
        }else{
            try {
                String brandMessage = msgInterface.strSystemInfo("brand", brandMap.get(brand));
                //here we need to send this to C++ process via ibinder
                miBinderManager.SendMsg2iBinder(brandMessage);
                Log.v(TAG, "initHamService :=> send message: " + brandMessage);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        //step 2: get model number
        String modelNumber;
        modelNumber = mHisenseTvAPI.getMachineName(MtkTvConfigType.CFG_FACTORY_FAC_MACHINE_NAME);
        if(modelNumber.equals("")){
            //TODO: If not retrieved successfully, return false or use a default value
            return false;
        }else{
            try {
                String modelNumberMessage = msgInterface.strSystemInfo("model_number", modelNumber);
                //There we need to send this to C++ process via ibinder
                miBinderManager.SendMsg2iBinder(modelNumberMessage);
                Log.v(TAG, "initHamService :=> send message: " + modelNumberMessage);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }



        return true;
    }

    public String retrievePQmode(){
        return MtkTvConfigType.CFG_VIDEO_PIC_MODE;
    }

    public String retrieveAQmode(){
        return MtkTvConfigType.CFG_AUD_SOUND_MODE;
    }

    public static void setPQmode(int mode){
        MtkTvConfig mTvConfig = MtkTvConfig.getInstance();
        Log.v(TAG, "setPQmode: " + mode);
        mTvConfig.setConfigValue("g_video__picture_mode", mode);//default to be 1:standard
        //TODO: show toast for CES demo
    }

    public static void setAQmode(int mode){
        MtkTvConfig mTvConfig = MtkTvConfig.getInstance();
        Log.v(TAG, "setAQmode: " + mode);
        mTvConfig.setConfigValue("g_audio__sound_mode", mode);////default to be 1:standard
    }

    public static String retrieveCountry(HisenseTvAPI mHisenseTvAPI) {
        int tvCountry = 0;
        if(mHisenseTvAPI != null){
            tvCountry = mHisenseTvAPI.getConfigValue(MtkTvConfigType.CFG_SYSTEM_COUNTRY);
        }
        Log.d(TAG, "tvCountry = " + tvCountry);
        String country;
        switch (tvCountry){
            case 59:
                country = "USA";
                break;
            case 60:
                country = "CAN";
                break;
            case 61:
                country = "MEX";
                break;
            default:
                country = "USA";
                break;
        }
        return country;
    }

    public static String retrieveLogo(HisenseTvAPI mHisenseTvAPI){
        int tvLogo = 0;
        String logo;
        if(mHisenseTvAPI != null){
            tvLogo = mHisenseTvAPI.getConfigValue(MtkTvConfigType.CFG_FACTORY_FAC_BOOTUP_LOGO);
        }
        Log.d(TAG, "tvLogo = " + tvLogo);
        switch (tvLogo){
            case 1:
                logo = "his";
                break;
            case 49:
                logo = "shp";
                break;
            default:
                logo = "his";
                break;
        }
        return logo;
    }

    public static String retrieveLan(HisenseTvAPI mHisenseTvAPI) {
        //int tvLan = 0;
        String language;
        String tvLan = "en";
        if(mHisenseTvAPI != null){
            //tvLan = mHisenseTvAPI.getConfigValue(MtkTvConfigType.CFG_SYSTEM_LANGUAGE);

        	//change to get language directly from locale
            tvLan = Locale.getDefault().getLanguage();
        }
        Log.d(TAG, "tvLan = " + tvLan);
//        switch(tvLan){
        String lan = "EN";
        if(mHisenseTvAPI != null){
//            tvLan = mHisenseTvAPI.getConfigValue(MtkTvConfigType.CFG_SYSTEM_LANGUAGE);
            lan = Locale.getDefault().getLanguage();
        }
        Log.d(TAG, "tvLan = " + tvLan);
//        switch(tvLan){
//            case 1:
//                language = "1";
//                break;
//            case 2:
//                language = "2";
//                break;
//            case 3:
//                language = "6";
//                break;
//            default:
//                language = "1";
//        }
        switch(lan){
            case "en":
                language = "1";
                break;
            case "fr":
                language = "2";
                break;
            case "es":
                language = "6";
                break;
            default:
                language = "1";
        }
        return language;
    }

//    private static int retrieveTimezone(HisenseTvAPI mHisenseTvAPI) {
//        Log.e(TAG, mHisenseTvAPI.getConfigValue(MtkTvConfigType.CFG_SYSTEM_TIMEZONE));
//        return 0;
//    }

    //this function is used to handle the communication with JHK operation server
    public static int getSwitch(HisenseTvAPI mHisenseTvAPI, String token){

        Log.d(TAG, "get parameters from device");
        String FEATURE_CODE = "ro.product.hitdeviceprefix";
        String CAPABILITY_CODE = "ro.product.capabilitycode";
        String ACR_SWITCH = "ro.hisense.acr.support.switch";

        String featureCode = SystemProperties.get(FEATURE_CODE);
        String capabilityCode = SystemProperties.get(CAPABILITY_CODE);
        String acrSwitch = SystemProperties.get(ACR_SWITCH);
        String country = retrieveCountry(mHisenseTvAPI);
        String logo    = retrieveLogo(mHisenseTvAPI);
        String lan     = retrieveLan(mHisenseTvAPI);

//        retrieveTimezone(mHisenseTvAPI);
        Log.d(TAG, "feature code: " + featureCode + ", capability code: " + capabilityCode + ", country: " + country + ", logo: " + logo + ", language: " + lan + ", acr switch:" + acrSwitch);
        if (acrSwitch.equals("off")){
            return 0;
        }

        String killSwitchUrl = "https://api-launcher-na.hismarttv.com/cgi-bin/launcher_index.fcgi?action=GetSwitchFlag&product_type=launcher&areacode=" + country + "&brandcode=" + logo + "&oemname=-1" + "&token=" + token + "&language_id=" + lan + "&tm_zone=-5" + "&tv_mode=Hicloud&version=5.0&os_version=0.0.0.0&os_type=1&format=0";
        int killSwitch = 0;
        if("".equals(token) || token == null){
            Log.e(TAG, "Launcher token is null, return");
            return 0;
        } else {
            String switchString = ServerRequestManager.getHttpsInputStream(killSwitchUrl);
            if("304".equals(switchString)){
                Log.d(TAG, "304 return");
                return 0;
            }
            if(switchString == null){
                Log.d(TAG, "launcher restful api returns null");
                return 0;
            }else if (switchString.contains("acrFlag")){
                Log.d(TAG, switchString);
                switchString = switchString.split("acrFlag>")[1];
                switchString = switchString.substring(0, 1);
                killSwitch = Integer.parseInt(switchString);
                Log.d(TAG, "kill switch = " + killSwitch);

            }else{
                Log.d(TAG, switchString);
                Log.d(TAG, "error when getting kill switch");
                return 0;
            }
        }
        return killSwitch;

    }

    public static void getMap(HisenseTvAPI mHisenseTvAPI, String token, Context context){
        Log.d(TAG, "get parameters from device");
        String FEATURE_CODE = "ro.product.hitdeviceprefix";
        String CAPABILITY_CODE = "ro.product.capabilitycode";

        String featureCode = SystemProperties.get(FEATURE_CODE);
        String capabilityCode = SystemProperties.get(CAPABILITY_CODE);
        String country = retrieveCountry(mHisenseTvAPI);
        String logo    = retrieveLogo(mHisenseTvAPI);
        String lan     = retrieveLan(mHisenseTvAPI);

        Log.d(TAG, "feature code: " + featureCode + ", capability code: " + capabilityCode + ", country: " + country + ", logo: " + logo + ", language: " + lan);

        String mapUrl = "https://api-4know-na.hismarttv.com/application/acr_mapping?token=" + token + "&region=" + country + "&brand=" + logo + "&feature_code=" + featureCode + "&capability=" + capabilityCode + "&language_id=" + lan + "&time_stamp=0&app_name=acr";

        if("".equals(token) || token == null){
            Log.e(TAG, "Launcher token is null, return");
            return;
        } else {
            String mapString = ServerRequestManager.getHttpsInputStream(mapUrl);
            if("304".equals(mapString)){
                Log.e(TAG, "304 return");
                return;
            }
            if(mapString == null){
                Log.e(TAG, "launcher restful api returns null");
                return;
            }else{
                Log.e(TAG, mapString);
                try {
                    parseMap(mapString, context);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void parseMap(String mapString, Context context) throws JSONException {
        JSONObject jsonObj = new JSONObject(mapString);
        DBManager dbManager = DBManager.getInstance(context);

        String aqObj = jsonObj.getString("aq").replace("{", "").replace("}", "");
        String pqObj = jsonObj.getJSONObject("pq").getString("sdr").replace("{", "").replace("}", "");
        String[] aqMapArray = aqObj.split(",\"");
        String[] pqMapArray = pqObj.split(",");

        String aqType;
        String aqGenre;
        String aqValue;

        String pqType;
        String pqValue;


        for(String pqString : pqMapArray){
            Log.e(TAG, pqString);
            pqType = pqString.split(":")[0].replace("\"", "");
            pqValue = pqString.split(":")[1].replace("\"", "");
            for(String aqString : aqMapArray){
                aqType = aqString.split(":")[0].split(",")[0].replace("\"", "");
                if(aqType.equals(pqType)) {
                    aqGenre = aqString.split(":")[0].split(",")[1].replace("\"", "");
                    aqValue = aqString.split(":")[1].replace("\"", "");

                    Log.e(TAG, "PQ: Type = " + pqType + " Value = " + pqValue);
                    Log.e(TAG, "AQ: Type = " + aqType + " Genre = " + aqGenre + " Value = " + aqValue);

                    dbManager.updatePAQ(aqType, aqGenre, Integer.valueOf(pqValue), Integer.valueOf(aqValue));
                }
            }

        }

    }
    public static String[] getProps(HisenseTvAPI mHisenseTvAPI){
        String BRAND_NAME = "ro.product.brand";
        String CHIP_NAME = "ro.product.board";
        String MODEL_NAME = "ro.product.hisense.model";


        String brandName = SystemProperties.get(BRAND_NAME, "Hisense");
        String chipName = SystemProperties.get(CHIP_NAME, "MT5660");
        String model = SystemProperties.get(MODEL_NAME, "HU65U7AG");
        String language = Locale.getDefault().getLanguage();
        String country = retrieveCountry(mHisenseTvAPI);
        String locale = Locale.getDefault().toString();
        String OS = "android";
        String OSVersion = Build.VERSION.RELEASE;
        String panelSize = model.substring(0,2);
//        Log.d(TAG, "brand name:" + brandName + ", chip name: " + chipName + ", model: " + model + ", language: " + language + ", country: " + country
//                + ", locale: " + locale + ", OS: " + OS + ", OSVersion: " + OSVersion + ", panel size: " + panelSize);

        return new String[] {brandName, chipName, OS, OSVersion, model, panelSize, language, locale, country};

    }

}