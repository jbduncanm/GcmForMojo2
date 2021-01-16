package com.swjtu.gcmformojo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

import static com.swjtu.gcmformojo.MyFirebaseMessagingService.QQ;
import static com.swjtu.gcmformojo.MyFirebaseMessagingService.SYS;
import static com.swjtu.gcmformojo.MyFirebaseMessagingService.WEIXIN;
import static com.swjtu.gcmformojo.MyFirebaseMessagingService.curTime;
import static com.swjtu.gcmformojo.MyFirebaseMessagingService.currentUserAdapter;

public class CurrentUserActivity extends AppCompatActivity {

    final public static ArrayList<User> currentUserList = new ArrayList<>();
    public static Handler userHandler;
    public ListView currentUserListView;

    public ArrayList<User> currentUserListTest = new ArrayList<>();

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    final private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_user);
        verifyStoragePermissions(this);

        userHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {

                if(currentUserAdapter!=null){
                    currentUserAdapter.notifyDataSetChanged();
                }

                super.handleMessage(msg);
            }
        };

        SharedPreferences Settings = getSharedPreferences("com.swjtu.gcmformojo_preferences", Context.MODE_PRIVATE);
        Boolean qqIsReply=Settings.getBoolean("check_box_preference_qq_reply",false);
        final String qqReplyUrl=Settings.getString("edit_text_preference_qq_replyurl","");
        final String wxReplyUrl=Settings.getString("edit_text_preference_wx_replyurl","");
        final  String qqPackgeName=Settings.getString("edit_text_preference_qq_packgename","com.tencent.mobileqq");
        final  String wxPackgeName=Settings.getString("edit_text_preference_wx_packgename","com.tencent.mm");

        currentUserListView = (ListView) findViewById(R.id.current_user_list_view);
        addNotfiyContent();

        currentUserAdapter = new UserAdapter(CurrentUserActivity.this,R.layout.current_userlist_item,currentUserList);
        currentUserListView.setAdapter(currentUserAdapter);
        currentUserListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        currentUserListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                User p=(User) parent.getItemAtPosition(position);
                Intent intentReply = new Intent(getApplicationContext(), DialogActivity.class);
                intentReply.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intentReply.putExtra(DialogActivity.msgIdReply, p.getUserId());
                intentReply.putExtra(DialogActivity.qqReplyUrl, qqReplyUrl);
                intentReply.putExtra(DialogActivity.wxReplyUrl, wxReplyUrl);
                intentReply.putExtra(DialogActivity.ReplyType, p.getSenderType());
                intentReply.putExtra(DialogActivity.msgType, p.getUserType());
                intentReply.putExtra(DialogActivity.messageTitle, p.getUserName());
                intentReply.putExtra(DialogActivity.messageBody, p.getUserMessage());
                intentReply.putExtra(DialogActivity.NotificationId, p.getNotificationId());
                intentReply.putExtra(DialogActivity.RecivedTime, p.getUserTime());
                intentReply.putExtra(DialogActivity.qqPackgeName, qqPackgeName);
                intentReply.putExtra(DialogActivity.wxPackgeName, wxPackgeName);

                startActivity(intentReply);
            }
        });

        currentUserListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent,View view,int position,long id) {

                //系统信息不能删除
                if(!currentUserList.get(position).getUserId().equals("0") && !currentUserList.get(position).getUserId().equals("1") && !currentUserList.get(position).getUserId().equals("2")) {
                    currentUserList.remove(position);
                    currentUserAdapter.notifyDataSetChanged();
                }
                return true;//当返回true时,不会触发短按事件
                //return false;//当返回false时,会触发短按事件
            }
        });
    }

    public void addNotfiyContent() {

        //点击通知增加会话内容，用于缓存被杀时列表内容为空的情况，使用通知自带的最后一条消息

        Intent intentCurrentListUser = getIntent();
        if(intentCurrentListUser!=null) {
            Bundle msgBundle = intentCurrentListUser.getExtras();
            if (msgBundle != null && msgBundle.containsKey("userId")) {

                if (!isHaveMsg(currentUserList, msgBundle.getString("userId"))) {
                    User noifyMsg = new User(msgBundle.getString("userName"), msgBundle.getString("userId"), msgBundle.getString("userType"), msgBundle.getString("userMessage"), msgBundle.getString("userTime"), msgBundle.getString("senderType"), msgBundle.getInt("NotificationId"), msgBundle.getString("msgCount"));
                    currentUserList.add(0, noifyMsg);

                    if (currentUserAdapter != null) {
                        currentUserAdapter.notifyDataSetChanged();
                    }
                }
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch(item.getItemId()) {
            case R.id.action_qq_contacts:
                Intent intentFriend = new Intent(this,QqContactsActivity.class);
                startActivity(intentFriend);
                break;
            case R.id.action_settings:
                Intent intentSettings = new Intent(this, FragmentPreferences.class);
                startActivity(intentSettings);
                break;
            case R.id.action_help:
                Intent intentHelp = new Intent(this, HelpActivity.class);
                startActivity(intentHelp);
                break;
            case R.id.action_token:
                Intent intentToken = new Intent(this, TokenActivity.class);
                startActivity(intentToken);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private static void verifyStoragePermissions(Activity activity) {
        // Check if we have read or write permission
        int writePermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readPermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (writePermission != PackageManager.PERMISSION_GRANTED || readPermission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    public Handler getHandler(){
        return userHandler;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume() {

        super.onResume();
        if (!isHaveMsg(currentUserList,"2"))
            currentUserList.add(new User("微信机器人(未开放)", "2", WEIXIN, "用于控制服务端。", curTime(), "1", 2, "0"));
        if (!isHaveMsg(currentUserList,"1"))
            currentUserList.add(new User("QQ机器人(未开放)", "1", QQ, "用于控制服务端。", curTime(), "1", 1, "0"));
        if (!isHaveMsg(currentUserList,"0"))
            currentUserList.add(new User("欢迎使用GcmForMojo", "0", SYS, "请点击右上角选项获取设备码。", curTime(), "1", 0, "0"));

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        addNotfiyContent();
    }

    public  Boolean  isHaveMsg(final ArrayList<User> userList,final String userId){
        if(userList.size()==0) {
            return false;
        }
        for(int    i=0;    i<userList.size();    i++){
            String str = userList.get(i).getUserId();

            if(str.equals(userId)){
                return true;
            }
        }
        return false;
    }

}
