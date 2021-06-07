package com.swjtu.gcmformojo;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import java.util.List;

import static com.swjtu.gcmformojo.MyApplication.getInstance;

/**
 * Created by HeiPi on 2017/2/1.
 * 加载设置资源
 */

public class FragmentPreferences extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragement()).commit();
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

