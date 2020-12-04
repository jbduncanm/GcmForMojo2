package com.swjtu.gcmformojo;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;

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

       // setContentView(R.layout.layout_settings);
    }


    public static class PrefsFragement extends PreferenceFragment{
        @Override
        public void onCreate(Bundle savedInstanceState) {
            // TODO Auto-generated method stub
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_settings);

           // addPreferencesFromResource(R.xml.preferences);

        }
    }
}
