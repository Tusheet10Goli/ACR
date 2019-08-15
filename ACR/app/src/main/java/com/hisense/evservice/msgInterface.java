package com.hisense.evservice;

/* ***********************
*  This class is designed as a static class.
*  The member functions can be called to:
*    1. generate message and write to binder
*    2. generate message and push it to internal message queue
* ************************/

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mediatek.twoworlds.tv.HisenseTvAPI;

import static android.os.SystemClock.sleep;

public class msgInterface {
    private static final String TAG = "msgInterface";
    private static Context msgInterfaceCnt;
//    private static boolean optFlag = false;
    public static boolean initStatus = false; //this is use to check if the hamservice is init
    private static String genreBuf = "";
    private static String showTypeBuf = "";
    private static String genreString = "";
    public static boolean hsOptin = false;
    public static boolean audioCollect = false;


    //this is the function open to public checking acknowledged opt-in status
    public static boolean checkInitStatus() {
        return initStatus;
    }

    public static void setContext(Context context){
        msgInterface.msgInterfaceCnt = context;
    }

    public static Context getContext(){
        return msgInterfaceCnt;
    }
    //this function is used to parse JSON string and dispatch to specific case
    //all message received from the C++ process is parsed and handled here
    public static String JSONParser(String input) throws JSONException {
        JSONObject jsonObj = new JSONObject(input);
        DBManager dbManager = DBManager.getInstance(getContext());
        String type= jsonObj.getString("type");
        String replyString = new String("");
        String dataString = new String("");
        String whiteListString = new String("");
        JSONObject dataObject;
        switch(type){
            case "init_ack":
                initStatus = true;
                Log.d(TAG, "init finished");
                break;
            case "opt_in_ack":
                String status = jsonObj.getString("status");
                String result = jsonObj.getString("enable");

                if(status.equals("succeed")){
                    if(result.equals("true")) {
                        hsOptin = true;
                        audioCollect = true;
                    }else if(result.equals("false")){
                        hsOptin = false;
                        audioCollect = false;
                    }
                }else if(status.equals("failed")){
                    Log.e(TAG, "opt in " + result + " failed! Try again 3 seconds later!");
                    sleep(3000);
                    String opt = strOptIn(result.equals("true"));
                    miBinderManager.SendMsg2iBinder(opt);
                }else if(status.equals("overtime")){
                    Log.v(TAG, "opt-in/out overtime, will retry after 10 seconds!");
                    sleep(10000);
                    String opt_inStr = strOptIn(hsOptin);//it always send the most updated data from user
                    miBinderManager.SendMsg2iBinder(opt_inStr);
                    Log.v(TAG, "going to send opt in message: " + opt_inStr);
                }else{
                    Log.v(TAG, "opt-in unhandled status!");
                }
                //Hdler_opt_in_ack(enabled);
                break;
            case "start_ack":
                String audio_start_status = jsonObj.getString("status");
                if(audio_start_status.equals("succeed")) {
                    audioCollect = true;
                } else {
                    Log.d(TAG, "start audio collection failed");
                }
                break;
            case "stop_ack":
                String audio_stop_status = jsonObj.getString("status");
                if(audio_stop_status.equals("succeed")) {
                audioCollect = false;
            } else {
                Log.d(TAG, "stop audio collection failed");
            }
                break;
            case "reset_adid_ack":
                String adid = jsonObj.getString("enabled");
                Hdler_reset_adid_ack(adid);
                break;
            case "recognition":
                //TODO: probably need to pass in a JSON object, handler need to be implemented here
                //need to confirm the data format

                Log.v(TAG, input);
                dataObject = jsonObj.getJSONObject("data");
                String showType = dataObject.getString("showType");
                Log.v(TAG, "showType: " + showType);
                JSONArray genreArray = dataObject.getJSONArray("genre");
                if(genreArray != null && genreArray.length()!= 0){
                    genreString = genreArray.getString(0);//@TODO: find genre message here!!
                    Log.v(TAG, genreString);
                } else {
                    Log.v(TAG, "no genre");
                    genreString = " ";
                }


                if(!showType.equals(showTypeBuf) || !genreString.equals(genreBuf)){//only show on different recognition received
                    //String title = dataObject.getString("title");
                    int[] paq = dbManager.getPAQ(showType, genreString);
                    if(UIMessageHandlerThread.isAutoPQ) {
                        sysInterface.setPQmode(paq[0]);
                        Log.v(TAG, "new show type recognized! => " + showType + ", PQ changed to: " + paq[0]);
                    }
                    if(UIMessageHandlerThread.isAutoAQ) {
                        sysInterface.setAQmode(paq[1]);
                        Log.v(TAG, "new show type recognized! => " + showType + ", AQ changed to: " + paq[1]);
                    }
                    Log.v(TAG, "new show type recognized! => " + showType + ", PQ changed to: " + paq[0] + ", AQ changed to: " + paq[1]);

                    if(UIMessageHandlerThread.isAutoAQ || UIMessageHandlerThread.isAutoPQ) {
                        Intent broadcast = new Intent();
                        broadcast.putExtra("genre", showType);
                        broadcast.setAction("com.hisense.evservice.TEST");
                        broadcast.setComponent(new ComponentName("com.hisense.evservice", "com.hisense.evservice.MBroadcastReceiver"));
                        getContext().sendBroadcast(broadcast);
                    }
                    showTypeBuf = showType;
                    genreBuf = genreString;
                }
                break;
            case "recommendation":
                //TODO: probably need to pass in a JSON object, handler need to be implemented here
//                dataString = jsonObj.getString("data");
                Log.v(TAG, input);
                whiteListString = jsonObj.getString("config");
                Log.d(TAG, "white list string: " + whiteListString);
                JSONObject whitelistObj = new JSONObject(whiteListString);
                if(whiteListString != null) {
                    JSONArray whiteList = whitelistObj.getJSONArray("app_configs");
                    for(int i = 0; i < whiteList.length(); i++){
                        JSONObject app_config = whiteList.getJSONObject(i);
                        String pkg = app_config.getString("name");
                        String acr = app_config.getString("acr");
//                        String popularity = app_config.getString("popularity");
//                        String recommendation = app_config.getString("recommendation");
//                        String trending = app_config.getString("trending");

                        dbManager.updateAppConfig(pkg, acr);
                    }
                }

//                dataObject = new JSONObject(dataString);
//                JSONArray recommendArray = dataObject.getJSONArray("recommendations");
//                for(int i = 0; i < recommendArray.length(); i++){
//                    String[] attributes = new String[4];//"title", "description", "thumbnail_url", "link_url"
////                    String contentStr = recommendArray[i].getJSONObject("data").toString();
//                    JSONObject a_recommend = recommendArray.getJSONObject(i);
////                    JSONObject contentObj = new JSONObject(contentStr);
//                    attributes[0] = a_recommend.getString("title");
//                    attributes[1] = a_recommend.getString("description");
//                    attributes[2] = a_recommend.getString("url");
//                    attributes[3] = a_recommend.has("link_url")? a_recommend.getString("link_url") : "";
//
////                    dbManager.insertProgram(attributes);
//                    Log.v(TAG, "Recommendation pushed to UI =>: " + attributes[0] + ", " + attributes[1] + ", " + attributes[2] + ", " + attributes[3] + ".");
//                }
                break;
            case "appconfig":
                Log.d(TAG, "white list message received");
                if(jsonObj.getString("status").equals("succeed")){
                    dataObject = jsonObj.getJSONObject("data");
                    Log.d(TAG, "data = " + dataObject.toString());
                    if(dataObject != null){
                    	
                    	//implementation ..
                    	// take the default acr value from JSON and save it into database
                    	JSONObject defaultJSON = dataObject.getJSONObject("defaults");
                    	//manually created string for storing the default value
                    	String pkgDefaultValue = "defaults";
                    	String defaultAcrStr = defaultJSON.getString("acr");
                    	dbManager.updateAppConfig(pkgDefaultValue, defaultAcrStr);
                    	//..
                    	
                        JSONArray whiteList = dataObject.getJSONArray("app_configs");
                        for(int i = 0; i < whiteList.length(); i++) {
                            JSONObject app_config = whiteList.getJSONObject(i);
                            String pkg = app_config.getString("name");
                            String acr = app_config.getString("acr");
                            dbManager.updateAppConfig(pkg, acr);
                        }
                    }
                }else{
                    Log.d(TAG, "get the white list later");
                }
                break;

            case "heartbeat":
                //TODO: handler need to be implemented here
                replyString = "{\"type\":\"heartbeat_ack\"}";
                break;
//            case "whilte_list":
//                JSONArray appList = jsonObj.getJSONArray("app_list");
//                //TODO:handler need to be implemented here
//                //need to confirm the data format
//                break;
            default:
                Log.e(TAG, "JSONParser => unrecognized message type");
                return "";
                //need to confirm the data format
        }
        return replyString;
    }

