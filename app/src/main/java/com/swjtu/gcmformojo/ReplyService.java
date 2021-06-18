package com.swjtu.gcmformojo;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.text.Spanned;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.app.Notification.DEFAULT_LIGHTS;
import static com.swjtu.gcmformojo.DialogActivity.doGetRequestResutl;
import static com.swjtu.gcmformojo.DialogActivity.getMD5;
import static com.swjtu.gcmformojo.MyApplication.PREF;
import static com.swjtu.gcmformojo.MyApplication.QQ;
import static com.swjtu.gcmformojo.MyApplication.WEIXIN;
import static com.swjtu.gcmformojo.MyApplication.getColorMsgTime;
import static com.swjtu.gcmformojo.MyApplication.getCurTime;
import static com.swjtu.gcmformojo.MyApplication.toSpannedMessage;

/**
 * 通知栏直接回复信息专用
 * Created by Think on 2018/2/5.
 */

public class ReplyService extends IntentService {

    private static final String KEY_TEXT_REPLY = "key_text_reply";

    private int notifyId;

    private String msgId;
    private String msgTitle;
    private String msgBody;
    private String senderType;
    private String msgType;
    private String msgTime;
    private String wxPackgeName;
    private String qqPackgeName;

    private Map<String, List<Spanned>> msgSave;
    private ArrayList<User> currentUserList;

    private Bundle directReplyBundle;

    public ReplyService() {
        super("ReplyService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (intent == null) {
            return;
        }

        currentUserList = MyApplication.getInstance().getCurrentUserList();

        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        String message = null;
        if (remoteInput != null) {
            message = remoteInput.getCharSequence(KEY_TEXT_REPLY).toString();
        }

        directReplyBundle = intent.getExtras();
        notifyId = directReplyBundle.getInt("notifyId");
        msgId = directReplyBundle.getString("msgId");
        msgTitle =directReplyBundle.getString("msgTitle");
        msgBody =directReplyBundle.getString("msgBody");
        senderType =directReplyBundle.getString("senderType");
        msgType =directReplyBundle.getString("msgType");
        msgTime =directReplyBundle.getString("msgTime");
        if(directReplyBundle.containsKey("qqPackgeName")) qqPackgeName =directReplyBundle.getString("qqPackgeName");
        if(directReplyBundle.containsKey("wxPackgeName"))  wxPackgeName =directReplyBundle.getString("wxPackgeName");

        msgSave = MyApplication.getInstance().getMsgSave();
        sendmsg(message,msgId,senderType,msgType);
    }

