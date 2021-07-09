package com.swjtu.gcmformojo;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by HeiPi on 2017/1/24.
 * 处理微信通知点击
 */

public class WeixinNotificationBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        ArrayList<User> currentUserList;
        Map<Integer, Integer> msgCountMap;

        currentUserList = MyApplication.getInstance().getCurrentUserList();
        msgCountMap = MyApplication.getInstance().getMsgCountMap();

        String action = intent.getAction();
        Bundle msgNotifyBundle = intent.getExtras();
        int notifyId = msgNotifyBundle.getInt("notifyId");
        String wxPackgeName=msgNotifyBundle.getString("wxPackgeName");

        if (notifyId != -1) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(notifyId);
            for(int    i=0;    i<currentUserList.size();    i++){
                if(currentUserList.get(i).getNotifyId()==notifyId){
                    currentUserList.get(i).setMsgCount("0");
                    if(CurrentUserActivity.userHandler!=null)
                        new userThread().start();
                    break;
                }
            }

        }

        if (action.equals("weixin_notification_clicked")) {


            Intent intentNewWx = context.getPackageManager().getLaunchIntentForPackage(wxPackgeName);

                if (intentNewWx != null) {
                if(msgCountMap.get(notifyId)!=null)
                msgCountMap.put(notifyId,0);
                context.startActivity(intentNewWx);
                } else {
                Toast.makeText(context.getApplicationContext(), R.string.toast_check_package_fail + wxPackgeName, Toast.LENGTH_LONG).show();
                }

        }


        if (action.equals("weixin_notification_cancelled")) {
            //处理滑动清除和点击删除事件
            if(msgCountMap.get(notifyId)!=null)
            msgCountMap.put(notifyId,0);
        }
    }

    /*
*子线程处理会话界面通信
*
*/
    private class userThread extends Thread {
        @Override
        public void run() {
            Message msg = new Message();
            msg.obj = "UpdateCurrentUserList";
            CurrentUserActivity.userHandler.sendMessage(msg);
            super.run();
        }
    }
}
