package com.proper.utils;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;

/**
 * Created by Lebel on 13/08/2014.
 */
public class NetUtils {

    /**
     * Detecting whether the network is available
     *
     * @return
     */
    public static boolean isNetworkConnected(Activity act) {
        ConnectivityManager cm = (ConnectivityManager) act
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnectedOrConnecting();
    }

    /**
     * Get the current network type
     *
     * @return 0：No Network 1：WiFi Network 2：WAP Network 3：Ethernet Network
     */
    public static String getNetworkType(Activity act) {
        String netType = "";
        ConnectivityManager connectivityManager = (ConnectivityManager) act
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null) {
            return netType;
        }
        int nType = networkInfo.getType();
        if (nType == ConnectivityManager.TYPE_MOBILE) {

            String extraInfo = networkInfo.getExtraInfo();
            if (extraInfo != null) {
                if (extraInfo.toLowerCase().equals("cmnet")) {
                    netType = "CMNET";
                } else {
                    netType = "CMWAP";
                }
            }
        } else if (nType == ConnectivityManager.TYPE_WIFI) {
            netType = "WIFI";
        }
        return netType;
    }

    /*
    * Disconnect from the current AP and remove configured networks.
    */
//    public boolean disconnectAP() {
//        // remove saved networks
//        if (!mWifiManager.isWifiEnabled()) {
//            log("Enabled wifi before remove configured networks");
//            mWifiManager.setWifiEnabled(true);
//            sleep(SHORT_TIMEOUT);
//        }
//        List<WifiConfiguration> wifiConfigList = mWifiManager.getConfiguredNetworks();
//        if (wifiConfigList == null) {
//            log("no configuration list is null");
//            return true;
//        }
//        log("size of wifiConfigList: " + wifiConfigList.size());
//        for (WifiConfiguration wifiConfig: wifiConfigList) {
//            log("remove wifi configuration: " + wifiConfig.networkId);
//            int netId = wifiConfig.networkId;
//            mWifiManager.forget(netId, new WifiManager.ActionListener() {
//                public void onSuccess() {
//                }
//                public void onFailure(int reason) {
//                    log("Failed to forget " + reason);
//                }
//            });
//        }
//        return true;
//    }

    public ScanResult scanForNetwork() {
        ScanResult result = null;
        return result;
    }
}
