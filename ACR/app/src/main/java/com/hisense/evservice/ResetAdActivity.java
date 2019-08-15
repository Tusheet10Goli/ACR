package com.hisense.evservice;

import android.os.Bundle;
import android.app.Activity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class ResetAdActivity extends Activity {

    private static final String TAG = "ResetAdActivity";
    private Button activate, cancel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_ad);

        activate = findViewById(R.id.ad_active);
        cancel = findViewById(R.id.ad_cancel);
        Log.e(TAG, "get buttons and focus");

        activate.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                activeHandler(v);
            }


        });

        activate.setOnFocusChangeListener(new View.OnFocusChangeListener(){

                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    Button button= v.findViewById(R.id.ad_active);
                    Log.e(TAG, "set focus change function");
                    if(hasFocus){
                        button.setTextColor(ContextCompat.getColor(getApplication(), R.color.my_text_selected));
                        button.setBackgroundColor(ContextCompat.getColor(getApplication(),R.color.my_button_selected));
                    } else {
                        button.setTextColor(ContextCompat.getColor(getApplication(), R.color.my_text_default));
                        button.setBackgroundColor(ContextCompat.getColor(getApplication(),R.color.my_background_dark));
                    }
                }
            });

        cancel.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                cancelHandler(v);
            }
        });

        cancel.setOnFocusChangeListener(new View.OnFocusChangeListener(){

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Button button= v.findViewById(R.id.ad_cancel);
                if(hasFocus){
                    button.setTextColor(ContextCompat.getColor(getApplication(), R.color.my_text_selected));
                    button.setBackgroundColor(ContextCompat.getColor(getApplication(),R.color.my_button_selected));
                } else {
                    button.setTextColor(ContextCompat.getColor(getApplication(), R.color.my_text_default));
                    button.setBackgroundColor(ContextCompat.getColor(getApplication(),R.color.my_background_dark));
                }
            }
        });

        cancel.requestFocus();
    }


    @Override
    public void onBackPressed(){
        finish();
    }


    public void activeHandler(View v) {
        Log.e(TAG, "activate pressed");
        String reset_ad_string = msgInterface.strResetAdId();
        if(miBinderManager.BinderIsExist()) {
            miBinderManager.SendMsg2iBinder(reset_ad_string);
            Log.v(TAG, "handleMessage :=> reset ad id message send: " + reset_ad_string);
        }
        finish();
    }

    public void cancelHandler(View v) {
        Log.e(TAG, "cancel pressed");
        finish();
    }
}
