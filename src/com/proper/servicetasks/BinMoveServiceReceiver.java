package com.proper.servicetasks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Lebel on 12/08/2014.
 */
public class BinMoveServiceReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, BinMoveService.class);
        context.startService(i);
    }
}
