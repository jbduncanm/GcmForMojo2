package com.swjtu.gcmformojo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.iid.FirebaseInstanceId;
import com.huawei.android.pushagent.api.PushManager;

import static com.swjtu.gcmformojo.MyApplication.PREF;
import static com.swjtu.gcmformojo.MyApplication.deviceGcmToken;
import static com.swjtu.gcmformojo.MyApplication.deviceHwToken;
import static com.swjtu.gcmformojo.MyApplication.deviceMiToken;


public class TokenActivity extends Activity {


    private TextView myTokenSender;
    private TextView myToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token);

        myTokenSender = (TextView) findViewById(R.id.textView_sender);
        myToken = (TextView) findViewById(R.id.myToken);


    }

    @Override
    protected void onResume() {

        super.onResume();
        String tokenNo = "尚未注册成功，稍后再试！";

        SharedPreferences Settings =        getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String tokenSender = Settings.getString("push_type","GCM");

        SharedPreferences miSettings =        getSharedPreferences("mipush", Context.MODE_PRIVATE);
        deviceMiToken = miSettings.getString("regId",deviceMiToken);

       // getMyToken();



        myTokenSender.setText(tokenSender);

        switch (tokenSender) {
            case "GCM":
                deviceGcmToken = FirebaseInstanceId.getInstance().getToken();
                if(deviceGcmToken !=null)
                    myToken.setText(deviceGcmToken);
                else {
                    myToken.setText(tokenNo);
                }
                break;
            case "MiPush":
                if(deviceMiToken!=null)
                    myToken.setText(deviceMiToken);
                else
                    myToken.setText(tokenNo);
                break;
            case "HwPush":
                PushManager.requestToken(this);
                if(deviceHwToken!=null)
                    myToken.setText(deviceHwToken);
                else {
                    myToken.setText(tokenNo);
                }
                break;
        }


    }

    public void onTitleClick(View view) {
    }

}
