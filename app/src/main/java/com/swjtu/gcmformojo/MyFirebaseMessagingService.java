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
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.text.Html;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import static android.app.Notification.DEFAULT_LIGHTS;
import static android.text.Html.FROM_HTML_MODE_COMPACT;
import static com.swjtu.gcmformojo.CurrentUserActivity.currentUserList;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    public static final String MYTAG = "GcmForMojo";

    final public static Map<Integer, Integer> msgCountMap = new HashMap<>();
    final public static Map<String, List<Spanned>> msgSave = new HashMap<>();
    final private static Map<String, Integer> msgIdMap = new HashMap<>();


    //消息类型常量
    final public static String QQ="Mojo-Webqq";
    final public static String WEIXIN="Mojo-Weixin";
    final public static String SYS="Mojo-Sys";

    final public static String qqColor="#1296DB";
    final public static String wxColor="#62B900";

    public static  int isQqOnline=1;
    public static  int isWxOnline=1;


    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
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


        Log.d(MYTAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(MYTAG, "原始信息: " + remoteMessage.getData());


           // Looper.prepare();
          //  Toast.makeText(getApplicationContext(), remoteMessage.getData().get("title"), Toast.LENGTH_LONG).show();
           // Looper.loop();


        }


      // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(MYTAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.

        if (remoteMessage.getData().size() > 0) {

            String msgId;
            String userId;
            String msgType;
            String senderType;
            String msgTitle;
            String msgBody;
            int notifyId;
            int msgCount;

            msgId=remoteMessage.getData().get("msgId");
            msgTitle=remoteMessage.getData().get("title");
            msgBody=remoteMessage.getData().get("message");
            msgType=remoteMessage.getData().get("type");
            senderType=remoteMessage.getData().get("senderType");

            if(msgId==null) msgId="0"; //处理特殊情况
            if(senderType==null) senderType="1"; //处理特殊情况 默认为好友

            userId=msgId;

            //进行通知数据准备
            //利用msgId生成通知id存储到hashmap中全局使用
            if(msgIdMap.get(msgId)==null) {
                if(msgType.equals(QQ))
                {
                    if(msgId.length()>9){
                        notifyId=Integer.parseInt(msgId.substring(0,9));
                    }else{
                        notifyId=Integer.parseInt(msgId);
                    }
                }else if (msgType.equals(WEIXIN)){ //微信的ID用随机数字代替

                        Random random = new Random();
                        notifyId = random.nextInt(10000);

                } else if(msgType.equals(SYS)){

                    notifyId = Integer.parseInt(msgId); //QQ通知为1，微信通知为2

                }else{
                    notifyId = 0; //其它未知类型消息Id设置为0
                }
                msgIdMap.put(msgId,notifyId); //写入msgIdMap
            }else {
                notifyId = msgIdMap.get(msgId);
            }

            //未读消息计数
            if(msgCountMap.get(notifyId)==null || msgCountMap.get(notifyId).equals(0) ){
                msgCount=1;
            }else{
                msgCount=msgCountMap.get(notifyId)+1;
            }
            if(DialogActivity.notifyId==notifyId){  //如果弹出窗口消息id与最新id一致，则将未读消息清0
                msgCount=0;
            }
            msgCountMap.put(notifyId,msgCount);

            //无论任何设置都先存储消息，供对话列表使用,仅更新最后一条消息
            User currentUser = new User(msgTitle,msgId,msgType,msgBody,curTime(),senderType,notifyId,String.valueOf(msgCount));
            for(int    i=0;    i<currentUserList.size();    i++){
               if(currentUserList.get(i).getUserId().equals(msgId)){
                   currentUserList.remove(i);
                   break;
               }
           }
            currentUserList.add(0,currentUser);
            if(CurrentUserActivity.userHandler!=null)
                new userThread().start();

            //存储对话框消息记录：有可能存储后系统回收内存造成点击通知进入列表界面后为空，需要在点击时将相关变量传入会话列表界面。
            Spanned spannedMessage=toSpannedMessage(msgTime(msgType,false)+msgBody);

          if(msgSave.get(msgId)==null) {

                List<Spanned> msgList = new ArrayList<>();
                msgList.add(spannedMessage);
                msgSave.put(msgId,msgList);

            } else {

                List<Spanned> msgList=msgSave.get(msgId);
                msgList.add(spannedMessage);
                msgSave.put(msgId,msgList);

            }
            //如果回复窗口存在则发送handler消息，更新ui界面
            if(DialogActivity.msgHandler!=null)
            new MsgThread().start();

            //提取设置数据
            SharedPreferences Settings = getSharedPreferences("com.swjtu.gcmformojo_preferences", Context.MODE_PRIVATE);
            String qqReciveType=Settings.getString("qq_list_preference_1","1");
            String wxReciveType=Settings.getString("wx_list_preference_1","1");
            Boolean qqIsDetail=Settings.getBoolean("check_box_preference_qq_detail",true);
            Boolean wxIsDetail=Settings.getBoolean("check_box_preference_wx_detail",true);
            String qqSound=Settings.getString("ringtone_preference_qq","");
            String wxSound=Settings.getString("ringtone_preference_wx","");
            String qqVibrate=Settings.getString("qq_list_preference_vibrate","1");
            String wxVibrate=Settings.getString("wx_list_preference_vibrate","1");
            Boolean qqIsReciveGroup=Settings.getBoolean("check_box_preference_qq_isReciveGroup",true);
            Boolean wxIsReciveGroup=Settings.getBoolean("check_box_preference_wx_isReciveGroup",true);

            String qqPackgeName=Settings.getString("edit_text_preference_qq_packgename","com.tencent.mobileqq");
            String wxPackgeName=Settings.getString("edit_text_preference_wx_packgename","com.tencent.mm");


            //  如果当前聊天对象已经弹出窗口则终止通知 最高优先级
            if(DialogActivity.notifyId ==notifyId){
                return;
            }

            //通过设置参数进行通知
            if(msgType.equals(QQ)){

                if(!Settings.getBoolean("check_box_preference_qq",false)){ //关闭推送
                        return;
                }

                //判断是否弹出群消息
                if(!qqIsReciveGroup) {
                    if(senderType.equals("2") || senderType.equals("3")){
                        return;
                    }
                }

                //判断接收方式
                //QQ判断
                if(qqReciveType.equals("1")){//不检测运行状态

                    Log.d(MYTAG, "QQ不检测运行状态！");

                }else if(qqReciveType.equals("2")){ //前台时不推送

                    Boolean isForeground;
                    if(android.os.Build.VERSION.SDK_INT>= Build.VERSION_CODES.LOLLIPOP_MR1) {

                        isForeground = queryAppUsageStats(this,qqPackgeName);
                        if(isForeground) {
                            Log.d(MYTAG, "QQ前台不推送！");
                            return;
                        }

                    }else if(android.os.Build.VERSION.SDK_INT<=Build.VERSION_CODES.KITKAT) {

                        isForeground = BackgroundUtil.getRunningTask(this, qqPackgeName);
                        if(isForeground) {
                            Log.d(MYTAG, "QQ前台不推送！");
                            return;
                        }

                    } else { //对5.0 API 21 单独处理

                        isForeground = BackgroundUtil.getLinuxCoreInfo(this, qqPackgeName);
                        if(isForeground) {
                            Log.d(MYTAG, "QQ前台不推送！");
                            return;
                        }

                    }

                }else if(qqReciveType.equals("3")){ //运行时不推送

                    if(isServiceRunning(this,qqPackgeName)) {
                        Log.d(MYTAG, "QQ运行不推送！");
                        return;
                    }

                }else if(qqReciveType.equals("4")){//启用时不推送

                    if(android.os.Build.VERSION.SDK_INT>= Build.VERSION_CODES.M){
                        UsageStatsManager usageStatsManager= null;
                            usageStatsManager = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
                            if(!usageStatsManager.isAppInactive(qqPackgeName)){
                                Log.d(MYTAG, "QQ启用不推送！");
                                return;
                            }
                    }else  {
                        Looper.prepare();
                        Toast.makeText(getApplicationContext(),"您的系统不支持浅睡模式！", Toast.LENGTH_LONG).show();
                        Looper.loop();
                    }
                }
            }

            //微信判断

            if(msgType.equals(WEIXIN)){

                if(!Settings.getBoolean("check_box_preference_wx",false)){ //关闭推送
                    return;
                }

                //判断是否弹出群消息
                if(!wxIsReciveGroup) {
                    if(senderType.equals("2") || senderType.equals("3")){
                        return;
                    }
                }

                if(wxReciveType.equals("1")){//不检测运行状态

                    Log.d(MYTAG, "微信不检测运行状态！");

                }else if(wxReciveType.equals("2")){ //前台时不推送
                    if(android.os.Build.VERSION.SDK_INT>= Build.VERSION_CODES.LOLLIPOP_MR1) {

                        Boolean isForeground = queryAppUsageStats(this,wxPackgeName);
                        if(isForeground) {
                            Log.d(MYTAG, "微信前台不推送！");
                            return;
                        }

                    }else if(android.os.Build.VERSION.SDK_INT<=Build.VERSION_CODES.KITKAT) {

                        Boolean isForeground = BackgroundUtil.getRunningTask(this, wxPackgeName);
                        if (isForeground) {
                            Log.d(MYTAG, "微信前台不推送！");
                            return;
                        }
                    }else {

                        Boolean isForeground = BackgroundUtil.getLinuxCoreInfo(this, wxPackgeName);
                        if (isForeground) {
                            Log.d(MYTAG, "微信前台不推送！");
                            return;
                        }
                    }

                }else if(wxReciveType.equals("3")){ //运行时不推送

                    if(isServiceRunning(this,wxPackgeName)) {
                        Log.d(MYTAG, "微信运行不推送！");
                        return;
                    }

                }else if(wxReciveType.equals("4")){//启用时不推送

                    if(android.os.Build.VERSION.SDK_INT>= Build.VERSION_CODES.M){
                        UsageStatsManager usageStatsManager= null;
                        usageStatsManager = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);

                            if(!usageStatsManager.isAppInactive(wxPackgeName)){
                                Log.d(MYTAG, "微信启用不推送！");
                                return;
                            }
                    }else  {
                        Looper.prepare();
                        Toast.makeText(getApplicationContext(),"您的系统不支持浅睡模式！", Toast.LENGTH_LONG).show();
                        Looper.loop();
                    }
                }
            }


           //弹出通知
            if(msgType.equals(QQ)) {  //qq通知显示

                if(!qqIsDetail) {
                    msgTitle="联系人";
                    msgBody="你收到了消息！";
                }

                sendNotificationQq(msgTitle,msgBody,notifyId,msgCount,qqSound,qqVibrate,msgId,senderType,qqPackgeName);

            }else if (msgType.equals(WEIXIN)){ //微信通知显示

                if(!wxIsDetail) {
                    msgTitle="联系人";
                    msgBody="你收到了消息！";
                }

                sendNotificationWx(msgTitle,msgBody,notifyId,msgCount,wxSound,wxVibrate,msgId,senderType,wxPackgeName);

            }else if (msgType.equals(SYS)) { //系统通知显示
                //自动下载二维码

                if(msgTitle.contains("二维码事件")) {
                    try {
                        download(this,msgBody);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                //清除会话列表
                if(remoteMessage.getData().get("title").contains("登录")) {
                    if(msgId.equals("1")){
                        for(int    i=0;    i<currentUserList.size();    i++){
                            if(currentUserList.get(i).getUserType().equals("Mojo-Webqq")){
                                currentUserList.remove(i);
                            }
                        }
                    }else if(msgId.equals("2")){
                        for(int    i=0;    i<currentUserList.size();    i++){
                            if(currentUserList.get(i).getUserType().equals("Mojo-Weixin")){
                                currentUserList.remove(i);
                            }
                        }
                    }

                }

                //设置登录成功变量
                if(msgTitle.contains("扫描二维码事件")) {

                    if(msgId.equals("1")){
                        isQqOnline=0;
                    }else if(msgId.equals("2")){
                        isWxOnline=0;
                    }

                }

                if(msgBody.contains("登录成功")) {

                    if(msgId.equals("1")){
                        isQqOnline=1;
                    }else if(msgId.equals("2")){
                        isWxOnline=1;
                    }

                }


                //发出系统通知
                sendNotificationSys(msgTitle, msgBody,msgId,notifyId,msgCount);
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
    private void sendNotificationQq(String msgTitle,String msgBody,int notifyId,int msgCount,String qqSound,String qqVibrate,String msgId,
                                    String senderType,String qqPackgeName) {

        SharedPreferences Settings = getSharedPreferences("com.swjtu.gcmformojo_preferences", Context.MODE_PRIVATE);
        Boolean qqIsReply=Settings.getBoolean("check_box_preference_qq_reply",false);
        Boolean isOpenQq=Settings.getBoolean("check_box_preference_qq_isOpenQq",true);
        String qqReplyUrl=Settings.getString("edit_text_preference_qq_replyurl","");


        Bundle msgNotifyBundle = new Bundle();
        msgNotifyBundle.putInt("notifyId",notifyId);
        msgNotifyBundle.putString("qqPackgeName",qqPackgeName);

        //通知清除事件(接收器)
        Intent intentCancel = new Intent(this, QqNotificationBroadcastReceiver.class);
        intentCancel.setAction("qq_notification_cancelled");
        intentCancel.putExtras(msgNotifyBundle);
        PendingIntent pendingIntentCancel = PendingIntent.getBroadcast(this, notifyId, intentCancel, PendingIntent.FLAG_UPDATE_CURRENT);

        //通知点击事件
        //应用界面 需要传递最后一次消息内容 避免会话列表为空
        Intent intent = new Intent(this, CurrentUserActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Bundle msgListBundle = new Bundle();
        msgListBundle.putString("userName",msgTitle);
        msgListBundle.putString("userId",msgId);
        msgListBundle.putString("userType",QQ);
        msgListBundle.putString("userMessage",msgBody);
        msgListBundle.putString("userTime",curTime());
        msgListBundle.putString("senderType",senderType);
        msgListBundle.putInt("notifyId",notifyId);
        msgListBundle.putString("msgCount",String.valueOf(msgCount));
        intent.putExtras(msgListBundle);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, notifyId, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        //qq界面(接收器)
        Intent intentClick = new Intent(this, QqNotificationBroadcastReceiver.class);
        intentClick.setAction("qq_notification_clicked");
        intentClick.putExtras(msgNotifyBundle);
        PendingIntent pendingIntentClickQq = PendingIntent.getBroadcast(this, notifyId, intentClick, PendingIntent.FLAG_ONE_SHOT);

        StringBuffer ticker = new StringBuffer();
        ticker.append(msgTitle);
        ticker.append("\r\n");
        ticker.append(msgBody);

        if(msgCount!=1){
            msgTitle=msgTitle+"("+msgCount+"条新消息)";
        }

        Uri defaultSoundUri= Uri.parse(qqSound);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.qq_notification)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.qq))
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

        //开启应用界面还是QQ界面
        if(isOpenQq)
            notificationBuilder.setContentIntent(pendingIntentClickQq);
        else
            notificationBuilder.setContentIntent(pendingIntent);

        //自动弹出
        notificationBuilder.setPriority(Notification.PRIORITY_HIGH);

        //振动参数
        if(qqVibrate.equals("1")){
            notificationBuilder.setVibrate(new long[] {0});
        }else if(qqVibrate.equals("2")){
            notificationBuilder.setVibrate(new long[] {0,100,300,100});
        }else if(qqVibrate.equals("3")){
            notificationBuilder.setVibrate(new long[] {0,500,300,500});
        }else {
            notificationBuilder.setVibrate(new long[] {0});
        }

        //快速回复功能
        if(qqIsReply) {
            Intent intentReply = new Intent(this, DialogActivity.class);
            intentReply.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            Bundle msgDialogBundle = new Bundle();
            msgDialogBundle.putString("msgId",msgId);
           // msgDialogBundle.putString("qqReplyUrl",qqReplyUrl);
            msgDialogBundle.putString("senderType",senderType);
            msgDialogBundle.putString("msgType",QQ);
            msgDialogBundle.putString("msgTitle",msgTitle);
            msgDialogBundle.putString("msgBody",msgBody);
            msgDialogBundle.putInt("notifyId",notifyId);
            msgDialogBundle.putString("msgTime",curTime());
            msgDialogBundle.putString("qqPackgeName",qqPackgeName);
            intentReply.putExtras(msgDialogBundle);

            PendingIntent pendingIntentReply = PendingIntent.getActivity(this, notifyId, intentReply, PendingIntent.FLAG_UPDATE_CURRENT);
            notificationBuilder.addAction(0, "回复", pendingIntentReply);
            notificationBuilder.addAction(0, "清除", pendingIntentCancel);
        }

        NotificationManager notificationManager =  (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(notifyId , notificationBuilder.build());

    }

    //微信通知方法
    private void sendNotificationWx(String msgTitle,String msgBody, int notifyId,int msgCount,String wxSound, String wxVibrate, String msgId,
                                    String senderType,String wxPackgeName) {

        SharedPreferences Settings = getSharedPreferences("com.swjtu.gcmformojo_preferences", Context.MODE_PRIVATE);
        Boolean wxIsReply=Settings.getBoolean("check_box_preference_wx_reply",false);
        Boolean isOpenWx=Settings.getBoolean("check_box_preference_wx_isOpenWx",true);
        String wxReplyUrl=Settings.getString("edit_text_preference_wx_replyurl","");

        Bundle msgNotifyBundle = new Bundle();
        msgNotifyBundle.putInt("notifyId",notifyId);
        msgNotifyBundle.putString("wxPackgeName",wxPackgeName);

        //通知清除事件(接收器)
        Intent intentCancel = new Intent(this, WeixinNotificationBroadcastReceiver.class);
        intentCancel.setAction("weixin_notification_cancelled");
        intentCancel.putExtras(msgNotifyBundle);
        PendingIntent pendingIntentCancel = PendingIntent.getBroadcast(this, notifyId, intentCancel, PendingIntent.FLAG_UPDATE_CURRENT);

        //通知点击事件
        //应用界面
        Intent intent = new Intent(this, CurrentUserActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Bundle msgListBundle = new Bundle();
        msgListBundle.putString("userName",msgTitle);
        msgListBundle.putString("userId",msgId);
        msgListBundle.putString("userType",WEIXIN);
        msgListBundle.putString("userMessage",msgBody);
        msgListBundle.putString("userTime",curTime());
        msgListBundle.putString("senderType",senderType);
        msgListBundle.putInt("notifyId",notifyId);
        msgListBundle.putString("msgCount",String.valueOf(msgCount));
        intent.putExtras(msgListBundle);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, notifyId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        //微信界面
        Intent intentClick = new Intent(this, WeixinNotificationBroadcastReceiver.class);
        intentClick.setAction("weixin_notification_clicked");
        intentClick.putExtras(msgNotifyBundle);
        PendingIntent pendingIntentClickWx = PendingIntent.getBroadcast(this, notifyId, intentClick, PendingIntent.FLAG_ONE_SHOT);

        StringBuffer tickerWx = new StringBuffer();
        tickerWx.append(msgTitle);
        tickerWx.append("\r\n");
        tickerWx.append(msgBody);

        if(msgCount!=1){
            msgTitle=msgTitle+"("+msgCount+"条新消息)";
        }

        Uri defaultSoundUri= Uri.parse(wxSound);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.weixin_notification)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.weixin))
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

        if(isOpenWx)
            notificationBuilder.setContentIntent(pendingIntentClickWx);
        else
            notificationBuilder.setContentIntent(pendingIntent);

        //自动弹出
        notificationBuilder.setPriority(Notification.PRIORITY_HIGH);

        //振动方式
        if(wxVibrate.equals("1")){
            notificationBuilder.setVibrate(new long[] {0});
        }else if(wxVibrate.equals("2")){
            notificationBuilder.setVibrate(new long[] {0,100,300,100});
        }else if(wxVibrate.equals("3")){
            notificationBuilder.setVibrate(new long[] {0,500,300,500});
        }else {
            notificationBuilder.setVibrate(new long[] {0});
        }

        //快速回复功能

        if(wxIsReply) {
            Intent intentReply = new Intent(this, DialogActivity.class);
            intentReply.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            Bundle msgDialogBundle = new Bundle();
            msgDialogBundle.putString("msgId",msgId);
           // msgDialogBundle.putString("wxReplyUrl",wxReplyUrl);
            msgDialogBundle.putString("senderType",senderType);
            msgDialogBundle.putString("msgType",WEIXIN);
            msgDialogBundle.putString("msgTitle",msgTitle);
            msgDialogBundle.putString("msgBody",msgBody);
            msgDialogBundle.putInt("notifyId",notifyId);
            msgDialogBundle.putString("msgTime",curTime());
            msgDialogBundle.putString("wxPackgeName",wxPackgeName);
            intentReply.putExtras(msgDialogBundle);

            PendingIntent pendingIntentReply = PendingIntent.getActivity(this, notifyId, intentReply, PendingIntent.FLAG_UPDATE_CURRENT);
            notificationBuilder.addAction(0, "回复", pendingIntentReply);
            notificationBuilder.addAction(0, "清除", pendingIntentCancel);
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(notifyId , notificationBuilder.build());
    }


    //系统通知方法
    private void sendNotificationSys(String msgTitle,String msgBody,String msgId, int notifyId,int msgCount) {
        Intent intent = new Intent(this, CurrentUserActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Bundle msgListBundle = new Bundle();
        msgListBundle.putString("userName",msgTitle);
        msgListBundle.putString("userId",msgId);
        msgListBundle.putString("userType",SYS);
        msgListBundle.putString("userMessage",msgBody);
        msgListBundle.putString("userTime",curTime());
        msgListBundle.putString("senderType","1");
        msgListBundle.putInt("notifyId",notifyId);
        msgListBundle.putString("msgCount",String.valueOf(msgCount));
        intent.putExtras(msgListBundle);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, notifyId /* Request code */, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Bundle msgNotifyBundle = new Bundle();
        msgNotifyBundle.putInt("notifyId",notifyId);

        //通知清除事件
        Intent intentCancel = new Intent(this, SysNotificationBroadcastReceiver.class);
        intentCancel.setAction("sys_notification_cancelled");
        intentCancel.putExtras(msgNotifyBundle);
        PendingIntent pendingIntentCancel = PendingIntent.getBroadcast(this, notifyId, intentCancel, PendingIntent.FLAG_ONE_SHOT);

        StringBuffer tickerSys = new StringBuffer();
        tickerSys.append(msgTitle);
        tickerSys.append("\r\n");
        tickerSys.append(msgBody);

        if(msgCount!=1){
            msgTitle=msgTitle+"("+msgCount+"条新消息)";
        }

        int smallIcon;
        Bitmap largeIcon;

        if(msgId.equals("1")){
            smallIcon=R.drawable.qq_notification;
            largeIcon=BitmapFactory.decodeResource(getResources(),R.mipmap.qq);
        }else if(msgId.equals("2")){
            smallIcon=R.drawable.weixin_notification;
            largeIcon=BitmapFactory.decodeResource(getResources(),R.mipmap.weixin);
        }else {
            smallIcon=R.drawable.sys_notification;
            largeIcon=BitmapFactory.decodeResource(getResources(),R.mipmap.sys);
        }

        int defaults=0;
        defaults |= Notification.DEFAULT_LIGHTS;
        defaults |= Notification.DEFAULT_VIBRATE;
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
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

        NotificationManager notificationManager =  (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(notifyId , notificationBuilder.build());
    }

     /**
     * 判断应用是否已经启动
     * @param context 一个context
     * @param serviceClassName 要判断应用的包名
     * @return boolean
     */
    private static boolean isServiceRunning(Context context ,String serviceClassName){
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo runningServiceInfo : services) {
            if (runningServiceInfo.service.getPackageName().equals(serviceClassName)){
                return true;              }
        }
        return false;
    }

    //下载二维码
   private static String download(Context context, String docUrl) throws Exception {                           /***加载正文***/
        //获取存储卡路径、构成保存文件的目标路径
        String dirName;
        //SD卡具有读写权限、指定附件存储路径为SD卡上指定的文件夹
        dirName = Environment.getExternalStorageDirectory()+"/GcmForMojo/";
        File f = new File(dirName);
        if(!f.exists()){      //判断文件夹是否存在
            f.mkdir();        //如果不存在、则创建一个新的文件夹
        }
        //准备拼接新的文件名
        String[] list = docUrl.split("/");
        String fileName = list[list.length-1];
        String fileNameTemp = fileName;
        fileName = dirName + fileName;
        File file = new File(fileName);
        if(file.exists()){    //如果目标文件已经存在
            file.delete();    //则删除旧文件
        }
        //1K的数据缓冲
        byte[] bs = new byte[1024];
        //读取到的数据长度
        int len;
        try{
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
            while((len = is.read(bs)) != -1){
                os.write(bs,0,len);
            }
            //完毕关闭所有连接
            os.close();
            is.close();
            }catch(MalformedURLException e){
                 //fileName = null;
                 System.out.println("创建URL对象失败");
                 throw e;
            }catch(FileNotFoundException e){
                // fileName = null;
                 System.out.println("无法加载文件");
                 throw e;
            }catch(IOException e){
               //  fileName = null;
                 System.out.println("获取连接失败");
                 throw e;
            }

        // 其次把文件插入到系统图库
        try {
            MediaStore.Images.Media.insertImage(context.getContentResolver(),
                    file.getAbsolutePath(), fileNameTemp, null);
           // Log.d(TAG, file.getAbsolutePath()+fileNameTemp);
        } catch (FileNotFoundException e) {
           e.printStackTrace();
       }

        // 最后通知图库更新
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri uri = Uri.fromFile(new File(fileName));
        intent.setData(uri);
        context.sendBroadcast(intent);

        return fileName;
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
    private static boolean queryAppUsageStats(Context context, String packageName) {
        class RecentUseComparator implements Comparator<UsageStats> {
            @Override
            public int compare(UsageStats lhs, UsageStats rhs) {
                return (lhs.getLastTimeUsed() > rhs.getLastTimeUsed()) ? -1 : (lhs.getLastTimeUsed() == rhs.getLastTimeUsed()) ? 0 : 1;
            }
        }

        RecentUseComparator mRecentComp = new RecentUseComparator();
        UsageStatsManager mUsageStatsManager = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            mUsageStatsManager = (UsageStatsManager) context.getSystemService(USAGE_STATS_SERVICE);
        }

        Calendar calendar = Calendar.getInstance();
        long endTime = calendar.getTimeInMillis();
        calendar.add(Calendar.YEAR, -1);
        long startTime = calendar.getTimeInMillis();

        List<UsageStats> usageStats = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,startTime,endTime);
        if (usageStats == null || usageStats.size() == 0) {
            if (!HavaPermissionForTest(context) ) {
                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);

                Looper.prepare();
                Toast.makeText(context, "权限不够\n请打开手机设置，点击安全-高级，在有权查看使用情况的应用中，为这个App打上勾", Toast.LENGTH_SHORT).show();
                Looper.loop();

            }

            return false;
        }
        Collections.sort(usageStats, mRecentComp);
        String currentTopPackage = usageStats.get(0).getPackageName();
        Log.d(MYTAG,currentTopPackage);

        return currentTopPackage.equals(packageName);
    }

    /**
     * 判断是否有用权限
     *
     * @param context 上下文参数
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static boolean HavaPermissionForTest(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            int mode = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName);
            }
            return (mode == AppOpsManager.MODE_ALLOWED);
        } catch (PackageManager.NameNotFoundException e) {
            return true;
        }
    }


    public static void  clearMsgCount(String msgId,int motifyId){

        msgCountMap.put(motifyId, 0);
        for(int    i=0;    i<currentUserList.size();    i++){
            if(currentUserList.get(i).getUserId().equals(msgId)){
                currentUserList.get(i).setMsgCount("0");
               // if(CurrentUserActivity.userHandler!=null)
              //      new userThread().start();
                break;
            }
        }

    }



    /**
     *  转换文字格式
     *
     *
     */

    public static String curTime(){

        SimpleDateFormat formatter    =   new    SimpleDateFormat    ("yyyy/MM/dd HH:mm:ss",Locale.getDefault());
        Date curDate    =   new    Date(System.currentTimeMillis());//获取当前时间
        return formatter.format(curDate);
    }


    public static Spanned toSpannedMessage(String message){

        Spanned tmpMsg;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tmpMsg=Html.fromHtml(message,FROM_HTML_MODE_COMPACT);
        } else {
            tmpMsg=Html.fromHtml(message);
        }

        return tmpMsg;

    }


    public static String msgTime(String messageType,Boolean isSend){

        String    str    =    "";
        if(!isSend) {
            if(messageType.equals("Mojo-Webqq")){
                str    =    "<font color='"+qqColor+"'><small>"+curTime()+"</small></font><br>";
            }else if(messageType.equals("Mojo-Weixin")){
                str    =    "<font color='"+wxColor+"'><small>"+curTime()+"</small></font><br>";
            }
        }else {
            if(messageType.equals("Mojo-Webqq")){
                str    =    "<font color='"+wxColor+"'><small>"+curTime()+"</small></font><br>";
            }else if(messageType.equals("Mojo-Weixin")){
                str    =    "<font color='"+qqColor+"'><small>"+curTime()+"</small></font><br>";
            }
        }
        return str;
    }


    public static String msgColor(String message,String messageType,Boolean isSend) {

        String str = "";
        if(!isSend) {
            if (messageType.equals("Mojo-Webqq")) {
                str = "<font color='"+qqColor+"'>" + message + "</font>";
            } else if (messageType.equals("Mojo-Weixin")) {
                str = "<font color='"+wxColor+"'>" + message + "</font>";
            }
        }else {
            if (messageType.equals("Mojo-Webqq")) {
                str = "<font color='"+wxColor+"'>" + message + "</font>";
            } else if (messageType.equals("Mojo-Weixin")) {
                str = "<font color='"+qqColor+"'>" + message + "</font>";
            }
        }
        return str;
    }

/*
*子线程处理弹出框通信
*
 */
    class MsgThread extends Thread {
        private Context context;
        private Handler handler;
        public MsgThread(){

        }

        public MsgThread(Context context) {
            this.context = context;
            this.handler = ((DialogActivity) context).getHandler();
        }

        @Override
        public void run() {
            Message msg = new Message();
            msg.obj = "Update msgList";
            DialogActivity.msgHandler.sendMessage(msg);
            super.run();
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
