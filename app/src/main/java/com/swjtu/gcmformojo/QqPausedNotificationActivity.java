package com.swjtu.gcmformojo;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import java.util.Date;

//向配置文件写入一个值（下一次开始推送时间，当前时间加上暂停时间）
//如果接收到消息，则判断是否满足条件，满足则不推送，否则推送
public class QqPausedNotificationActivity extends Activity
{
    private int checked = R.id.paused_one_hour;
    private RadioGroup radioGroup;
    private Button btn_cancel;
    private Button btn_done;
    private EditText inputLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        initialization();
        monitor();
    }

    private void initialization()
    {
        setContentView(R.layout.activity_qq_paused_notification);
        radioGroup = (RadioGroup) findViewById(R.id.radio_group);
        btn_cancel = (Button) findViewById(R.id.btn_cancel);
        btn_done = (Button) findViewById(R.id.btn_done);
        inputLayout = (EditText) findViewById(R.id.input_oneself);
    }

    private void monitor()
    {
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId)
            {
                checked = checkedId;
                //判断是否选择自定义
                if (checkedId == R.id.paused_oneself)
                {
                    inputLayout.setVisibility(View.VISIBLE);
                } else
                {
                    inputLayout.setVisibility(View.GONE);
                }
            }
        });
        btn_cancel.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();
            }
        });
        btn_done.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                SharedPreferences.Editor editor = getSharedPreferences("paused_time", MODE_PRIVATE).edit();
                long time = new Date().getTime();
                switch (checked)
                {
                    case R.id.paused_one_hour://1小时
                        time += 3600000;
                        break;
                    case R.id.paused_two_hour://2小时
                        time += 7200000;
                        break;
                    case R.id.paused_six_hour://6小时
                        time += 21600000;
                        break;
                    case R.id.paused_one_day://1天
                        time += 86400000;
                        break;
                    case R.id.paused_oneself://自定义
                        if (isFormat())
                        {
                            int minutes = Integer.parseInt(inputLayout.getText().toString());
                            time += (60000 * minutes);
                        }
                        break;
                }
                editor.putLong("paused_time", time);
                editor.apply();
                finish();
            }
        });
        inputLayout.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
            }

            @Override
            public void afterTextChanged(Editable editable)
            {
                isFormat();
            }
        });
    }

    private boolean isFormat()
    {
        if (inputLayout.getText().length() == 0)
        {
            inputLayout.setError(getString(R.string.paused_oneself_null));
            return false;
        } else if (Integer.parseInt(inputLayout.getText().toString()) > 14400)
        {
            inputLayout.setError(getString(R.string.paused_oneself_error));
            return false;
        }
        return true;
    }
}
