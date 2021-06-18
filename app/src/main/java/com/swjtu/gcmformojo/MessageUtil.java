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
import android.support.v4.app.RemoteInput;
import android.text.Spanned;
import android.util.Log;
import android.widget.Toast;

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

import static android.app.Notification.DEFAULT_LIGHTS;
import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.USAGE_STATS_SERVICE;
import static com.swjtu.gcmformojo.MyApplication.KEY_TEXT_REPLY;
import static com.swjtu.gcmformojo.MyApplication.MYTAG;
import static com.swjtu.gcmformojo.MyApplication.PREF;
import static com.swjtu.gcmformojo.MyApplication.QQ;
import static com.swjtu.gcmformojo.MyApplication.SYS;
import static com.swjtu.gcmformojo.MyApplication.WEIXIN;
import static com.swjtu.gcmformojo.MyApplication.WechatUIDConvert;
import static com.swjtu.gcmformojo.MyApplication.getColorMsgTime;
import static com.swjtu.gcmformojo.MyApplication.getCurTime;
import static com.swjtu.gcmformojo.MyApplication.isQqOnline;
import static com.swjtu.gcmformojo.MyApplication.isWxOnline;
import static com.swjtu.gcmformojo.MyApplication.mySettings;
import static com.swjtu.gcmformojo.MyApplication.toSpannedMessage;

/**
 *
 * @author heipidage
 * 处理推送消息
 */

public class MessageUtil {


