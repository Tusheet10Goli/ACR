package com.hisense.evservice;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.mediatek.twoworlds.tv.HisenseTvAPI;
import com.mediatek.twoworlds.tv.HisenseTvAPIBase;
import com.mediatek.twoworlds.tv.common.MtkTvConfigType;

import org.json.JSONException;

import static java.lang.Thread.sleep;


public class mMessageService extends Service{
    private static final String TAG = "mMessageService";
    private mMessageHandler hacr_msgQ;
    private HisenseTvAPI mHisenseTvAPI = null;
    private DBManager dbManager;
    private STRReceiver strReceiver;

    public static Handler handler = new Handler(Looper.getMainLooper()) {//TODO: have to be main looper?
        @Override
        public void handleMessage(Message msg) {
            // here is where we process the message
            //super.handleMessage(msg);

            Log.i(TAG, "handleMessage => " + (String) msg.obj);
            try {
                msgInterface.JSONParser((String) msg.obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate => ");
        dbManager = DBManager.getInstance(this);

        strReceiver = new STRReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.hisense.STR_RESUME");
        filter.addAction("com.hisense.STR_SUSPEND");
        registerReceiver(strReceiver,filter);
        mHisenseTvAPI =  HisenseTvAPI.getInstance(this);
        msgInterface.setContext(getApplicationContext());
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public int onStartCommand(Intent intent, int flag, int startId){
        Log.i(TAG, "onStartCommand => ");

        hacr_msgQ = new mMessageHandler();
        hacr_msgQ.execute(() -> {
            Log.i(TAG, "binder retrieved :=>: " + miBinderManager.BinderIsExist());
            miBinderManager.SendCallback2iBinder();

            if(dbManager.checkACRStatus()[0] == 1) {

                if(!msgInterface.checkInitStatus()) {
                    //确认是否要将所有信息上传SDK
                    String [] props = sysInterface.getProps(mHisenseTvAPI);
                    String init_message = msgInterface.strInit(props);

                    Log.v(TAG, "going to send init message: " + init_message);
                    if(init_message != null) {
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
                if(!dbManager.getAppConfig("com.google.android.youtube.tv")){
                    dbManager.updateAppConfig("com.google.android.youtube.tv", "true");
                }
            }
        });
        return START_STICKY;
    }

}
