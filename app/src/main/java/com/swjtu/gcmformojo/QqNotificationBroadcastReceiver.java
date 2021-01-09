package com.swjtu.gcmformojo;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import static com.swjtu.gcmformojo.CurrentUserActivity.currentUserList;
import static com.swjtu.gcmformojo.MyFirebaseMessagingService.msgCountMap;

/**
 * Created by HeiPi on 2017/1/24.
 * 处理QQ通知点击
 */

public class QqNotificationBroadcastReceiver extends BroadcastReceiver {

    public static final String QQPACKGENAME = "qqPackgeName";
    public static final String IntentNotificationId = "intentnotificationid";
    private static final String TAG = "GcmForMojoWebqq";


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int intentNotificationId = intent.getIntExtra(IntentNotificationId, -1);
        String qqPackgeName = intent.getStringExtra(QQPACKGENAME);

        if (intentNotificationId != -1) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(intentNotificationId);
            for(int    i=0;    i<currentUserList.size();    i++){
                if(currentUserList.get(i).getNotificationId()==intentNotificationId){
                    currentUserList.get(i).setMsgCount("0");
                    if(CurrentUserActivity.userHandler!=null)
                        new userThread().start();
                    break;
                }
            }
        }

        if (action.equals("qq_notification_clicked")) {
            //处理点击事件


          //  Log.d(TAG, "跳转到qq");


                // 通过包名获取要跳转的app，创建intent对象
                Intent intentNewQq = context.getPackageManager().getLaunchIntentForPackage(qqPackgeName);
                //  Intent intentNewQqi = context.getPackageManager().getLaunchIntentForPackage("com.tencent.mobileqqi");

                if (intentNewQq != null) {
                    if (msgCountMap.get(intentNotificationId) != null)
                        msgCountMap.put(intentNotificationId, 0);
                    context.startActivity(intentNewQq);
                } else {
                    // 没有安装要跳转的app应用进行提醒
                    Toast.makeText(context.getApplicationContext(), "未检测到" + qqPackgeName, Toast.LENGTH_LONG).show();
                }

        }

        if (action.equals("qq_notification_cancelled")) {
            //处理滑动清除和点击删除事件
            Log.d(TAG, "清除未读计数");
            if(msgCountMap.get(intentNotificationId)!=null)
            msgCountMap.put(intentNotificationId,0);

        }

        if (action.equals("qq_notification_reply")) {
            //处理回复事件
            Log.d(TAG, "打开回复弹出框");
            if(msgCountMap.get(intentNotificationId)!=null)
            msgCountMap.put(intentNotificationId,0); //清除计数

            Intent i=new Intent(context,DialogActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);


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
