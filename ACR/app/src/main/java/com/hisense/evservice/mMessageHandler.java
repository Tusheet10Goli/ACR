package com.hisense.evservice;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import org.json.JSONException;

//public class mMessageHandler extends Handler {
//    private msgInterface msgHandler = new msgInterface();
//
//    public mMessageHandler(Looper looper) {
//        super(looper);
//    }
//
//    @Override
//    public void handleMessage(Message msg) {
//        // TODO Auto-generated method stub
//        //super.handleMessage(msg);
//
//        try {
//            msgHandler.JSONParser((String) msg.obj);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//    }
//}

public class mMessageHandler extends HandlerThread {

    private Handler handler;

    private static final String TAG = "mMessageHandler";

    public mMessageHandler() {
        super(TAG);
        start();
        handler = new Handler(getLooper());
    }

    public mMessageHandler execute(Runnable task) {
        handler.post(task);//Posts a message to an object that implements Runnable.
        return this;
    }

}