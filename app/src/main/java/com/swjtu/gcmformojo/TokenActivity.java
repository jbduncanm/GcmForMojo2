package com.swjtu.gcmformojo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.iid.FirebaseInstanceId;

public class TokenActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token);
        String tokenNo = "尚未注册成功，稍后再试！";
        String token = FirebaseInstanceId.getInstance().getToken();

        TextView myTokenSender = (TextView) findViewById(R.id.textView_sender);
        TextView myToken = (TextView) findViewById(R.id.myToken);


        myTokenSender.setText(R.string.action_token);
        if (token == null) {

            myToken.setText(tokenNo);
        }else{

            myToken.setText(token);
        }
    }

    public void onTitleClick(View view) {
    }
}
