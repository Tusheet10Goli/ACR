package com.hisense.evservice;

import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class miBinderCallback extends Binder {
    private static final String TAG = "miBinderCallback";

    //this function is the callbacks function when received message from the C++ Process
    @Override
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        //Log.d(TAG, "miBinderCallback :=> onTransact code= " + code + " readInt= " + data.readInt());
        String receivedString = data.readString();
        Log.d(TAG, "miBinderCallback :=> onTransact code= " + code + " readString= " + receivedString);

        String replyString = new String("");

        try {
            replyString = msgInterface.JSONParser(receivedString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if(replyString != "" ) {
            Log.d(TAG, "miBinderCallback :=> onTransact code= " + code + " replyString= " + replyString);
            reply.writeString(replyString);//write string back to reply
        }
        //push it to message queue and return
//        Message message = Message.obtain();
//        message.obj = data.readString();
//        mMessageService.handler.sendMessage(message);
        // Call the parent method with the arguments passed in
        return true;
        //return super.onTransact(code, data, reply, flags);
    }
}
