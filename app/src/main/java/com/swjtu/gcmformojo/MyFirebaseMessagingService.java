package com.swjtu.gcmformojo;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.text.Spanned;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.wenming.library.BackgroundUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static android.app.Notification.DEFAULT_LIGHTS;
import static com.swjtu.gcmformojo.MyApplication.MYTAG;
import static com.swjtu.gcmformojo.MyApplication.QQ;
import static com.swjtu.gcmformojo.MyApplication.SYS;
import static com.swjtu.gcmformojo.MyApplication.WEIXIN;
import static com.swjtu.gcmformojo.MyApplication.getColorMsgTime;
import static com.swjtu.gcmformojo.MyApplication.getCurTime;
import static com.swjtu.gcmformojo.MyApplication.isQqOnline;
import static com.swjtu.gcmformojo.MyApplication.isWxOnline;
import static com.swjtu.gcmformojo.MyApplication.toSpannedMessage;


public class MyFirebaseMessagingService extends FirebaseMessagingService
{

    private SharedPreferences mySettings;

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage)
    {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        //handler = new Handler(Looper.getMainLooper()); // 使用应用的主消息循环

        Map<String, List<Spanned>> msgSave;
        Map<Integer, Integer> msgCountMap;
        Map<String, Integer> msgIdMap;
        ArrayList<User> currentUserList;


        msgSave = MyApplication.getInstance().getMsgSave();
        msgCountMap = MyApplication.getInstance().getMsgCountMap();
        msgIdMap = MyApplication.getInstance().getMsgIdMap();
        currentUserList = MyApplication.getInstance().getCurrentUserList();
        mySettings = getSharedPreferences("com.swjtu.gcmformojo_preferences", Context.MODE_PRIVATE);


        Log.d(MYTAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0)
        {
            Log.d(MYTAG, "原始信息: " + remoteMessage.getData());

        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null)
        {
            Log.d(MYTAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }


        if (remoteMessage.getData().size() > 0)
        {
            String msgId;
            String msgType;
            String senderType;
            String msgTitle;
            String msgBody;
            String msgIsAt;
            int notifyId;
            int msgCount;

            msgId = remoteMessage.getData().get("msgId");
            msgTitle = remoteMessage.getData().get("title");
            msgBody = remoteMessage.getData().get("message");
            msgType = remoteMessage.getData().get("type");
            senderType = remoteMessage.getData().get("senderType");

            if (msgId == null) msgId = "0"; //处理特殊情况
            if (senderType == null) senderType = "1"; //处理特殊情况 默认为好友

            if (remoteMessage.getData().get("isAt") != null )
                msgIsAt = remoteMessage.getData().get("isAt");
            else
                msgIsAt = "0";

            //进行通知数据准备
            //利用msgId生成通知id存储到hashmap中全局使用
            if (msgIdMap.get(msgId) == null)
            {
                switch (msgType) {
                    case QQ:
                        if (msgId.length() > 9) {
                            notifyId = Integer.parseInt(msgId.substring(0, 9));
                        } else {
                            notifyId = Integer.parseInt(msgId);
                        }
                        break;
                    case WEIXIN:
                        //微信的ID用随机数字代替
                        Random random = new Random();
                        notifyId = random.nextInt(10000);
                        break;
                    case SYS:
                        notifyId = Integer.parseInt(msgId); //QQ通知为1，微信通知为2
                        break;
                    default:
                        notifyId = 0; //其它未知类型消息Id设置为0
                }

                msgIdMap.put(msgId, notifyId); //写入msgIdMap
            } else
            {
                notifyId = msgIdMap.get(msgId);
            }

            //未读消息计数
            if (msgCountMap.get(notifyId) == null || msgCountMap.get(notifyId).equals(0))
            {
                msgCount = 1;
            } else
            {
                msgCount = msgCountMap.get(notifyId) + 1;
            }
            if (DialogActivity.notifyId == notifyId)
            {  //如果弹出窗口消息id与最新id一致，则将未读消息清0
                msgCount = 0;
            }
            msgCountMap.put(notifyId, msgCount);

            //无论任何设置都先存储消息，供对话列表使用,仅更新最后一条消息
            User currentUser = new User(msgTitle, msgId, msgType, msgBody, getCurTime(), senderType, notifyId, String.valueOf(msgCount));
            for (int i = 0; i < currentUserList.size(); i++)
            {
                if (currentUserList.get(i).getUserId().equals(msgId))
                {
                    currentUserList.remove(i);
                    break;
                }
            }
            currentUserList.add(0, currentUser);
            if (CurrentUserActivity.userHandler != null)
                new userThread().start();

            //存储对话框消息记录：有可能存储后系统回收内存造成点击通知进入列表界面后为空，需要在点击时将相关变量传入会话列表界面。
            Spanned spannedMessage = toSpannedMessage(getColorMsgTime(msgType, false) + msgBody);

            if (msgSave.get(msgId) == null)
            {

                List<Spanned> msgList = new ArrayList<>();
                msgList.add(spannedMessage);
                msgSave.put(msgId, msgList);

            } else
            {

                List<Spanned> msgList = msgSave.get(msgId);
                msgList.add(spannedMessage);
                msgSave.put(msgId, msgList);

            }
            //如果回复窗口存在则发送handler消息，更新ui界面
            if (DialogActivity.msgHandler != null)
                new MsgThread().start();

            //提取设置数据
            String qqReciveType = mySettings.getString("qq_list_preference_1", "1");
            String wxReciveType = mySettings.getString("wx_list_preference_1", "1");
            Boolean qqIsDetail = mySettings.getBoolean("check_box_preference_qq_detail", true);
            Boolean wxIsDetail = mySettings.getBoolean("check_box_preference_wx_detail", true);
            String qqSound = mySettings.getString("ringtone_preference_qq", "");
            String wxSound = mySettings.getString("ringtone_preference_wx", "");
            String qqVibrate = mySettings.getString("qq_list_preference_vibrate", "1");
            String wxVibrate = mySettings.getString("wx_list_preference_vibrate", "1");
            Boolean qqIsReciveGroup = mySettings.getBoolean("check_box_preference_qq_isReciveGroup", true);
            Boolean wxIsReciveGroup = mySettings.getBoolean("check_box_preference_wx_isReciveGroup", true);

            String qqPackgeName = mySettings.getString("edit_text_preference_qq_packgename", "com.tencent.mobileqq");
            String wxPackgeName = mySettings.getString("edit_text_preference_wx_packgename", "com.tencent.mm");


            //  如果当前聊天对象已经弹出窗口则终止通知 最高优先级
            if (DialogActivity.notifyId == notifyId)
            {
                return;
            }

            //通过设置参数进行通知
            if (msgType.equals(QQ))
            {
                long time = new Date().getTime();
                long paused_time = getSharedPreferences("paused_time", MODE_PRIVATE).getLong("paused_time", 0);

                if (!mySettings.getBoolean("check_box_preference_qq", false))
                { //关闭推送
                    return;
                } else if (time < paused_time)
                { //暂停通知
                    return;
                }

                //判断是否弹出群消息
                if (!qqIsReciveGroup)
                {
                    if (senderType.equals("2") || senderType.equals("3"))
                    {
                        if(msgIsAt.equals("0")) //群消息且没有At则返回
                        return;
                    }
                }

                //判断接收方式
                //QQ判断
                switch (qqReciveType) {
                    case "1":
                        Log.d(MYTAG, "QQ不检测运行状态！");
                        break;
                    case "2":
                        Boolean isForeground;
                        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
                        {

                            isForeground = queryAppUsageStats(this, qqPackgeName);
                            if (isForeground)
                            {
                                Log.d(MYTAG, "QQ前台不推送！");
                                return;
                            }

                        } else if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT)
                        {

                            isForeground = BackgroundUtil.getRunningTask(this, qqPackgeName);
                            if (isForeground)
                            {
                                Log.d(MYTAG, "QQ前台不推送！");
                                return;
                            }

                        } else
                        { //对5.0 API 21 单独处理

                            isForeground = BackgroundUtil.getLinuxCoreInfo(this, qqPackgeName);
                            if (isForeground)
                            {
                                Log.d(MYTAG, "QQ前台不推送！");
                                return;
                            }

                        }
                        break;
                    case "3":
                        if (isServiceRunning(this, qqPackgeName))
                        {
                            Log.d(MYTAG, "QQ运行不推送！");
                            return;
                        }
                        break;
                    case "4":
                        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        {
                            UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
                            if (!usageStatsManager.isAppInactive(qqPackgeName))
                            {
                                Log.d(MYTAG, "QQ启用不推送！");
                                return;
                            }
                        } else
                        {
                            Looper.prepare();
                            Toast.makeText(getApplicationContext(), "您的系统不支持浅睡模式！", Toast.LENGTH_LONG).show();
                            Looper.loop();
                        }
                        break;
                }
            }

            //微信判断

            if (msgType.equals(WEIXIN))
            {

                if (!mySettings.getBoolean("check_box_preference_wx", false))
                { //关闭推送
                    return;
                }

                //判断是否弹出群消息
                if (!wxIsReciveGroup)
                {
                    if (senderType.equals("2") || senderType.equals("3"))
                    {
                        if(msgIsAt.equals("0")) //群消息且没有At则返回
                        return;
                    }
                }

                switch (wxReciveType) {
                    case "1":
                        Log.d(MYTAG, "微信不检测运行状态！");
                        break;
                    case "2":
                        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
                        {

                            Boolean isForeground = queryAppUsageStats(this, wxPackgeName);
                            if (isForeground)
                            {
                                Log.d(MYTAG, "微信前台不推送！");
                                return;
                            }

                        } else if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT)
                        {

                            Boolean isForeground = BackgroundUtil.getRunningTask(this, wxPackgeName);
                            if (isForeground)
                            {
                                Log.d(MYTAG, "微信前台不推送！");
                                return;
                            }
                        } else
                        {

                            Boolean isForeground = BackgroundUtil.getLinuxCoreInfo(this, wxPackgeName);
                            if (isForeground)
                            {
                                Log.d(MYTAG, "微信前台不推送！");
                                return;
                            }
                        }
                        break;
                    case "3":
                        if (isServiceRunning(this, wxPackgeName))
                        {
                            Log.d(MYTAG, "微信运行不推送！");
                            return;
                        }
                        break;
                    case "4":

                        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        {
                            UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);

                            if (!usageStatsManager.isAppInactive(wxPackgeName))
                            {
                                Log.d(MYTAG, "微信启用不推送！");
                                return;
                            }
                        } else
                        {
                            Looper.prepare();
                            Toast.makeText(getApplicationContext(), "您的系统不支持浅睡模式！", Toast.LENGTH_LONG).show();
                            Looper.loop();
                        }
                        break;
                }
            }


            //弹出通知
            switch (msgType) {
                case QQ:
                    if (!qqIsDetail)
                    {
                        msgTitle = "联系人";
                        msgBody = "你收到了消息！";
                    }
                    sendNotificationQq(msgTitle, msgBody, notifyId, msgCount, qqSound, qqVibrate, msgId, senderType, qqPackgeName);
                    break;
                case WEIXIN:
                    if (!wxIsDetail)
                    {
                        msgTitle = "联系人";
                        msgBody = "你收到了消息！";
                    }
                    sendNotificationWx(msgTitle, msgBody, notifyId, msgCount, wxSound, wxVibrate, msgId, senderType, wxPackgeName);
                    break;
                case SYS:
                    if (msgTitle.contains("二维码事件"))
                    {
                        try {
                            download(this, msgBody);
                        } catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }

                    //设置登录变量
                    if (msgTitle.contains("扫描二维码事件"))
                    {
                        if (msgId.equals("1"))
                        {
                            isQqOnline = 0;
                        } else if (msgId.equals("2"))
                        {
                            isWxOnline = 0;
                        }
                    }

                    if (msgBody.contains("登录成功"))
                    {
                        if (msgId.equals("1")) //QQ登录事件
                        {
                            isQqOnline = 1;
                            //清除会话列表
                            for (int i = 0; i < currentUserList.size(); i++)
                            {
                                if (currentUserList.get(i).getUserType().equals(QQ))
                                {
                                    currentUserList.remove(i);
                                }
                            }
                            //清除好友列表
                            MyApplication.getInstance().getQqFriendArrayList().clear();
                            MyApplication.getInstance().getQqFriendGroups().clear();
                            //清除聊天记录
                            for (Object o : msgSave.entrySet()) {
                                String key = o.toString();
                                if (key.length() == 10)  //QQ的key使用msgId为10位
                                    msgSave.remove(key);
                            }
                        } else if (msgId.equals("2")) //微信登录事件
                        {
                            isWxOnline = 1;
                            //清除会话列表
                            for (int i = 0; i < currentUserList.size(); i++)
                            {
                                if (currentUserList.get(i).getUserType().equals(WEIXIN))
                                {
                                    currentUserList.remove(i);
                                }
                            }
                            //清除好友列表
                            //清除聊天记录
                            for (Object o : msgSave.entrySet()) {
                                String key = o.toString();
                                if (key.length() > 10)  //微信的key使用msgId大于10位
                                    msgSave.remove(key);
                            }
                        }
                        //删除所有二维码
                        Log.i(MYTAG, "onMessageReceived: 准备删除二维码");
                        File file = new File(Environment.getExternalStorageDirectory() + "/GcmForMojo/");
                        File[] childFiles = file.listFiles();
                        for (File temp : childFiles)
                        {
                            Log.d(MYTAG, "onMessageReceived: delete: " + temp.getAbsolutePath());
                            //noinspection ResultOfMethodCallIgnored
                            temp.delete();
                        }
                        //更新会话列表
                        if (CurrentUserActivity.userHandler != null)
                            new userThread().start();
                        //更新聊天对话框
                        if (DialogActivity.msgHandler != null)
                            new MsgThread().start();
                    }

                    //发出系统通知
                    sendNotificationSys(msgTitle, msgBody, msgId, notifyId, msgCount);
                    break;
            }
        }

    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param msgBody FCM message body received.
     */

    //qq通知方法
    private void sendNotificationQq(String msgTitle, String msgBody, int notifyId, int msgCount, String qqSound, String qqVibrate, String msgId,
                                    String senderType, String qqPackgeName)
    {

        Boolean isOpenQq = mySettings.getBoolean("check_box_preference_qq_isOpenQq", true);

        Bundle msgNotifyBundle = new Bundle();
        msgNotifyBundle.putInt("notifyId", notifyId);
        msgNotifyBundle.putString("qqPackgeName", qqPackgeName);

        //通知清除事件(接收器)
        Intent intentCancel = new Intent(this, QqNotificationBroadcastReceiver.class);
        intentCancel.setAction("qq_notification_cancelled");
        intentCancel.putExtras(msgNotifyBundle);
        PendingIntent pendingIntentCancel = PendingIntent.getBroadcast(this, notifyId, intentCancel, PendingIntent.FLAG_UPDATE_CURRENT);

        //通知暂停事件 by Mystery0
       // Intent intentPause = new Intent(this, QqPausedNotificationReceiver.class);
       // intentPause.setAction("qq_notification_paused");
       // intentPause.putExtras(msgNotifyBundle);
       // PendingIntent pendingIntentPause = PendingIntent.getBroadcast(this, notifyId, intentPause, PendingIntent.FLAG_UPDATE_CURRENT);

        //应用界面 传递最后一次消息内容，避免会话列表为空
        Intent intentList = new Intent(this, CurrentUserActivity.class);
        intentList.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Bundle msgListBundle = new Bundle();
        msgListBundle.putString("userName", msgTitle);
        msgListBundle.putString("userId", msgId);
        msgListBundle.putString("userType", QQ);
        msgListBundle.putString("userMessage", msgBody);
        msgListBundle.putString("userTime", getCurTime());
        msgListBundle.putString("senderType", senderType);
        msgListBundle.putInt("notifyId", notifyId);
        msgListBundle.putString("msgCount", String.valueOf(msgCount));
        intentList.putExtras(msgListBundle);
        PendingIntent pendingIntentList = PendingIntent.getActivity(this, notifyId, intentList, PendingIntent.FLAG_UPDATE_CURRENT);

        //qq界面(接收器)
        Intent intentQq = new Intent(this, QqNotificationBroadcastReceiver.class);
        intentQq.setAction("qq_notification_clicked");
        intentQq.putExtras(msgNotifyBundle);
        PendingIntent pendingIntentQq = PendingIntent.getBroadcast(this, notifyId, intentQq, PendingIntent.FLAG_ONE_SHOT);

       //回复窗口
        Intent intentDialog = new Intent(this, DialogActivity.class);
        intentDialog.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        Bundle msgDialogBundle = new Bundle();
        msgDialogBundle.putString("msgId", msgId);
        msgDialogBundle.putString("senderType", senderType);
        msgDialogBundle.putString("msgType", QQ);
        msgDialogBundle.putString("msgTitle", msgTitle);
        msgDialogBundle.putString("msgBody", msgBody);
        msgDialogBundle.putInt("notifyId", notifyId);
        msgDialogBundle.putString("msgTime", getCurTime());
        msgDialogBundle.putString("qqPackgeName", qqPackgeName);
        intentDialog.putExtras(msgDialogBundle);
        PendingIntent pendingIntentDialog = PendingIntent.getActivity(this, notifyId, intentDialog, PendingIntent.FLAG_UPDATE_CURRENT);

        StringBuffer ticker = new StringBuffer();
        ticker.append(msgTitle);
        ticker.append("\r\n");
        ticker.append(msgBody);

        if (msgCount != 1)
        {
            msgTitle = msgTitle + "(" + msgCount + "条新消息)";
        }

        Uri defaultSoundUri = Uri.parse(qqSound);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.qq_notification)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.qq))
                .setTicker(ticker)
                .setContentTitle(msgTitle)
                .setContentText(msgBody)
                .setStyle(new NotificationCompat.BigTextStyle() // 设置通知样式为大型文本样式
                        .bigText(msgBody))
                .setAutoCancel(true)
                .setNumber(msgCount)
                .setSound(defaultSoundUri)
                .setDefaults(DEFAULT_LIGHTS)
                .setDeleteIntent(pendingIntentCancel);


        //自动弹出
        notificationBuilder.setPriority(Notification.PRIORITY_HIGH);

        //振动参数
        switch (qqVibrate) {
            case "1":
                notificationBuilder.setVibrate(new long[]{0});
                break;
            case "2":
                notificationBuilder.setVibrate(new long[]{0, 100, 300, 100});
                break;
            case "3":
                notificationBuilder.setVibrate(new long[]{0, 500, 300, 500});
                break;
            default:
                notificationBuilder.setVibrate(new long[]{0});
        }

        Boolean qqIsReply=mySettings.getBoolean("check_box_preference_qq_reply",false);

        if(qqIsReply)
            notificationBuilder.addAction(0, "回复", pendingIntentDialog);
        else
            notificationBuilder.addAction(0, "列表", pendingIntentList);
        notificationBuilder.addAction(0, "清除", pendingIntentCancel);
      // notificationBuilder.addAction(0, "暂停", pendingIntentPause);


        //开启应用界面还是QQ界面
        if (isOpenQq)
            notificationBuilder.setContentIntent(pendingIntentQq);
        else
            notificationBuilder.setContentIntent(pendingIntentList);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notifyId, notificationBuilder.build());

    }

    //微信通知方法
    private void sendNotificationWx(String msgTitle, String msgBody, int notifyId, int msgCount, String wxSound, String wxVibrate, String msgId,
                                    String senderType, String wxPackgeName)
    {
        Boolean isOpenWx = mySettings.getBoolean("check_box_preference_wx_isOpenWx", true);

        Bundle msgNotifyBundle = new Bundle();
        msgNotifyBundle.putInt("notifyId", notifyId);
        msgNotifyBundle.putString("wxPackgeName", wxPackgeName);

        //通知清除事件(接收器)
        Intent intentCancel = new Intent(this, WeixinNotificationBroadcastReceiver.class);
        intentCancel.setAction("weixin_notification_cancelled");
        intentCancel.putExtras(msgNotifyBundle);
        PendingIntent pendingIntentCancel = PendingIntent.getBroadcast(this, notifyId, intentCancel, PendingIntent.FLAG_UPDATE_CURRENT);

        //应用界面 传递最后一次消息内容，避免会话列表为空
        Intent intentList = new Intent(this, CurrentUserActivity.class);
        intentList.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Bundle msgListBundle = new Bundle();
        msgListBundle.putString("userName", msgTitle);
        msgListBundle.putString("userId", msgId);
        msgListBundle.putString("userType", WEIXIN);
        msgListBundle.putString("userMessage", msgBody);
        msgListBundle.putString("userTime", getCurTime());
        msgListBundle.putString("senderType", senderType);
        msgListBundle.putInt("notifyId", notifyId);
        msgListBundle.putString("msgCount", String.valueOf(msgCount));
        intentList.putExtras(msgListBundle);
        PendingIntent pendingIntentList = PendingIntent.getActivity(this, notifyId, intentList, PendingIntent.FLAG_UPDATE_CURRENT);

        //微信界面（接收器）
        Intent intentWx = new Intent(this, WeixinNotificationBroadcastReceiver.class);
        intentWx.setAction("weixin_notification_clicked");
        intentWx.putExtras(msgNotifyBundle);
        PendingIntent pendingIntentWx = PendingIntent.getBroadcast(this, notifyId, intentWx, PendingIntent.FLAG_ONE_SHOT);

        //回复窗口
        Intent intentDialog = new Intent(this, DialogActivity.class);
        intentDialog.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        Bundle msgDialogBundle = new Bundle();
        msgDialogBundle.putString("msgId", msgId);
        msgDialogBundle.putString("senderType", senderType);
        msgDialogBundle.putString("msgType", WEIXIN);
        msgDialogBundle.putString("msgTitle", msgTitle);
        msgDialogBundle.putString("msgBody", msgBody);
        msgDialogBundle.putInt("notifyId", notifyId);
        msgDialogBundle.putString("msgTime", getCurTime());
        msgDialogBundle.putString("wxPackgeName", wxPackgeName);
        intentDialog.putExtras(msgDialogBundle);
        PendingIntent pendingIntentDialog = PendingIntent.getActivity(this, notifyId, intentDialog, PendingIntent.FLAG_UPDATE_CURRENT);

        StringBuffer tickerWx = new StringBuffer();
        tickerWx.append(msgTitle);
        tickerWx.append("\r\n");
        tickerWx.append(msgBody);

        if (msgCount != 1)
        {
            msgTitle = msgTitle + "(" + msgCount + "条新消息)";
        }

        Uri defaultSoundUri = Uri.parse(wxSound);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.weixin_notification)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.weixin))
                .setTicker(tickerWx)
                .setContentTitle(msgTitle)
                .setStyle(new NotificationCompat.BigTextStyle() // 设置通知样式为大型文本样式
                        .bigText(msgBody))
                .setContentText(msgBody)
                .setAutoCancel(true)
                .setNumber(msgCount)
                .setSound(defaultSoundUri)
                .setDefaults(DEFAULT_LIGHTS)
                .setDeleteIntent(pendingIntentCancel);


        //自动弹出
        notificationBuilder.setPriority(Notification.PRIORITY_HIGH);

        //振动方式
        switch (wxVibrate) {
            case "1":
                notificationBuilder.setVibrate(new long[]{0});
                break;
            case "2":
                notificationBuilder.setVibrate(new long[]{0, 100, 300, 100});
                break;
            case "3":
                notificationBuilder.setVibrate(new long[]{0, 500, 300, 500});
                break;
            default:
                notificationBuilder.setVibrate(new long[]{0});
        }


        Boolean wxIsReply=mySettings.getBoolean("check_box_preference_wx_reply",false);
        if(wxIsReply)
            notificationBuilder.addAction(0, "回复", pendingIntentDialog);
        else
            notificationBuilder.addAction(0, "列表", pendingIntentList);
        notificationBuilder.addAction(0, "清除", pendingIntentCancel);


        //开启回复还是微信
        if (isOpenWx)
            notificationBuilder.setContentIntent(pendingIntentWx);
        else
            notificationBuilder.setContentIntent(pendingIntentList);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notifyId, notificationBuilder.build());
    }


    //系统通知方法
    private void sendNotificationSys(String msgTitle, String msgBody, String msgId, int notifyId, int msgCount)
    {
        Intent intent = new Intent(this, CurrentUserActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Bundle msgListBundle = new Bundle();
        msgListBundle.putString("userName", msgTitle);
        msgListBundle.putString("userId", msgId);
        msgListBundle.putString("userType", SYS);
        msgListBundle.putString("userMessage", msgBody);
        msgListBundle.putString("userTime", getCurTime());
        msgListBundle.putString("senderType", "1");
        msgListBundle.putInt("notifyId", notifyId);
        msgListBundle.putString("msgCount", String.valueOf(msgCount));
        intent.putExtras(msgListBundle);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, notifyId /* Request code */, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Bundle msgNotifyBundle = new Bundle();
        msgNotifyBundle.putInt("notifyId", notifyId);

        //通知清除事件
        Intent intentCancel = new Intent(this, SysNotificationBroadcastReceiver.class);
        intentCancel.setAction("sys_notification_cancelled");
        intentCancel.putExtras(msgNotifyBundle);
        PendingIntent pendingIntentCancel = PendingIntent.getBroadcast(this, notifyId, intentCancel, PendingIntent.FLAG_ONE_SHOT);

        StringBuffer tickerSys = new StringBuffer();
        tickerSys.append(msgTitle);
        tickerSys.append("\r\n");
        tickerSys.append(msgBody);

        if (msgCount != 1)
        {
            msgTitle = msgTitle + "(" + msgCount + "条新消息)";
        }

        int smallIcon;
        Bitmap largeIcon;

        switch (msgId) {
            case "1":
                smallIcon = R.drawable.qq_notification;
                largeIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.qq);
                break;
            case "2":
                smallIcon = R.drawable.weixin_notification;
                largeIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.weixin);
                break;
            default:
                smallIcon = R.drawable.sys_notification;
                largeIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.sys);
        }

        int defaults = 0;
        defaults |= Notification.DEFAULT_LIGHTS;
        defaults |= Notification.DEFAULT_VIBRATE;
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(smallIcon)
                .setLargeIcon(largeIcon)
                .setTicker(tickerSys)
                .setContentTitle(msgTitle)
                .setStyle(new NotificationCompat.BigTextStyle() // 设置通知样式为大型文本样式
                        .bigText(msgBody))
                .setContentText(msgBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setDefaults(defaults)
                .setContentIntent(pendingIntent)
                .setDeleteIntent(pendingIntentCancel);
        notificationBuilder.setPriority(Notification.PRIORITY_HIGH); //自动弹出通知

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notifyId, notificationBuilder.build());
    }

    /**
     * 判断应用是否已经启动
     *
     * @param context  一个context
     * @param serviceClassName 要判断应用的包名
     * @return boolean
     */
    private static boolean isServiceRunning(Context context, String serviceClassName)
    {
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo runningServiceInfo : services)
        {
            if (runningServiceInfo.service.getPackageName().equals(serviceClassName))
            {
                return true;
            }
        }
        return false;
    }

    //下载二维码
    private static void download(Context context, String docUrl) throws Exception
    {
        //获取存储卡路径、构成保存文件的目标路径
        //SD卡具有读写权限、指定附件存储路径为SD卡上指定的文件夹
        String dirName = Environment.getExternalStorageDirectory() + "/GcmForMojo/";
        File f = new File(dirName);
        if (!f.exists())
        {      //判断文件夹是否存在
            //noinspection ResultOfMethodCallIgnored
            f.mkdir();        //如果不存在、则创建一个新的文件夹
        }
        //准备拼接新的文件名
        String[] list = docUrl.split("/");
        String fileName = list[list.length - 1];
        String fileNameTemp = fileName;
        fileName = dirName + fileName;
        File file = new File(fileName);
        if (file.exists())
        {    //如果目标文件已经存在
            //noinspection ResultOfMethodCallIgnored
            file.delete();    //则删除旧文件
        }
        //1K的数据缓冲
        byte[] bs = new byte[1024];
        //读取到的数据长度
        int len;
        try
        {
            //通过文件地址构建url对象
            URL url = new URL(docUrl);
            //获取链接
            //URLConnection conn = url.openConnection();
            //创建输入流
            InputStream is = url.openStream();
            //获取文件的长度
            //int contextLength = conn.getContentLength();
            //输出的文件流
            OutputStream os = new FileOutputStream(file);
            //开始读取
            while ((len = is.read(bs)) != -1)
            {
                os.write(bs, 0, len);
            }
            //完毕关闭所有连接
            os.close();
            is.close();
        } catch (MalformedURLException e)
        {
            //fileName = null;
            System.out.println("创建URL对象失败");
            throw e;
        } catch (FileNotFoundException e)
        {
            // fileName = null;
            System.out.println("无法加载文件");
            throw e;
        } catch (IOException e)
        {
            //  fileName = null;
            System.out.println("获取连接失败");
            throw e;
        }


        // 其次把文件插入到系统图库
        try
        {
            MediaStore.Images.Media.insertImage(context.getContentResolver(),
                    file.getAbsolutePath(), fileNameTemp, null);
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

        // 最后通知图库更新
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri uri = Uri.fromFile(new File(fileName));
        intent.setData(uri);
        context.sendBroadcast(intent);

        // return fileName;
    }


    /**
     * 通过使用UsageStatsManager获取，此方法是android5.0A之后提供的API
     * 必须：
     * 1. 此方法只在android5.0以上有效
     * 2. AndroidManifest中加入此权限<uses-permission xmlns:tools="http://schemas.android.com/tools" android:name="android.permission.PACKAGE_USAGE_STATS"
     * tools:ignore="ProtectedPermissions" />
     * 3. 打开手机设置，点击安全-高级，在有权查看使用情况的应用中，为这个App打上勾
     *
     * @param context     上下文参数
     * @param packageName 需要检查是否位于栈顶的App的包名
     */

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static boolean queryAppUsageStats(Context context, String packageName)
    {
        class RecentUseComparator implements Comparator<UsageStats>
        {
            @Override
            public int compare(UsageStats lhs, UsageStats rhs)
            {
                return (lhs.getLastTimeUsed() > rhs.getLastTimeUsed()) ? -1 : (lhs.getLastTimeUsed() == rhs.getLastTimeUsed()) ? 0 : 1;
            }
        }

        RecentUseComparator mRecentComp = new RecentUseComparator();
        UsageStatsManager mUsageStatsManager = null;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
        {
            mUsageStatsManager = (UsageStatsManager) context.getSystemService(USAGE_STATS_SERVICE);
        }

        Calendar calendar = Calendar.getInstance();
        long endTime = calendar.getTimeInMillis();
        calendar.add(Calendar.YEAR, -1);
        long startTime = calendar.getTimeInMillis();

        assert mUsageStatsManager != null;
        List<UsageStats> usageStats = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime);
        if (usageStats == null || usageStats.size() == 0)
        {
            if (!HavaPermissionForTest(context))
            {
                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                //   Looper.prepare();
                //   Toast.makeText(context, "权限不够\n请打开手机设置，点击安全-高级，在有权查看使用情况的应用中，为这个App打上勾", Toast.LENGTH_SHORT).show();
                //   Looper.loop();
            }
            return false;
        }
        Collections.sort(usageStats, mRecentComp);
        String currentTopPackage = usageStats.get(0).getPackageName();
        Log.d(MYTAG, currentTopPackage);

        return currentTopPackage.equals(packageName);
    }

    /**
     * 判断是否有用权限
     *
     * @param context 上下文参数
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static boolean HavaPermissionForTest(Context context)
    {
        try
        {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            int mode = 0;
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            {
                mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName);
            }
            return (mode == AppOpsManager.MODE_ALLOWED);
        } catch (PackageManager.NameNotFoundException e)
        {
            return true;
        }
    }


    /*
    *子线程处理弹出框通信
    *
     */
    private class MsgThread extends Thread
    {
        @Override
        public void run()
        {
            Message msg = new Message();
            msg.obj = "UpdateMsgList";
            DialogActivity.msgHandler.sendMessage(msg);
            super.run();
        }
    }

    /*
*子线程处理会话界面通信
*
 */
    class userThread extends Thread
    {
        @Override
        public void run()
        {
            Message msg = new Message();
            msg.obj = "UpdateCurrentUserList";
            CurrentUserActivity.userHandler.sendMessage(msg);
            super.run();
        }
    }


}
