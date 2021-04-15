package com.swjtu.gcmformojo;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

//跳转activity
public class QqPausedNotificationReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        Bundle msgNotifyBundle = intent.getExtras();
        int notifyId = msgNotifyBundle.getInt("notifyId");
        if (notifyId != -1) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(notifyId);
        }

        context.startActivity(new Intent(context, QqPausedNotificationActivity.class));
    }
}
