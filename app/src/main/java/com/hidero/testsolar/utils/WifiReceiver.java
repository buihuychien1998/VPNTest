package com.hidero.testsolar.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import com.hidero.testsolar.MainActivity;

import java.util.Calendar;

public class WifiReceiver extends BroadcastReceiver {
    private final String TAG = "WifiReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        MainActivity activity = (MainActivity) context;
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {

            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();

            if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI &&
                    networkInfo.isConnected()) {
                // Wifi is connected
                WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                long connectedTime = System.currentTimeMillis();
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(connectedTime);
                Intent local = new Intent();
                local.setAction("service.to.activity.transfer");
                local.putExtra("connectedTime", connectedTime);
                context.sendBroadcast(local);
                Log.e(TAG, " -- Wifi connected --- " + " SSID " + connectedTime);

            }
        } else if (intent.getAction().equalsIgnoreCase(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
            if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
                Log.e(TAG, " ----- Wifi  Disconnected ----- ");

            }

        }
        activity.checkConnectedWifi();

    }

}