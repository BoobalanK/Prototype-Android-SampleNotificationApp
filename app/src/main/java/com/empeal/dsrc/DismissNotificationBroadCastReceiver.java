package com.empeal.dsrc;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DismissNotificationBroadCastReceiver extends BroadcastReceiver {
    public static final String NOTIFICATION_ID = "NOTIFICATION_ID";
    @Override
    public void onReceive(Context context, Intent intent) {

        int notificationId = intent.getIntExtra(NOTIFICATION_ID,-1);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(notificationId);
    }
}
