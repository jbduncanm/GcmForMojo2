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
 * 处理微信通知点击
 */

public class WeixinNotificationBroadcastReceiver extends BroadcastReceiver {

    public static final String WXPACKGENAME = "wxPackgeName";
    public static final String IntentNotificationId = "intentnotificationid";
    private static final String TAG = "GcmForMojoWeixin";
    public static final String ISOPENWX = "isOpenWx";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int intentNotificationId = intent.getIntExtra(IntentNotificationId, -1);
        String wxPackgeName = intent.getStringExtra(WXPACKGENAME);
        Boolean isOpenWx = intent.getBooleanExtra(ISOPENWX,true);


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

        if (action.equals("weixin_notification_clicked")) {

            if(isOpenWx) {
            Intent intentNewWx = context.getPackageManager().getLaunchIntentForPackage(wxPackgeName);

                if (intentNewWx != null) {
                if(msgCountMap.get(intentNotificationId)!=null)
                msgCountMap.put(intentNotificationId,0);
                context.startActivity(intentNewWx);

                } else {
                Toast.makeText(context.getApplicationContext(), "未检测到微信"+wxPackgeName, Toast.LENGTH_LONG).show();
                }

              }else {
                Intent intentCurrentUser = new Intent(context,CurrentUserActivity.class);
                if (msgCountMap.get(intentNotificationId) != null)
                    msgCountMap.put(intentNotificationId, 0);
                context.startActivity(intentCurrentUser);

            }
        }


        if (action.equals("weixin_notification_cancelled")) {
            //处理滑动清除和点击删除事件

            Log.d(TAG, "清除未读计数");
            if(msgCountMap.get(intentNotificationId)!=null)
            msgCountMap.put(intentNotificationId,0);
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