    // 由DialogActivity复制而来
    // 用于传递消息及对消息发送情况做出反馈
    private void sendmsg(String msg, String msgId, String senderType, String msgType) {
        if(msg.length()==0) {
            return;
        }
        String sendResult = sendMessage(msg, msgId, senderType, msgType);
        String isSucess;
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        switch (sendResult) {
            case "发送成功":
                isSucess = "";
                notificationManager.cancel(notifyId);
                break;
            case "success":
                isSucess = "";
                notificationManager.cancel(notifyId);
                break;
            default:
                isSucess = "[!"+sendResult+"] ";
                notificationManager.cancel(notifyId);
                Intent intentDialog = new Intent(this, DialogActivity.class);
                intentDialog.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                intentDialog.putExtras(directReplyBundle);
                PendingIntent pendingIntentDialog = PendingIntent.getActivity(this, notifyId, intentDialog, PendingIntent.FLAG_UPDATE_CURRENT);
                NotificationCompat.Builder notificationBuilder = null;
                notificationBuilder = new NotificationCompat.Builder(this)
                        .setContentTitle(getString(R.string.notification_send_fail_title))
                        .setContentText(getString(R.string.notification_send_fail_content))
                        .setAutoCancel(true)
                        .setDefaults(DEFAULT_LIGHTS)
                        .setContentIntent(pendingIntentDialog);
                if (msgType.equals(QQ)) {
                    notificationBuilder.setSmallIcon(R.drawable.qq_notification)
                            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.qq))
                            .setSubText(getString(R.string.notification_group_qq_name))
                            .setGroup(getString(R.string.notification_group_qq_id));
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        notificationBuilder.setColor(getColor(R.color.colorNotification_qq));
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        //根据发送者类别区分通知渠道
                        switch(senderType) {
                            case "1":
                                notificationBuilder.setChannelId(getString(R.string.notification_channel_qq_contact_id));
                                break;
                            case "3":
                                notificationBuilder.setChannelId(getString(R.string.notification_channel_qq_discuss_id));
                                break;
                            case "2":
                                notificationBuilder.setChannelId(getString(R.string.notification_channel_qq_group_id));
                                break;
                        }
                    }

                } else {
                    notificationBuilder.setSmallIcon(R.drawable.weixin_notification)
                            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.weixin))
                            .setSubText(getString(R.string.notification_group_wechat_name))
                            .setGroup(getString(R.string.notification_group_wechat_id));
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        notificationBuilder.setColor(getColor(R.color.colorNotification_wechat));
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        switch (senderType) {
                            case "1":
                                notificationBuilder.setChannelId(getString(R.string.notification_channel_wechat_contact_id));
                                break;
                            case "3":
                                notificationBuilder.setChannelId(getString(R.string.notification_channel_wechat_discuss_id));
                                break;
                            case "2":
                                notificationBuilder.setChannelId(getString(R.string.notification_channel_wechat_group_id));
                                break;
                        }
                    }
                }
                notificationBuilder.setGroupSummary(true);
                notificationManager.notify(notifyId, notificationBuilder.build());
                break;
        }

        //将发送信息加入聊天记录
        Spanned mySendMsg;
        String str = getColorMsgTime(msgType,true);
        mySendMsg=toSpannedMessage( str + isSucess + msg);
        if(msgSave.get(msgId)==null) {
            List<Spanned> msgList = new ArrayList<>();
            msgList.add(mySendMsg);
            msgSave.put(msgId,msgList);
        } else {
            List<Spanned> msgList=msgSave.get(msgId);
            msgList.add(mySendMsg);
            msgSave.put(msgId,msgList);
        }

        //将发送信息加入会话列表
        User currentUser = new User(msgTitle, msgId, msgType,isSucess + msg, getCurTime(), senderType, notifyId,"0");
        for(int    i=0;    i<currentUserList.size();    i++){
            if(currentUserList.get(i).getUserId().equals(msgId)){
                currentUserList.remove(i);
                break;
            }
        }
        currentUserList.add(0,currentUser);

        //更新会话列表界面
        if(CurrentUserActivity.userHandler!=null)
            new userThread().start();

    }

    // 由DialogActivity复制而来
    // 用于最终发送消息
    private String sendMessage(String msgSend, String msgId ,String senderType,String msgType) {

        SharedPreferences Settings = getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String urlServer="";
        String urlType="";
        String urlQX="";
        String urlSend;

        HashMap<String, String> msgSendRequest = new HashMap<>();

        Boolean validationRequired=false;
        String validationSalt="";

        if(msgType.equals(QQ)){
            urlServer=Settings.getString("edit_text_preference_qq_replyurl","");
            urlQX="openqq";
            if(Settings.getBoolean("check_box_preference_qq_validation",false)) {
                validationRequired = true;
                validationSalt=Settings.getString("edit_text_preference_qq_salt","");
            }
        }else if(msgType.equals(WEIXIN)){
            urlServer=Settings.getString("edit_text_preference_wx_replyurl","");
            urlQX="openwx";
            if(Settings.getBoolean("check_box_preference_wx_validation",false)) {
                validationRequired = true;
                validationSalt=Settings.getString("edit_text_preference_wx_salt","");
            }
        }

        switch (senderType) {
            case "1":
                urlType="/"+urlQX+"/send_friend_message";
                break;
            case "2":
                urlType="/"+urlQX+"/send_group_message";
                break;
            case "3":
                urlType="/"+urlQX+"/send_discuss_message";
                break;
        }

        urlSend=urlServer+urlType;
        msgSendRequest.put("id",msgId);
        msgSendRequest.put("content",msgSend);
        if(validationRequired) {
            String sign="";
            try{
                sign=getMD5(msgSend+msgId+validationSalt);
            }catch(Exception e){
                e.printStackTrace();
            }
            msgSendRequest.put("sign",sign);
        }
        return doGetRequestResutl(urlSend,msgSendRequest);

    }

    class userThread extends Thread {
        @Override
        public void run() {
            Message msg = new Message();
            msg.obj = "UpdateCurrentUserList";
            CurrentUserActivity.userHandler.sendMessage(msg);
            super.run();
        }
    }
}
