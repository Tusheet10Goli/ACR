package com.hisense.evservice;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.hisense.hitv.hicloud.bean.account.AppCodeReply;
import com.hisense.hitv.hicloud.bean.account.SignonReplyInfo;
import com.hisense.hitv.hicloud.bean.global.HiSDKInfo;
import com.hisense.hitv.hicloud.factory.HiCloudServiceFactory;
import com.hisense.hitv.hicloud.service.HiCloudAccountService;
import com.hisense.hitv.hicloud.util.DeviceConfig;
import com.hisense.hitv.hicloud.util.SDKUtil;

import java.lang.reflect.Method;

public class HiCloudTokenManager {
    private static final String TAG = "HiCloudTokenManager";

    private static final String LAUNCHER_APPKEY = "1175287431";
    private static final String LAUNCHER_APPSECRET = "15k5edu06f1xwf3qat4nwy3m8xg7v3oe";
    private static final String FKNOW_APPKEY = "1178487606";
    private static final String FKNOW_APPSECRET = "vq6ulduyjw4ogig6win614dx6p0i7yhh";
    private static final String DEFAULT_DOMAIN_NAME = "na.hismarttv.com";
    private static final String PREFIX_DOMAIN_AUTH = "auth-";
    private static final String PREFIX_FKNOW_DOMAIN_AUTH = "auth-4know-";
    private static final String PROPERTY_DOMAIN_NAME = "ro.domain.name";

    private static HiCloudTokenManager mInstance = null;

    private Context mContext;
    private DBManager dbManager;
    private HiCloudAccountService mHiCloudService = null;

    public static HiCloudTokenManager getmInstance(Context context){
        if(mInstance == null){
            mInstance = new HiCloudTokenManager(context);
        }
        return mInstance;
    }

    public HiCloudTokenManager(Context context) {
        this.mContext = context;
        dbManager = DBManager.getInstance(context);
    }

    public String getAppToken(){
        HiSDKInfo info = new HiSDKInfo();
        info.setDomainName(getDomainName(PREFIX_DOMAIN_AUTH));
        info.setLanguageId(SDKUtil.getLocaleLanguage());
        if(mHiCloudService == null){
            mHiCloudService = HiCloudServiceFactory.getHiCloudAccountService(info);
        }
        do{
            String deviceId = DeviceConfig.getDeviceId(mContext);
            Log.d(TAG, "getToken -> deviceId = " + deviceId);
            if(TextUtils.isEmpty(deviceId)){
                Log.e(TAG, "cannot get token, deviceId is empty");
                break;
            }

            AppCodeReply codeReply = ((mHiCloudService != null) ? mHiCloudService.appAuth(LAUNCHER_APPKEY, LAUNCHER_APPSECRET) : null);

            if(codeReply == null) {
                Log.e(TAG, "cannot get token, app auth failed, code reply is null");
                break;
            } else {
                int reply = codeReply.getReply();
                if(reply != 0){
                    Log.e(TAG, "cannot get token, reply is not 0. it is:" + reply);
                    break;
                } else {
                    Log.d(TAG, "reply code:" + codeReply.getCode());
                    SignonReplyInfo replyInfo = mHiCloudService.signon(codeReply.getCode(), deviceId, "" , "");
                    if(replyInfo != null) {
                        String token = replyInfo.getToken();
                        long createTime = replyInfo.getTokenCreateTime();
                        int expireTime = replyInfo.getTokenExpireTime();
                        Log.d(TAG, "token = " + token);
                        Log.d(TAG, "create time = " + createTime);
                        Log.d(TAG, "expire time = " + expireTime);
                        dbManager.updateToken(1, token, createTime, expireTime);
                        return token;
                    }else{
                        Log.e(TAG, "cannot get token, result  is null");
                        break;
                    }
                }
            }
        }while(false);
        return null;
    }

    public String get4KNowToken(){
        HiSDKInfo info = new HiSDKInfo();
        info.setDomainName(getDomainName(PREFIX_FKNOW_DOMAIN_AUTH));
        info.setLanguageId(SDKUtil.getLocaleLanguage());
        if(mHiCloudService == null){
            mHiCloudService = HiCloudServiceFactory.getHiCloudAccountService(info);
        }
        do{
            String deviceId = DeviceConfig.getDeviceId(mContext);
            Log.d(TAG, "getToken -> deviceId = " + deviceId);
            if(TextUtils.isEmpty(deviceId)){
                Log.e(TAG, "cannot get token, deviceId is empty");
                break;
            }

            AppCodeReply codeReply = ((mHiCloudService != null) ? mHiCloudService.appAuth(FKNOW_APPKEY, FKNOW_APPSECRET) : null);

            if(codeReply == null) {
                Log.e(TAG, "cannot get token, app auth failed, code reply is null");
                break;
            } else {
                int reply = codeReply.getReply();
                if(reply != 0){
                    Log.e(TAG, "cannot get token, reply is not 0. it is:" + reply);
                    break;
                } else {
                    Log.d(TAG, "reply code:" + codeReply.getCode());
                    SignonReplyInfo replyInfo = mHiCloudService.signon(codeReply.getCode(), deviceId, "" , "");
                    if(replyInfo != null) {
                        String token = replyInfo.getToken();
                        long createTime = replyInfo.getTokenCreateTime();
                        int expireTime = replyInfo.getTokenExpireTime();
                        Log.d(TAG, "token = " + token);
                        Log.d(TAG, "create time = " + createTime);
                        Log.d(TAG, "expire time = " + expireTime);
                        if(token != null) {
                            dbManager.updateToken(2, token, createTime, expireTime);
                            return token;
                        } else {
                            Log.e(TAG, "token returns null");
                            break;
                        }
                    }else{
                        Log.e(TAG, "cannot get token, result is null");
                        break;
                    }
                }
            }
        }while(false);
        return null;
    }

    private String getDomainName(String auth){
        String domain_name = getProp(PROPERTY_DOMAIN_NAME);
        if(TextUtils.isEmpty(domain_name)) {
            Log.e(TAG, "cannot get domain name from property.use default domain");
            domain_name = DEFAULT_DOMAIN_NAME;
        }
        domain_name = auth + domain_name;
        Log.i(TAG, "domain_name: " + domain_name);
        return domain_name;
    }

    private static final String getProp(String key){
        try{
            Method method = Class.forName("android.os.SystemProperties").getMethod("get", String.class);
            return (String)method.invoke(null, key);
        } catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }
}