    public static void  MessageUtilDo(Context context ,String msgId,String msgType,String senderType,String msgTitle,String msgBody,String msgIsAt) {

        Map<String, List<Spanned>> msgSave;
        Map<Integer, Integer> msgCountMap;
        Map<String, Integer> msgIdMap;
        ArrayList<User> currentUserList;


        msgSave = MyApplication.getInstance().getMsgSave();
        msgCountMap = MyApplication.getInstance().getMsgCountMap();
        msgIdMap = MyApplication.getInstance().getMsgIdMap();
        currentUserList = MyApplication.getInstance().getCurrentUserList();
        //SharedPreferences mySettings = context.getSharedPreferences(PREF, MODE_PRIVATE);


            int notifyId;
            int msgCount;


            if (msgId == null) msgId = "0"; //处理特殊情况
            if (senderType == null) senderType = "1"; //处理特殊情况 默认为好友
            if(msgIsAt == null) msgIsAt = "0"; //处理部分服务端未加@标志

            if(msgType.equals(QQ) ) { //如果能收到QQ或者微信的非系统消息，则表明在线
                isQqOnline=1;
            } else if ( msgType.equals(WEIXIN)) {
                isWxOnline = 1;
            }

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
                        notifyId = WechatUIDConvert(msgId);
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
                long paused_time = context.getSharedPreferences("paused_time", MODE_PRIVATE).getLong("paused_time", 0);

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

                            isForeground = queryAppUsageStats(context, qqPackgeName);
                            if (isForeground)
                            {
                                Log.d(MYTAG, "QQ前台不推送！");
                                return;
                            }

                        } else if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT)
                        {

                            isForeground = BackgroundUtil.getRunningTask(context, qqPackgeName);
                            if (isForeground)
                            {
                                Log.d(MYTAG, "QQ前台不推送！");
                                return;
                            }

                        } else
                        { //对5.0 API 21 单独处理

                            isForeground = BackgroundUtil.getLinuxCoreInfo(context, qqPackgeName);
                            if (isForeground)
                            {
                                Log.d(MYTAG, "QQ前台不推送！");
                                return;
                            }

                        }
                        break;
                    case "3":
                        if (isServiceRunning(context, qqPackgeName))
                        {
                            Log.d(MYTAG, "QQ运行不推送！");
                            return;
                        }
                        break;
                    case "4":
                        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        {
                            UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(USAGE_STATS_SERVICE);
                            if (!usageStatsManager.isAppInactive(qqPackgeName))
                            {
                                Log.d(MYTAG, "QQ启用不推送！");
                                return;
                            }
                        } else
                        {
                            Looper.prepare();
                            Toast.makeText(context.getApplicationContext(), R.string.toast_check_doze_fail, Toast.LENGTH_LONG).show();
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

                            Boolean isForeground = queryAppUsageStats(context, wxPackgeName);
                            if (isForeground)
                            {
                                Log.d(MYTAG, "微信前台不推送！");
                                return;
                            }

                        } else if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT)
                        {

                            Boolean isForeground = BackgroundUtil.getRunningTask(context, wxPackgeName);
                            if (isForeground)
                            {
                                Log.d(MYTAG, "微信前台不推送！");
                                return;
                            }
                        } else
                        {

                            Boolean isForeground = BackgroundUtil.getLinuxCoreInfo(context, wxPackgeName);
                            if (isForeground)
                            {
                                Log.d(MYTAG, "微信前台不推送！");
                                return;
                            }
                        }
                        break;
                    case "3":
                        if (isServiceRunning(context, wxPackgeName))
                        {
                            Log.d(MYTAG, "微信运行不推送！");
                            return;
                        }
                        break;
                    case "4":

                        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        {
                            UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(USAGE_STATS_SERVICE);

                            if (!usageStatsManager.isAppInactive(wxPackgeName))
                            {
                                Log.d(MYTAG, "微信启用不推送！");
                                return;
                            }
                        } else
                        {
                            Looper.prepare();
                            Toast.makeText(context.getApplicationContext(), R.string.toast_check_doze_fail, Toast.LENGTH_LONG).show();
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
                        msgTitle = context.getString(R.string.notification_title_default);
                        msgBody = context.getString(R.string.notification_detail_default);
                    }
                    sendNotificationQq(context, msgTitle, msgBody, notifyId, msgCount, qqSound, qqVibrate, msgId, senderType, qqPackgeName, msgIsAt);
                    break;
                case WEIXIN:
                    if (!wxIsDetail)
                    {
                        msgTitle = context.getString(R.string.notification_title_default);
                        msgBody = context.getString(R.string.notification_detail_default);
                    }
                    sendNotificationWx(context, msgTitle, msgBody, notifyId, msgCount, wxSound, wxVibrate, msgId, senderType, wxPackgeName, msgIsAt);
                    break;
                case SYS:
                    if (msgTitle.contains(context.getString(R.string.text_login_qrcode)))
                    {
                        try {
                            download(context, msgBody);
                        } catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }

                    //设置登录变量
                    if (msgTitle.contains(context.getString(R.string.text_login_qrcode_scan)))
                    {
                        if (msgId.equals("1"))
                        {
                            isQqOnline = 0;
                        } else if (msgId.equals("2"))
                        {
                            isWxOnline = 0;
                        }
                    }

                    if (msgBody.contains(context.getString(R.string.text_login_success)))
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
                    sendNotificationSys(context, msgTitle, msgBody, msgId, notifyId, msgCount);
                    break;
            }

    }

    //qq通知方法
    private static void sendNotificationQq(Context context, String msgTitle, String msgBody, int notifyId, int msgCount, String qqSound, String qqVibrate, String msgId,
                                           String senderType, String qqPackgeName, String msgIsAt)
    {
        SharedPreferences mySettings = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);

        String qqNotifyClick = mySettings.getString("qq_notify_click","1");

        Bundle msgNotifyBundle = new Bundle();
        msgNotifyBundle.putInt("notifyId", notifyId);
        msgNotifyBundle.putString("qqPackgeName", qqPackgeName);

        //通知清除事件(接收器)
        Intent intentCancel = new Intent(context,QqNotificationBroadcastReceiver.class);
        intentCancel.setAction("qq_notification_cancelled");
        intentCancel.putExtras(msgNotifyBundle);
        PendingIntent pendingIntentCancel = PendingIntent.getBroadcast(context, notifyId, intentCancel, PendingIntent.FLAG_UPDATE_CURRENT);

        //通知暂停事件 by Mystery0
        // Intent intentPause = new Intent(this, QqPausedNotificationReceiver.class);
        // intentPause.setAction("qq_notification_paused");
        // intentPause.putExtras(msgNotifyBundle);
        // PendingIntent pendingIntentPause = PendingIntent.getBroadcast(this, notifyId, intentPause, PendingIntent.FLAG_UPDATE_CURRENT);

        //应用界面 传递最后一次消息内容，避免会话列表为空
        Intent intentList = new Intent(context, CurrentUserActivity.class);
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
        PendingIntent pendingIntentList = PendingIntent.getActivity(context, notifyId, intentList, PendingIntent.FLAG_UPDATE_CURRENT);

        //qq界面(接收器)
        Intent intentQq = new Intent(context, QqNotificationBroadcastReceiver.class);
        intentQq.setAction("qq_notification_clicked");
        intentQq.putExtras(msgNotifyBundle);
        PendingIntent pendingIntentQq = PendingIntent.getBroadcast(context, notifyId, intentQq, PendingIntent.FLAG_ONE_SHOT);

        //回复动作
        Intent intentDialog;
        PendingIntent pendingIntentDialog;
        //根据系统版本判断可否直接回复
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            intentDialog = new Intent(context, ReplyService.class);
        } else {
            intentDialog = new Intent(context, DialogActivity.class);
            intentDialog.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        Bundle msgDialogBundle = new Bundle();
        msgDialogBundle.putString("msgId", msgId);
        msgDialogBundle.putString("senderType", senderType);
        msgDialogBundle.putString("msgType", QQ);
        msgDialogBundle.putString("msgTitle", msgTitle);
        msgDialogBundle.putString("msgBody", msgBody);
        msgDialogBundle.putInt("notifyId", notifyId);
        msgDialogBundle.putString("msgTime", getCurTime());
        msgDialogBundle.putString("qqPackgeName", qqPackgeName);
        msgDialogBundle.putString("fromNotify", "1");
        intentDialog.putExtras(msgDialogBundle);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            pendingIntentDialog = PendingIntent.getService(context, notifyId, intentDialog, PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            pendingIntentDialog = PendingIntent.getActivity(context, notifyId, intentDialog, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        StringBuffer ticker = new StringBuffer();
        ticker.append(msgTitle);
        ticker.append("\r\n");
        ticker.append(msgBody);

        if (msgCount != 1)
        {
            msgTitle = msgTitle + "(" + msgCount +context.getString(R.string.notify_title_msgcount_new) +")";
        }

        Uri defaultSoundUri = Uri.parse(qqSound);

        NotificationCompat.Builder notificationBuilder = null;
        //判断API版本，并设置通知图标颜色
        notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.qq_notification)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.qq))
                .setTicker(ticker)
                .setContentTitle(msgTitle)
                .setContentText(msgBody)
                .setStyle(new NotificationCompat.BigTextStyle() // 设置通知样式为大型文本样式
                        .bigText(msgBody))
                .setSubText(context.getString(R.string.notification_group_qq_name))
                .setAutoCancel(true)
                .setNumber(msgCount)
                .setSound(defaultSoundUri)
                .setDefaults(DEFAULT_LIGHTS)
                .setDeleteIntent(pendingIntentCancel);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setColor(context.getResources().getColor(R.color.colorNotification_qq));
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            notificationBuilder.setGroup(context.getString(R.string.notification_group_qq_id));
            notificationBuilder.setGroupSummary(true);
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            //根据发送者类别区分通知渠道
            switch(senderType) {
                case "1":
                    notificationBuilder.setChannelId(context.getString(R.string.notification_channel_qq_contact_id));
                    break;
                case "2":
                    if(msgIsAt.equals("1")){
                        notificationBuilder.setChannelId(context.getString(R.string.notification_channel_qq_at_id));
                    } else {
                        notificationBuilder.setChannelId(context.getString(R.string.notification_channel_qq_group_id));
                    }
                    break;
                case "3":
                    if(msgIsAt.equals("1")){
                        notificationBuilder.setChannelId(context.getString(R.string.notification_channel_qq_at_id));
                    } else {
                        notificationBuilder.setChannelId(context.getString(R.string.notification_channel_qq_discuss_id));
                    }
                    break;
            }
        }


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
            //针对安卓7.0及以上进行优化，直接进行通知栏回复
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                String replyLabel = context.getString(R.string.notification_action_reply);
                RemoteInput remoteInput = new RemoteInput.Builder(KEY_TEXT_REPLY)
                        .setLabel(replyLabel)
                        .build();
                NotificationCompat.Action reply_N = new NotificationCompat.Action.Builder(0, replyLabel, pendingIntentDialog)
                        .addRemoteInput(remoteInput)
                        .setAllowGeneratedReplies(true)
                        .build();
                notificationBuilder.addAction(reply_N);
            } else {
                notificationBuilder.addAction(0, context.getString(R.string.notification_action_reply), pendingIntentDialog);
            }
            notificationBuilder.addAction(0, context.getString(R.string.notification_action_list), pendingIntentList);
            notificationBuilder.addAction(0, context.getString(R.string.notification_action_clear), pendingIntentCancel);
        // notificationBuilder.addAction(0, "暂停", pendingIntentPause);

        //通知点击行为
        switch (qqNotifyClick) {
            case "1":
                notificationBuilder.setContentIntent(pendingIntentList);
                break;
            case "2":
                notificationBuilder.setContentIntent(pendingIntentDialog);
                break;
            case "3":
                notificationBuilder.setContentIntent(pendingIntentQq);
                break;
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notifyId, notificationBuilder.build());

    }

    //微信通知方法
    private static void sendNotificationWx(Context context ,String msgTitle, String msgBody, int notifyId, int msgCount, String wxSound, String wxVibrate, String msgId,
                                    String senderType, String wxPackgeName, String msgIsAt)
    {
        SharedPreferences mySettings = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String wxNotifyClick = mySettings.getString("wx_notify_click", "1");

        Bundle msgNotifyBundle = new Bundle();
        msgNotifyBundle.putInt("notifyId", notifyId);
        msgNotifyBundle.putString("wxPackgeName", wxPackgeName);

        //通知清除事件(接收器)
        Intent intentCancel = new Intent(context, WeixinNotificationBroadcastReceiver.class);
        intentCancel.setAction("weixin_notification_cancelled");
        intentCancel.putExtras(msgNotifyBundle);
        PendingIntent pendingIntentCancel = PendingIntent.getBroadcast(context, notifyId, intentCancel, PendingIntent.FLAG_UPDATE_CURRENT);

        //应用界面 传递最后一次消息内容，避免会话列表为空
        Intent intentList = new Intent(context, CurrentUserActivity.class);
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
        PendingIntent pendingIntentList = PendingIntent.getActivity(context, notifyId, intentList, PendingIntent.FLAG_UPDATE_CURRENT);

        //微信界面（接收器）
        Intent intentWx = new Intent(context, WeixinNotificationBroadcastReceiver.class);
        intentWx.setAction("weixin_notification_clicked");
        intentWx.putExtras(msgNotifyBundle);
        PendingIntent pendingIntentWx = PendingIntent.getBroadcast(context, notifyId, intentWx, PendingIntent.FLAG_ONE_SHOT);

        //回复动作
        Intent intentDialog;
        PendingIntent pendingIntentDialog;
        //根据系统版本判断可否直接回复
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            intentDialog = new Intent(context, ReplyService.class);
        } else {
            intentDialog = new Intent(context, DialogActivity.class);
            intentDialog.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        Bundle msgDialogBundle = new Bundle();
        msgDialogBundle.putString("msgId", msgId);
        msgDialogBundle.putString("senderType", senderType);
        msgDialogBundle.putString("msgType", WEIXIN);
        msgDialogBundle.putString("msgTitle", msgTitle);
        msgDialogBundle.putString("msgBody", msgBody);
        msgDialogBundle.putInt("notifyId", notifyId);
        msgDialogBundle.putString("msgTime", getCurTime());
        msgDialogBundle.putString("wxPackgeName", wxPackgeName);
        msgDialogBundle.putString("fromNotify", "1");
        intentDialog.putExtras(msgDialogBundle);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            pendingIntentDialog = PendingIntent.getService(context, 0, intentDialog, PendingIntent.FLAG_CANCEL_CURRENT);
        } else {
            pendingIntentDialog = PendingIntent.getActivity(context, notifyId, intentDialog, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        StringBuffer tickerWx = new StringBuffer();
        tickerWx.append(msgTitle);
        tickerWx.append("\r\n");
        tickerWx.append(msgBody);

        if (msgCount != 1)
        {
            msgTitle = msgTitle + "(" + msgCount + context.getString(R.string.notify_title_msgcount_new) +")";
        }

        Uri defaultSoundUri = Uri.parse(wxSound);

        NotificationCompat.Builder notificationBuilder = null;
        /**
         * Recommended usage for Android O:
         * NotificationCompat.Builder(Context context, String channelId);
         *
         * commented by Alex Wang
         * at 20180205
         */
        notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.weixin_notification)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.weixin))
                .setTicker(tickerWx)
                .setContentTitle(msgTitle)
                .setStyle(new NotificationCompat.BigTextStyle() // 设置通知样式为大型文本样式
                        .bigText(msgBody))
                .setContentText(msgBody)
                .setSubText(context.getString(R.string.notification_group_wechat_name))
                .setAutoCancel(true)
                .setNumber(msgCount)
                .setSound(defaultSoundUri)
                .setDefaults(DEFAULT_LIGHTS)
                .setDeleteIntent(pendingIntentCancel);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setColor(context.getResources().getColor(R.color.colorNotification_wechat));
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            notificationBuilder.setGroup(context.getString(R.string.notification_group_wechat_id));
            notificationBuilder.setGroupSummary(true);
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            switch(senderType) {
                case "1":
                    notificationBuilder.setChannelId(context.getString(R.string.notification_channel_wechat_contact_id));
                    break;
                case "2":
                    if(msgIsAt.equals("1")){
                        notificationBuilder.setChannelId(context.getString(R.string.notification_channel_wechat_at_id));
                    } else {
                        notificationBuilder.setChannelId(context.getString(R.string.notification_channel_wechat_group_id));
                    }
                    break;
                case "3":
                    if(msgIsAt.equals("1")){
                        notificationBuilder.setChannelId(context.getString(R.string.notification_channel_wechat_at_id));
                    } else {
                        notificationBuilder.setChannelId(context.getString(R.string.notification_channel_wechat_discuss_id));
                    }
                    break;
            }
        }

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
            //针对安卓7.0及以上进行优化，直接进行通知栏回复
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                String replyLabel = context.getString(R.string.notification_action_reply);
                RemoteInput remoteInput = new RemoteInput.Builder(KEY_TEXT_REPLY)
                        .setLabel(replyLabel)
                        .build();
                NotificationCompat.Action reply_N = new NotificationCompat.Action.Builder(0, replyLabel, pendingIntentDialog)
                        .addRemoteInput(remoteInput)
                        .setAllowGeneratedReplies(true)
                        .build();
                notificationBuilder.addAction(reply_N);
            } else {
                notificationBuilder.addAction(0, context.getString(R.string.notification_action_reply), pendingIntentDialog);
            }
            notificationBuilder.addAction(0, context.getString(R.string.notification_action_list), pendingIntentList);
            notificationBuilder.addAction(0, context.getString(R.string.notification_action_clear), pendingIntentCancel);

        //通知点击行为
        switch (wxNotifyClick) {
            case "1":
                notificationBuilder.setContentIntent(pendingIntentList);
                break;
            case "2":
                notificationBuilder.setContentIntent(pendingIntentDialog);
                break;
            case "3":
                notificationBuilder.setContentIntent(pendingIntentWx);
                break;
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notifyId, notificationBuilder.build());
    }


    //系统通知方法
    private static void sendNotificationSys(Context context, String msgTitle, String msgBody, String msgId, int notifyId, int msgCount)
    {
        Intent intent = new Intent(context, CurrentUserActivity.class);
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
        PendingIntent pendingIntent = PendingIntent.getActivity(context, notifyId /* Request code */, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Bundle msgNotifyBundle = new Bundle();
        msgNotifyBundle.putInt("notifyId", notifyId);

        //通知清除事件
        Intent intentCancel = new Intent(context, SysNotificationBroadcastReceiver.class);
        intentCancel.setAction("sys_notification_cancelled");
        intentCancel.putExtras(msgNotifyBundle);
        PendingIntent pendingIntentCancel = PendingIntent.getBroadcast(context, notifyId, intentCancel, PendingIntent.FLAG_ONE_SHOT);

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
                largeIcon = BitmapFactory.decodeResource(context.getResources(), R.mipmap.qq);
                break;
            case "2":
                smallIcon = R.drawable.weixin_notification;
                largeIcon = BitmapFactory.decodeResource(context.getResources(), R.mipmap.weixin);
                break;
            default:
                smallIcon = R.drawable.sys_notification;
                largeIcon = BitmapFactory.decodeResource(context.getResources(), R.mipmap.sys);
        }

        int defaults = 0;
        defaults |= Notification.DEFAULT_LIGHTS;
        defaults |= Notification.DEFAULT_VIBRATE;
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(smallIcon)
                .setLargeIcon(largeIcon)
                .setTicker(tickerSys)
                .setContentTitle(msgTitle)
                .setStyle(new NotificationCompat.BigTextStyle() // 设置通知样式为大型文本样式
                        .bigText(msgBody))
                .setContentText(msgBody)
                .setSubText(context.getString(R.string.notification_group_sys_name))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setDefaults(defaults)
                .setContentIntent(pendingIntent)
                .setDeleteIntent(pendingIntentCancel);
        notificationBuilder.setPriority(Notification.PRIORITY_HIGH); //自动弹出通知
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setColor(context.getResources().getColor(R.color.colorPrimary));
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationBuilder.setChannelId(context.getString(R.string.notification_channel_sys_id));
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
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
    private static class MsgThread extends Thread
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
   private static class userThread extends Thread
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
