package com.swjtu.gcmformojo;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import static com.swjtu.gcmformojo.MyApplication.MYTAG;
import static com.swjtu.gcmformojo.MyApplication.PREF;


public class MyFirebaseMessagingService extends FirebaseMessagingService
{
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

        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        //handler = new Handler(Looper.getMainLooper()); // 使用应用的主消息循环


        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null)
        {
            Log.d(MYTAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        if (remoteMessage.getData().size() > 0)
        {
            if(!remoteMessage.getData().containsKey("isAt")) remoteMessage.getData().put("isAt","0");
            if(!remoteMessage.getData().containsKey("senderType")) remoteMessage.getData().put("senderType","1");

            SharedPreferences Settings = getSharedPreferences(PREF, Context.MODE_PRIVATE);
            String tokenSender = Settings.getString("push_type","GCM");
            if(tokenSender.equals("GCM")) {
                Log.d(MYTAG, "谷歌推送: " + remoteMessage.getData());
                MessageUtil.MessageUtilDo(getApplicationContext(),remoteMessage.getData().get("msgId"),remoteMessage.getData().get("type"),remoteMessage.getData().get("senderType"),remoteMessage.getData().get("title"),remoteMessage.getData().get("message"),remoteMessage.getData().get("isAt"));
            }
        }
    }
}
