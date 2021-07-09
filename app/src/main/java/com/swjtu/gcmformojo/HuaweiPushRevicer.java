/*
 * Copyright (C) Huawei Technologies Co., Ltd. 2016. All rights reserved.
 * See LICENSE.txt for this sample's licensing information.
 */

package com.swjtu.gcmformojo;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.huawei.hms.support.api.push.PushReceiver;

import org.json.JSONObject;

import static com.swjtu.gcmformojo.MyApplication.MYTAG;
import static com.swjtu.gcmformojo.MyApplication.deviceHwToken;
import static com.swjtu.gcmformojo.MyApplication.mySettings;

/**
 * 应用需要创建一个子类继承com.huawei.hms.support.api.push.PushReceiver，
 * 实现onToken，onPushState ，onPushMsg，onEvent，这几个抽象方法，用来接收token返回，push连接状态，透传消息和通知栏点击事件处理。
 * onToken 调用getToken方法后，获取服务端返回的token结果，返回token以及belongId
 * onPushState 调用getPushState方法后，获取push连接状态的查询结果
 * onPushMsg 推送消息下来时会自动回调onPushMsg方法实现应用透传消息处理。本接口必须被实现。 在开发者网站上发送push消息分为通知和透传消息
 *           通知为直接在通知栏收到通知，通过点击可以打开网页，应用 或者富媒体，不会收到onPushMsg消息
 *           透传消息不会展示在通知栏，应用会收到onPushMsg
 * onEvent 该方法会在设置标签、点击打开通知栏消息、点击通知栏上的按钮之后被调用。由业务决定是否调用该函数。
 */
public class HuaweiPushRevicer extends PushReceiver {

    public static final String TAG = "HuaweiPushRevicer";

    public static final String ACTION_UPDATEUI = "action.updateUI";

    public static String token;

    @Override
    public void onToken(Context context, String tokenIn, Bundle extras) {
        String belongId = extras.getString("belongId");
        String content = "华为推送token：" + token;
        token = tokenIn;
        deviceHwToken = token;
        Log.d(MYTAG, content);
    }

    @Override
    public boolean onPushMsg(Context context, byte[] msg, Bundle bundle) {
        try {
            //CP可以自己解析消息内容，然后做相应的处理
            String remoteMessageOrign = new String(msg, "UTF-8");
            JSONObject remoteMessage = new JSONObject(remoteMessageOrign);

            if(!remoteMessage.has("isAt")) remoteMessage.put("isAt","0");
            if(!remoteMessage.has("senderType")) remoteMessage.put("senderType","1");

            //SharedPreferences Settings =        context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            String tokenSender = mySettings.getString("push_type","GCM");

            if(tokenSender.equals("HwPush")) {
                Log.d(MYTAG, "华为推送："+remoteMessageOrign);
                MessageUtil.MessageUtilDo(context,remoteMessage.getString("msgId"),remoteMessage.getString("type"),remoteMessage.getString("senderType"),remoteMessage.getString("title"),remoteMessage.getString("message"),remoteMessage.getString("isAt"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void onEvent(Context context, Event event, Bundle extras) {
        if (Event.NOTIFICATION_OPENED.equals(event) || Event.NOTIFICATION_CLICK_BTN.equals(event)) {
            int notifyId = extras.getInt(BOUND_KEY.pushNotifyId, 0);
            Intent intent = new Intent();
            intent.setAction(ACTION_UPDATEUI);
            intent.putExtra("log", "收到通知栏消息点击事件,notifyId:" + notifyId);
            context.sendBroadcast(intent);
            if (0 != notifyId) {
                NotificationManager manager = (NotificationManager) context
                        .getSystemService(Context.NOTIFICATION_SERVICE);
                manager.cancel(notifyId);
            }
        }

        String message = extras.getString(BOUND_KEY.pushMsgKey);
        super.onEvent(context, event, extras);
    }

    @Override
    public void onPushState(Context context, boolean pushState) {

        Intent intent = new Intent();
        intent.setAction(ACTION_UPDATEUI);
        intent.putExtra("log", "Push连接状态为:" + pushState);
        context.sendBroadcast(intent);
    }
}
