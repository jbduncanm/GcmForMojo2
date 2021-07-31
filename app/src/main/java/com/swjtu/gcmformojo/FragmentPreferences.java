package com.swjtu.gcmformojo;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.huawei.android.hms.agent.HMSAgent;
import com.huawei.android.hms.agent.common.handler.ConnectHandler;
import com.huawei.android.hms.agent.push.handler.GetTokenHandler;
import com.huawei.hms.support.api.push.TokenResult;
import com.xiaomi.mipush.sdk.MiPushClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import static com.swjtu.gcmformojo.MyApplication.MYTAG;
import static com.swjtu.gcmformojo.MyApplication.PREF;
import static com.swjtu.gcmformojo.MyApplication.deviceGcmToken;
import static com.swjtu.gcmformojo.MyApplication.getInstance;
import static com.swjtu.gcmformojo.MyApplication.miSettings;
import static com.swjtu.gcmformojo.MyApplication.mi_APP_ID;
import static com.swjtu.gcmformojo.MyApplication.mi_APP_KEY;
import static com.swjtu.gcmformojo.MyApplication.mySettings;

/**
 * Created by HeiPi on 2017/2/1.
 * 加载设置资源
 */

public class FragmentPreferences extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragement()).commit();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //激活推送通道
        mySettings = getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String pushType=mySettings.getString("push_type","GCM");
        switch (pushType){
            case "GCM":
                deviceGcmToken = FirebaseInstanceId.getInstance().getToken();
                stopMiPush();
                stopHwPush();
                Log.e(MYTAG, "使用GCM推送");
                break;
            case "MiPush":
                if(shouldInit()) {
                    MiPushClient.registerPush(this, mi_APP_ID, mi_APP_KEY);
                }
                miSettings = getSharedPreferences("mipush", Context.MODE_PRIVATE);
                stopHwPush();
                // MiPushClient.enablePush(getInstance().getApplicationContext());
                Log.e(MYTAG, "使用MiPush推送");
                break;
            case "HwPush":
                HMSAgent.init(this);
                HMSAgent.connect(this, new ConnectHandler() {
                    @Override
                    public void onConnect(int rst) {
                        //Log.e("HMS connect end:" + rst);
                    }
                });
                HMSAgent.Push.getToken(new GetTokenHandler() {
                    public void onResult(int rtnCode, TokenResult tokenResult) {
                        //Log.e("get token: end" + rtnCode);
                    }
                });
                stopMiPush();
                Log.e(MYTAG, "使用HwPush推送");
                break;
            default:
                deviceGcmToken = FirebaseInstanceId.getInstance().getToken();
                stopMiPush();
                stopHwPush();
                Log.e(MYTAG, "默认DefaultGCM推送");
                break;

        }
    }

    private void stopMiPush () {
        if(!isMiUi()) {
            Intent intent = new Intent("com.xiaomi.push.service.XMPushService");
            intent.setPackage(getPackageName());
            stopService(intent);
        }
    }

    private void stopHwPush () {
        if(!isMiUi()) {
            Intent intent = new Intent("com.huawei.android.pushagent.PushService");
            intent.setPackage(getPackageName());
            stopService(intent);
        }
    }

    private boolean shouldInit() {
        ActivityManager am = ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE));
        List<ActivityManager.RunningAppProcessInfo> processInfos = am.getRunningAppProcesses();
        String mainProcessName = getPackageName();
        int myPid = android.os.Process.myPid();
        for (ActivityManager.RunningAppProcessInfo info : processInfos) {
            if (info.pid == myPid && mainProcessName.equals(info.processName)) {
                return true;
            }
        }
        return false;
    }


    public static boolean isMiUi() {
        return !TextUtils.isEmpty(getSystemProperty("ro.miui.ui.version.name"));
    }

    public static String getSystemProperty(String propName) {
        String line;
        BufferedReader input = null;
        try {
            java.lang.Process p = Runtime.getRuntime().exec("getprop " + propName);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            line = input.readLine();
            input.close();
        } catch (IOException ex) {
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return line;
    }


    public static class PrefsFragement extends PreferenceFragment{

        public class PrefListener implements Preference.OnPreferenceChangeListener {
            private String format = null;

            public PrefListener(String key) {
                super();
                Preference preference = findPreference(key);
                format = preference.getSummary().toString();

                if (EditTextPreference.class.isInstance(preference)) {
                    // EditText
                    EditTextPreference etp = (EditTextPreference) preference;
                    onPreferenceChange(preference, etp.getText());
                } else if (ListPreference.class.isInstance(preference)) {
                    // List 切换推送通道 注册及关闭Miui和华为推送
                    ListPreference lp = (ListPreference) preference;
                    onPreferenceChange(preference, lp.getEntry());
                } else {
                    Log.e("GcmForMojoSetting", "不支持的Preference类型");
                }
                preference.setOnPreferenceChangeListener(this);
            }

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(format.replace("{v}", newValue==null?"null":newValue.toString()));
                return true;
            }


        }

        private boolean shouldInit() {
            ActivityManager am = ((ActivityManager) getInstance().getSystemService(Context.ACTIVITY_SERVICE));
            List<ActivityManager.RunningAppProcessInfo> processInfos = am.getRunningAppProcesses();
            String mainProcessName = getInstance().getPackageName();
            int myPid = android.os.Process.myPid();
            for (ActivityManager.RunningAppProcessInfo info : processInfos) {
                if (info.pid == myPid && mainProcessName.equals(info.processName)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            this.addPreferencesFromResource(R.xml.pref_settings);

            //监听QQ设置
            new PrefListener("edit_text_preference_qq_packgename"); //包名
            new PrefListener("edit_text_preference_qq_replyurl"); //服务端地址

            //监听微信设置
            new PrefListener("edit_text_preference_wx_packgename"); //包名
            new PrefListener("edit_text_preference_wx_replyurl"); //服务端地址
        }

    }

}

