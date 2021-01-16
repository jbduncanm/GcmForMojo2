package com.swjtu.gcmformojo;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.iid.FirebaseInstanceId;

public class HelpActivity extends AppCompatActivity {

    private static final String TAG = "HelpActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_main);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);
        TextView textView_Version;
        textView_Version = (TextView) findViewById(R.id.textVesion);
        textView_Version.setText(getVersion());

        Button logTokenButton = (Button) findViewById(R.id.button2);

        logTokenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get token
                String tokenNo = "尚未注册成功，稍后再试！";
                String token = FirebaseInstanceId.getInstance().getToken();

                TextView textView5 = (TextView) findViewById(R.id.textView5);

                if (token == null) {
                    Log.d(TAG, tokenNo);
                    textView5.setText(tokenNo);
                }else{
                    Log.d(TAG, token);
                    textView5.setText(token);
                }
            }
        });


    }


    /**
     * 获取版本号
     * @return 当前应用的版本号
     */
    public String getVersion() {
        try {
            PackageManager manager = this.getPackageManager();
            PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
            return "("+info.versionName+" By jklmn of SWJTU)";
        } catch (Exception e) {
            e.printStackTrace();
            return this.getString(R.string.can_not_find_version_name);
        }
    }


}
