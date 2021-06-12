package com.swjtu.gcmformojo;

import android.content.Context;
import android.util.Log;

import com.meizu.cloud.pushinternal.DebugLogger;
import com.meizu.cloud.pushsdk.MzPushMessageReceiver;
import com.meizu.cloud.pushsdk.platform.message.PushSwitchStatus;
import com.meizu.cloud.pushsdk.platform.message.RegisterStatus;
import com.meizu.cloud.pushsdk.platform.message.SubAliasStatus;
import com.meizu.cloud.pushsdk.platform.message.SubTagsStatus;
import com.meizu.cloud.pushsdk.platform.message.UnRegisterStatus;

import org.json.JSONObject;

import static com.swjtu.gcmformojo.MyApplication.MYTAG;
import static com.swjtu.gcmformojo.MyApplication.deviceFmToken;
import static com.swjtu.gcmformojo.MyApplication.mySettings;

/**
 * Created by HeiPi on 2017/3/16.
 */

public class FmPushReceiver extends MzPushMessageReceiver {

    @Override
    public void onMessage(Context context, String s) {
        //接收服务器推送的消息

        try {
            JSONObject remoteMessage = new JSONObject(s);

            if(!remoteMessage.has("isAt")) remoteMessage.put("isAt","0");
            if(!remoteMessage.has("senderType")) remoteMessage.put("senderType","1");

           // SharedPreferences Settings =        context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            String tokenSender = mySettings.getString("push_type","GCM");
            if(tokenSender.equals("FmPush")) {
                Log.d(MYTAG, "魅族推送："+s);
                MessageUtil.MessageUtilDo(context,remoteMessage.getString("msgId"),remoteMessage.getString("type"),remoteMessage.getString("senderType"),remoteMessage.getString("title"),remoteMessage.getString("message"),remoteMessage.getString("isAt"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    @Deprecated
    public void onRegister(Context context, String pushid) {
        //应用在接受返回的 pushid
    }

    @Override
    @Deprecated
    public void onUnRegister(Context context, boolean b) {
        //调用 PushManager.unRegister(context）方法后，会在此回调反注册状态
    }


    @Override
    public void onPushStatus(Context context,PushSwitchStatus
            pushSwitchStatus) {
        //检查通知栏和透传消息开关状态回调
    }

    @Override
    public void onRegisterStatus(Context context,RegisterStatus
            registerStatus) {
        Log.i(MYTAG, "魅族推送token：" + registerStatus.getPushId());
        deviceFmToken = registerStatus.getPushId();

        //新版订阅回调
    }
    @Override
    public void onUnRegisterStatus(Context context,UnRegisterStatus
            unRegisterStatus) {
        Log.i(MYTAG,"onUnRegisterStatus "+unRegisterStatus);
        //新版反订阅回调
    }
    @Override
    public void onSubTagsStatus(Context context,SubTagsStatus
            subTagsStatus) {
        Log.i(MYTAG, "onSubTagsStatus " + subTagsStatus);
        //标签回调
    }
    @Override
    public void onSubAliasStatus(Context context,SubAliasStatus
            subAliasStatus) {
        Log.i(MYTAG, "onSubAliasStatus " + subAliasStatus);
        //别名回调
    }
    @Override
    public void onNotificationArrived(Context context, String title, String
            content, String selfDefineContentString) {
        //通知栏消息到达回调
        DebugLogger.i(MYTAG,"onNotificationArrived title "+title + "content"+content + " selfDefineContentString "+selfDefineContentString);
    }
    @Override
    public void onNotificationClicked(Context context, String title, String
            content, String selfDefineContentString) {
        //通知栏消息点击回调
        DebugLogger.i(MYTAG,"onNotificationClicked title "+title + "content"+content + " selfDefineContentString "+selfDefineContentString);
    }

    @Override
    public void onNotificationDeleted(Context context, String title, String
            content, String selfDefineContentString) {
        //通知栏消息删除回调；flyme6 以上不再回调
        DebugLogger.i(MYTAG,"onNotificationDeleted title "+title + "content"+content + " selfDefineContentString "+selfDefineContentString);
    }
}