    //TODO:handler need to be implemented here
    private static void Hdler_opt_in_ack(boolean enable){
        Log.i(TAG, "Hdler_opt_in_ack => " + enable);
    }

    //TODO:handler need to be implemented here
    private static void Hdler_reset_adid_ack(String adid){
        Log.i(TAG, "Hdler_reset_adid_ack => " + adid);
    }

    //TODO:handler need to be implemented here
    private static void Hdler_heartbeat(String timestamp){
        Log.i(TAG, "Hdler_reset_adid_ack => " + timestamp);
        //check if it is expired and send message back
    }

    //this function is used to generate string for strOptIn
    public static String strOptIn(boolean opt){
//        optFlag = opt;
        String jsonString = null;
        try {
            jsonString = new JSONObject()
                    .put("type", "opt_in")
                    .put("enabled", opt)
                    .toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonString;
    }

    //this function is used to generate string for STR on/off message
//    public static String strSTR(boolean opt){
//        String jsonString = null;
//        if(opt == true) {//wake up from STR
//            try {
//                jsonString = new JSONObject()
//                        .put("type", "str_on")
//                        .toString();
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//        }else {//turn off to STR
//            try {
//                jsonString = new JSONObject()
//                        .put("type", "str_off")
//                        .toString();
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//        }
//
//        return jsonString;
//    }

    //this function is used to generate string for strResetAdId
    public static String strResetAdId(){
        String jsonString = null;
        try {
            jsonString = new JSONObject()
                    .put("type", "reset_adid")
                    .toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonString;
    }

    //this function is used to generate string for strSystemInfo
    public static String strSystemInfo(String infoType, String infoValue) throws JSONException {
        //Long tsLong = System.currentTimeMillis()/1000;
        //String ts = tsLong.toString();
        String jsonString = new JSONObject()
                .put("type", "sys_info")
                .put(infoType, infoValue)
                //.put("timestamp", ts)//TODO:is that necessary?
                .toString();
        return jsonString;
    }

    //this function is used to generate string for init call
    public static String strInit(String[] pros) {

        String jsonString  = null;
        try{
            jsonString = new JSONObject()
                    .put("type", "init")
                    .put("mBrand", pros[0])
                    .put("mChipset", pros[1])
                    .put("mOS", pros[2])
                    .put("mOsVersion", pros[3])
                    .put("mModel", pros[4])
                    .put("mPanelSize", pros[5])
                    .put("mLanguage", pros[6])
                    .put("mLocale", pros[7])
                    .put("mHost", "1.1.1.1")
//                    .put("mZipcode", "00000")
                    .put("mCountry", pros[8]).toString();
        }catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonString;
    }

    public static String strUpdateParams(String language, String country, String locale){

        String jsonString = null;
        Log.d(TAG, "make the parameter jsonString");
        try {
           jsonString =  new JSONObject()
                    .put("type", "sys_info")
                    .put("mLanguage", language)
//                    .put("mCountry", country)
                    .put("mLocale", locale)
                    .toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "finish the parameter json string");
        return jsonString;
    }

    public static String strUpdateSource(String pkg){

        String jsonString = null;
        Log.d(TAG, "make the source jsonString");
        try {
            jsonString =  new JSONObject()
                    .put("type", "sys_info")
                    .put("mProvider", pkg)
                    .toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "finish the source json string");
        return jsonString;
    }

    public static String strAudioStart(boolean swt){
        String jsonString = null;
        String  value = swt ? "start" : "stop";
        Log.d(TAG, "make the " + value + " collect the audio jsonString");
        try {
            jsonString =  new JSONObject()
                    .put("type", value)
                    .toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "finish " + value + " the audio collection json string");
        return jsonString;
    }


    //this function is used to parse whitelist message
//    public boolean parseWhiteList(String input){
//        JSONObject whiteList = new JSONObject(input);
//        JSONObject defaultConfigs = whiteList.getJSONObject("defaults");
//        pushAppConfig(defaultConfigs);
//        JSONArray appsConfigs = whiteList.getJSONArray("app_configs");
//
//        JSONObject temp;
//        for (int i = 0; i < appsConfigs.length(); i++) {
//            temp = appsConfigs.getJSONObject(i);
//            pushAppConfig(temp);
//        }
//    }
//
//    //this is used to push each app config in whitelist
//    private void pushAppConfig(JSONObject appConfig){
//        //example:"defaults": {
//        //    "acr": true,
//        //    "popularity": true,
//        //    "recommendation": true,
//        //    "trending": true
//        //  }
//
//        //TODO: add into local map
//        //pushToMap(appConfig.acr, appConfig.popularity, appConfig.recommendation, appConfig.trending);
//        Log.v(TAG, "pushAppConfig :=> " + appConfig);//.acr + appConfig.popularity + appConfig.recommendation + appConfig.trending);
//    }
}
