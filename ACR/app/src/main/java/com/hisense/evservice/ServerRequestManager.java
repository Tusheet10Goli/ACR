package com.hisense.evservice;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;

public class ServerRequestManager {
    private static final String TAG = "ServerRequestManager";

    public static String getHttpsInputStream(String httpsUrl){
        Log.d(TAG, "request url = " + httpsUrl);
        URL url = null;
        SSLContext sslContext = null;
        HttpsURLConnection connection;
        InputStream inputStream = null;
        try {
            url = new URL(httpsUrl);
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{ new X509ExtendedTrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {

                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {

                }

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {

                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {

                }

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }

            }, null);
        }catch (Exception e) {
            e.printStackTrace();
        }

        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
        int tryCount = 3;
        int response_code = 0;
        while(tryCount > 0) {
            try {
                connection = (HttpsURLConnection)url.openConnection();
                Log.i(TAG, "after open connection");
                connection.setConnectTimeout(1000);
                connection.setDoInput(true);
                connection.setDoOutput(false);
                connection.setRequestMethod("GET");
                connection.connect();

                Log.i(TAG, "after connection.connect()");
                inputStream = connection.getInputStream();
                response_code = connection.getResponseCode();
                break;
            } catch (SocketException e) {
                Log.e(TAG, "connect server timeout, ", e);
                tryCount--;
                if(tryCount  > 0) {
                    Log.i(TAG, "try connect again, remain count " + tryCount);
                }
            } catch(Exception e){
                Log.e(TAG, "getHttpsInputStream exception: " + e.getMessage(), e);
                break;
            }
        }
        Log.i(TAG, "get https input complete");
        if(response_code == 304){
            Log.d(TAG, "http response status is 304.");
            return "304";
        }
        BufferedReader reader;
        if(inputStream != null){
            reader = new BufferedReader(new InputStreamReader(inputStream));
        }else{
            Log.e(TAG, "inputStream is null, return null");
            return null;
        }
        StringBuilder builder = new StringBuilder();
        String line;
        try {
            while((line = reader.readLine()) != null){
                builder.append(line);
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException", e);
            e.printStackTrace();
        } finally {
            try{
                inputStream.close();
            }catch(IOException e){
                e.printStackTrace();
            }
        }
        Log.d(TAG, "result : " + builder.toString());
        return builder.toString();
    }
}
