package com.swjtu.gcmformojo;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import static com.swjtu.gcmformojo.CurrentUserActivity.currentUserList;
import static com.swjtu.gcmformojo.MyFirebaseMessagingService.msgCountMap;

/**
 * Created by HeiPi on 2017/1/24.
 * 处理QQ通知点击
 */

public class QqNotificationBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Bundle msgNotifyBundle = intent.getExtras();
        int notifyId = msgNotifyBundle.getInt("notifyId");
        String qqPackgeName=msgNotifyBundle.getString("qqPackgeName");

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

        if (action.equals("qq_notification_clicked")) {
            //处理点击事件

                // 通过包名获取要跳转的app，创建intent对象
                Intent intentNewQq = context.getPackageManager().getLaunchIntentForPackage(qqPackgeName);

                if (intentNewQq != null) {
                    if (msgCountMap.get(notifyId) != null)
                        msgCountMap.put(notifyId, 0);
                    context.startActivity(intentNewQq);
                } else {
                    // 没有安装要跳转的app应用进行提醒
                    Toast.makeText(context.getApplicationContext(), "未检测到" + qqPackgeName, Toast.LENGTH_LONG).show();
                }

        }

        if (action.equals("qq_notification_cancelled")) {
            //处理滑动清除和点击删除事件
            if(msgCountMap.get(notifyId)!=null)
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
