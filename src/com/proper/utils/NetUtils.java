package com.proper.utils;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

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

    public boolean isNetworkAvailable(Context context) {
        boolean success = false;
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            try {
                URL url = new URL("http://www.google.com");
                HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
                urlc.setConnectTimeout(3000);
                urlc.connect();
                if (urlc.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    success = true;
                }
            } catch (MalformedURLException e1) {
                e1.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        return success;
    }

    public boolean connectToMainWIfi(WifiManager wifiManager) {
        // TODO - Handle Wifi Connectivity
        boolean success = false;
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        String pwd = "\"propermus1c\"";
        String ssid = "\"wifi_zz\"";
        if (removeAllSavedNetworks(wifiManager)) {
            // setup a wifi configuration to our chosen network
            WifiConfiguration wc = new WifiConfiguration();
            //wc.SSID = getResources().getString(R.string.ssid);
            //wc.preSharedKey = getResources().getString(R.string.password);
            wc.SSID = ssid;
            //wc.BSSID = result.BSSID;
            wc.preSharedKey = pwd;
            wc.status = WifiConfiguration.Status.ENABLED;
            wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            // connect to and enable the connection
            int netId = wifiManager.addNetwork(wc);
            wifiManager.disconnect();   //disconnect ->>
            wifiManager.enableNetwork(netId, true);
            success = wifiManager.reconnect();
        }
        return success;
    }

    private boolean removeAllSavedNetworks(WifiManager mainWifi) {
        boolean success = false;
        if (!mainWifi.isWifiEnabled()) {
            //Log.d(TAG, "Enabled wifi before remove configured networks");
            mainWifi.setWifiEnabled(true);
        }
        List<WifiConfiguration> wifiConfigList = mainWifi.getConfiguredNetworks();
        if (wifiConfigList == null) {
            //Log.d(TAG, "no configuration list is null");
            return true;
        }
        //Log.d(TAG, "size of wifiConfigList: " + wifiConfigList.size());
        for (WifiConfiguration wifiConfig: wifiConfigList) {
            //Log.d(TAG, "remove wifi configuration: " + wifiConfig.networkId);
            int netId = wifiConfig.networkId;
            mainWifi.removeNetwork(netId);
            mainWifi.saveConfiguration();
            success = true;
        }
        return success;
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
