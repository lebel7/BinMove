package com.proper.servicetasks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import com.proper.binmove.R;
import com.proper.utils.WifiSignalLevelSorter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Lebel on 13/08/2014.
 */
public class WifiReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        final ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        final Resources res = context.getResources();
        final ArrayList<ScanResult> connections = new ArrayList<ScanResult>();
        final List<ScanResult>  wifiResults = wifi.getScanResults();
        final WifiSignalLevelSorter sorter = new WifiSignalLevelSorter();
        WifiInfo info = null;

        //Layout our accepted parameters
        String[] acceptedParam = {res.getString(R.string.ENDPOINT_THE2S), res.getString(R.string.ENDPOINT_AMAZONDISPATCH),
                res.getString(R.string.ENDPOINT_BACKSTOCK8)};

        if (wifiResults != null && !wifiResults.isEmpty()) {
            for (int i = 0; i < wifiResults.size(); i++) {

                //if the current info == our authorized list of endpoint then continue
                if (!(Arrays.binarySearch(acceptedParam, wifiResults.get(i).BSSID) == -1)) {
                    connections.add(wifiResults.get(i));
                }
            }
        }

        //Sort our list based on signal strength in ascending order
        Collections.sort(wifiResults, sorter);

        //Get current wifi info
        if (networkInfo.isConnected()) info = wifi.getConnectionInfo();
        //loop through results, if instance.BSSID != info.getBSSID
        if (wifiResults != null && !wifiResults.isEmpty()) {
            for (int x = wifiResults.size(); x >= 1; x--) {
                if (info != null && !info.getBSSID().isEmpty()) {
                    //if the current info == our accepted list of endpoint then continue
                    if (!(Arrays.binarySearch(acceptedParam, info.getBSSID()) == -1)) {
                        //Make sure that it's not the the one we're currently connected to
                        if (!info.getBSSID().equalsIgnoreCase(wifiResults.get(x).BSSID)) {
                            //Compare the current signal level to our WiFi Elect level
                            // TODO - Assign the currentWifi value and the Looping value     ******************************************************************************************

                            if (info.getRssi() > wifiResults.get(x).level) {
                            }
                            break;
                        } else {
                            continue;
                        }
                    }
                }
            }
        }

    }
}
