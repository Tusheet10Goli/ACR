package com.hisense.evservice;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mediatek.twoworlds.tv.HisenseTvAPI;
import com.mediatek.twoworlds.tv.common.MtkTvConfigType;

public class EVConsentActivity extends FragmentActivity {
    private static final String TAG = "EVConsentActivity";

    private DBManager dbManager;

    public boolean isConnected(Context context){
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnected();
        return isConnected;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        int useMode = Settings.Global.getInt(this.getContentResolver(), "use_mode", 0);
        Log.e(TAG, "usage mode is " + useMode);
        dbManager = DBManager.getInstance(this);
        if(!isConnected(this)){
            Toast.makeText(this, "No internet. Please start the service later.", Toast.LENGTH_SHORT).show();
            setResult(0);
            finish();
            return;
        } else if (dbManager.checkACRStatus()[0] == 1) {
            finish();
            return;
        }

        setContentView(R.layout.activity_ev_consent);
        EVIntroFragment intro = new EVIntroFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        fragmentTransaction.add(R.id.fragment_container, intro);
        fragmentTransaction.commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "now started");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.v(TAG, "restart from stop");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "resumed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "paused");
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "start to stop");
        super.onStop();
        Log.v(TAG, "stop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "destroy");
    }

    @Override
    public void onBackPressed(){
        setResult(0);
        finish();
        return;
    }

    public static class EVIntroFragment extends Fragment {
        private static final String TAG = "EVIntroFragment";
        private Button consent, unconsent;
        private DBManager dbManager;
        private TextView termText;
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState){
            View view = inflater.inflate(R.layout.fragment_evintro, container, false);
            consent = view.findViewById(R.id.consent);
            unconsent = view.findViewById(R.id.unconsent);
            termText = view.findViewById(R.id.termlinks);

            HisenseTvAPI mHisenseTvAPI = HisenseTvAPI.getInstance(getContext());
            int country = mHisenseTvAPI.getTvCountry(MtkTvConfigType.CFG_SYSTEM_COUNTRY);

            if (country == 59) {
                termText.setText(R.string.evopt_intro2_usa);
            } else {
                termText.setText(R.string.evopt_intro2_ca);
            }
            dbManager = DBManager.getInstance(getActivity().getApplicationContext());
            consent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    consentHandler(v);
                }
            });
            consent.setOnFocusChangeListener(new View.OnFocusChangeListener(){

                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    String tempString = getString(R.string.evopt_consent);
                    Button button= (Button)v.findViewById(R.id.consent);
                    SpannableString spanString = new SpannableString(tempString);
                    if(hasFocus){
                        spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);
                        button.setText(spanString);
                    } else {
                        spanString.setSpan(new StyleSpan(Typeface.NORMAL), 0, spanString.length(), 0);
                        button.setText(spanString);
                    }
                }
            });
            unconsent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    notConsentHandler(v);
                }
            });
            unconsent.setOnFocusChangeListener(new View.OnFocusChangeListener(){

                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    String tempString = getString(R.string.evopt_unconsent);
                    Button button = (Button)v.findViewById(R.id.unconsent);
                    SpannableString spanString = new SpannableString(tempString);
                    if(hasFocus){
                        spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);
                        button.setText(spanString);
                    } else {
                        spanString.setSpan(new StyleSpan(Typeface.NORMAL), 0, spanString.length(), 0);
                        button.setText(spanString);
                    }
                }
            });
            return view;
        }

        public void consentHandler(View v){

            EVFuncFragment func = new EVFuncFragment();
            FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            fragmentTransaction.replace(R.id.fragment_container, func);
            fragmentTransaction.addToBackStack("intro");
            fragmentTransaction.commit();
        }

        public void notConsentHandler(View v){
            dbManager.updateACR("g_system__acr_status",0);
            getActivity().setResult(0);
            getActivity().finish();
            return;
        }
    }

    public static class EVFuncFragment extends Fragment {
        private static final String TAG = "EVFuncFragment";
        private Button cancel, active;
        private Context context;
        private DBManager dbManager;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState){
            View view = inflater.inflate(R.layout.fragment_evfunc, container, false);

            cancel = view.findViewById(R.id.unconsent);
            active = view.findViewById(R.id.consent);
            context = getActivity().getApplicationContext();
            dbManager = DBManager.getInstance(context);
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cancelHandler(v);
                }
            });

            cancel.setOnFocusChangeListener(new View.OnFocusChangeListener(){
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    String tempString = getString(R.string.evset_unconsent);
                    Button button = v.findViewById(R.id.unconsent);
                    SpannableString spanString = new SpannableString(tempString);
                    if(hasFocus){
                        spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);
                        button.setText(spanString);
                    } else {
                        spanString.setSpan(new StyleSpan(Typeface.NORMAL), 0, spanString.length(), 0);
                        button.setText(spanString);
                    }
                }
            });
            active.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activeHandler(v);
                }
            });
            active.setOnFocusChangeListener(new View.OnFocusChangeListener(){
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    String tempString = getString(R.string.evset_consent);
                    Button button = v.findViewById(R.id.consent);
                    SpannableString spanString = new SpannableString(tempString);
                    if(hasFocus){
                        spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);
                        button.setText(spanString);
                    } else {
                        spanString.setSpan(new StyleSpan(Typeface.NORMAL), 0, spanString.length(), 0);
                        button.setText(spanString);
                    }
                }
            });
            return view;
        }

        public void cancelHandler(View v){
            getActivity().getSupportFragmentManager().popBackStack();
        }

        public void activeHandler(View v) {
            dbManager.updateACR("g_system__acr_status", 1);
            dbManager.updateACR("consent_date", 1);
            dbManager.updateFTE();                                                                  // add this because user will clear app data manually and erase the fte status
            startACR();
            getActivity().setResult(1);
            getActivity().finish();
        }
        public void startACR(){
            //todo: check the hisense cloud and other settings
//            Toast.makeText(context, "start the service", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "created the service ");
            Intent service = new Intent(context, MService.class);
            context.startService(service);
        }

    }
}
