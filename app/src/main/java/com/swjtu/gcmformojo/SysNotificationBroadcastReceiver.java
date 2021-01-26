package com.swjtu.gcmformojo;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by HeiPi on 2017/1/15.
 * 默认处理通知事件
 */

public class SysNotificationBroadcastReceiver extends BroadcastReceiver {

  //  private MyApplication MyApplication;
    private ArrayList<User> currentUserList;
    private Map<Integer, Integer> msgCountMap;

    @Override
    public void onReceive(Context context, Intent intent) {

     //   MyApplication = (MyApplication) context.getApplicationContext();
        currentUserList = MyApplication.getInstance().getCurrentUserList();
        msgCountMap = MyApplication.getInstance().getMsgCountMap();

        String action = intent.getAction();
        Bundle msgNotifyBundle = intent.getExtras();
        int notifyId = msgNotifyBundle.getInt("notifyId");

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

        if (action.equals("sys_notification_clicked")) {
            //处理点击事件

            msgCountMap.put(notifyId,0);
        }

        if (action.equals("sys_notification_cancelled")) {
            //处理滑动清除和点击删除事件

            msgCountMap.put(notifyId,0);
        }
    }

    /*
*子线程处理会话界面通信
*
*/
    class userThread extends Thread {
        private Context context;
        private Handler handler;
        public userThread(){

        }

        public userThread(Context context) {
            this.context = context;
            this.handler = ((CurrentUserActivity) context).getHandler();
        }

        @Override
        public void run() {
            Message msg = new Message();
            msg.obj = "Update currentUserList";
            CurrentUserActivity.userHandler.sendMessage(msg);
            super.run();
        }
    }
}
