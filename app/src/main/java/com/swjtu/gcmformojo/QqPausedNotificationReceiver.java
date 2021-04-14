package com.swjtu.gcmformojo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

//跳转activity
public class QqPausedNotificationReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        context.startActivity(new Intent(context, QqPausedNotificationActivity.class));
    }
}
