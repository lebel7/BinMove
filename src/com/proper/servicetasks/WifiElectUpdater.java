package com.proper.servicetasks;

import android.util.Log;

/**
 * Created by Lebel on 13/08/2014.
 */
public class WifiElectUpdater extends Thread {
    private static final String TAG = "WifiElectUpdater Thread";
    private long interval = 60000;   //minute
    private boolean isRunning = false;
    private BinMoveService service;

    public WifiElectUpdater(String threadName, boolean isRunning) {
        super(TAG);
        isRunning = determinedServiceRunning();
    }

    @Override
    public void run() {
        super.run();

        while (isRunning) {
            try {
                Log.d(TAG, " Run");
                //do some WIFI work here
                Thread.sleep(interval);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
                isRunning = false;
            }
        }
    }

    private boolean determinedServiceRunning() {
        return isRunning;
    }
}
