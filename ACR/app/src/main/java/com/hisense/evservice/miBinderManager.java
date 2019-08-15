package com.hisense.evservice;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import android.os.ServiceManager;

import org.json.JSONException;

import static android.os.SystemClock.sleep;

public class miBinderManager {
    private static final String TAG = "miBinderManager";
    //private static final String TAG = "IBinderManager";
    private static final String SERVICE_NAME = "acr.apk2native";

    public static boolean BinderIsExist(){
        IBinder b = null;
        int i = 0;
        while(b == null && i < 5){
            b = ServiceManager.getService(SERVICE_NAME);//this name is configured in the c process
            sleep(5000*i);//5 -> 10 -> 15 -> 20 -> 25 seconds
            Log.v(TAG, "get binder attempt: " + i++ + ",b: " + b);
        }
        if(b != null) {
            Log.v(TAG, "get binder succeed, b: " + b);
            return true;
        }else{
            return false;
        }
    }

    public static boolean SendCallback2iBinder() {
        String replyString;

        Log.i(TAG, "miBinderManager :=> SendCallback2iBinder has been called");
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        IBinder b = null;
        if(BinderIsExist()) {
            b = ServiceManager.getService(SERVICE_NAME);//this name is configured in the c process
        }
        try {
            //_data.writeInterfaceToken("sample.hello");
            _data.writeStrongBinder(new miBinderCallback());
            b.transact(0, _data, _reply, 0);
            String input = _reply.readString();
            Log.i(TAG, "_reply.readString() " + input);
            if(input != null) {
                replyString = msgInterface.JSONParser(input);
                Log.d(TAG, "get the reply string when get hamservice bind to message receiver: " + replyString);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (JSONException e){
            e.printStackTrace();
        }
        finally {
            _reply.recycle();
            _data.recycle();
        }
        return true;
    }

    public static boolean SendMsg2iBinder(String messageString) {
        Log.i(TAG, "miBinderManager :=> SendMsg2iBinder => message string: " + messageString);
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        IBinder b = null;
        String replyString = null;
        if(BinderIsExist()) {
            b = ServiceManager.getService(SERVICE_NAME);//this name is configured in the c process
        }
        Log.v(TAG, "get binder succeed, b: " + b);
        try {
            //_data.writeInterfaceToken("sample.hello");
            //_data.writeStrongBinder(new miBinderCallback());
            _data.writeString(messageString);
            b.transact(1, _data, _reply, 0);

            String input = _reply.readString();
            Log.i(TAG, "_reply.readString() " + input);
            if(input != null) {
                replyString = msgInterface.JSONParser(input);
                Log.d(TAG, "the reply string to the msg sent is: " + replyString);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch(JSONException e){
            e.printStackTrace();
        } finally {
            _reply.recycle();
            _data.recycle();
        }
        return true;
    }
}
