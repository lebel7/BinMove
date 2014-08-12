package com.proper.servicetasks;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Timer;
import java.util.TimerTask;

//import com.android.barcode.BinMoveService;

/**
 * Created by Lebel on 02/04/2014.
 */
public class ScheduleReceiver extends BroadcastReceiver {
    //repeat every 3 minutes
    private static final long repeatTimeInterval = 1000 * 180;
    private Timer timer =  new Timer();

    @Override
    public void onReceive(Context context, Intent intent) {
        //do

        Intent i = new Intent(context, BinMoveService.class);
        PendingIntent pending = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
        timer.schedule(new ScheduleTask(), repeatTimeInterval);
    }

    private class ScheduleTask extends TimerTask {

        @Override
        public void run() {
            //do something with the db here
        }
    }
}
