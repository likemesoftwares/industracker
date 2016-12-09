package com.industracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class IndusReceiver extends BroadcastReceiver {
    public IndusReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        Intent rintent = new Intent(context,FallDetectionService.class);
        context.startService(rintent);
    }
}
