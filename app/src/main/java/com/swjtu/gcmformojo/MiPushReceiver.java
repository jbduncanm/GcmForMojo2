package com.swjtu.gcmformojo;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.xiaomi.mipush.sdk.ErrorCode;
import com.xiaomi.mipush.sdk.MiPushClient;
import com.xiaomi.mipush.sdk.MiPushCommandMessage;
import com.xiaomi.mipush.sdk.MiPushMessage;
import com.xiaomi.mipush.sdk.PushMessageReceiver;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import static com.swjtu.gcmformojo.MyApplication.MYTAG;
import static com.swjtu.gcmformojo.MyApplication.PREF;
import static com.swjtu.gcmformojo.MyApplication.deviceMiToken;

/**
 * Created by HeiPi on 2017/3/14.
 */

public class MiPushReceiver extends PushMessageReceiver {

    @Override
    public void onReceiveRegisterResult(Context context, MiPushCommandMessage message) {
        Log.v(MYTAG,
                "onReceiveRegisterResult is called. " + message.toString());
        String command = message.getCommand();
        List<String> arguments = message.getCommandArguments();
        String cmdArg1 = ((arguments != null && arguments.size() > 0) ? arguments.get(0) : null);
        String log;
        if (MiPushClient.COMMAND_REGISTER.equals(command)) {
            if (message.getResultCode() == ErrorCode.SUCCESS) {
                deviceMiToken = cmdArg1;
                Log.v(MYTAG,
                        "小米推送token：" + deviceMiToken);
            } else {
                Log.v(MYTAG,
                        "小米推送注册失败!" );
            }
        } else {
            log = message.getReason();
        }

    }


    @Override
    public void onReceivePassThroughMessage(Context context, MiPushMessage message) {

        String remoteMessageOrign = message.toString();
        Log.d(MYTAG, remoteMessageOrign);

        try
        {
            JSONObject remoteMessage = new JSONObject(message.getContent());

            Log.d(MYTAG, "小米推送原始消息："+remoteMessageOrign);

            SharedPreferences Settings =        context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            String tokenSender = Settings.getString("push_type","GCM");
            if(tokenSender.equals("MiPush"))
            MessageUtil.MessageUtilDo(context,remoteMessage.getString("msgId"),remoteMessage.getString("type"),remoteMessage.getString("senderType"),remoteMessage.getString("title"),remoteMessage.getString("message"),remoteMessage.getString("isAt"));

        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

    }

}

