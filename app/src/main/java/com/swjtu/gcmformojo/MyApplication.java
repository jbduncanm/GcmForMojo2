package com.swjtu.gcmformojo;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ShortcutManager;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.text.Html;
import android.text.Spanned;
import android.support.multidex.MultiDex;
import android.util.Log;

import com.huawei.android.hms.agent.HMSAgent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.text.Html.FROM_HTML_MODE_COMPACT;
import static android.text.Html.FROM_HTML_MODE_LEGACY;

/**
 * 用于存储全局变量
 * Created by HeiPi on 2017/2/24.
 */

public class MyApplication extends Application {

    final public static String MYTAG = "GcmForMojo";
    final public static String PREF = "com.swjtu.gcmformojo_preferences";
    final public static String QQ="Mojo-Webqq";
    final public static String WEIXIN="Mojo-Weixin";
    final public static String SYS="Mojo-Sys";
    final public static String KEY_TEXT_REPLY="key_text_reply";


    final public static String mi_APP_ID = "2882303761517557334";
    final public static String mi_APP_KEY = "5631755784334";

    final public static String fm_APP_ID = "110370";
    final public static String fm_APP_KEY = "38b8c46a27c84d3881a41adf8aceb6f8";

    final public static String qqColor="#1296DB";
    final public static String wxColor="#62B900";
    final public static String sysReceiveColor="#3F51B5";
    final public static String sysSendColor="#FF4081";

    public static SharedPreferences mySettings;
    public static SharedPreferences miSettings;

    public static String deviceGcmToken;
    public static String deviceMiToken;
    public static String deviceHwToken;
    public static String deviceFmToken;

    public static int isQqOnline = 1;
    public static int isWxOnline = 1;

    private final Map<String, List<Spanned>> msgSave = new HashMap<>();
    private final Map<Integer, Integer> msgCountMap = new HashMap<>();
    private final Map<String, Integer> msgIdMap = new HashMap<>();
    private final ArrayList<User> currentUserList = new ArrayList<>();

    private final ArrayList<QqFriend> qqFriendArrayList = new ArrayList<>();
    private final ArrayList<QqFriendGroup> qqFriendGroups= new ArrayList<>();

    private final ArrayList<WechatFriend> WechatFriendArrayList = new ArrayList<>();
    private final ArrayList<WechatFriendGroup> WechatFriendGroups= new ArrayList<>();

    private static MyApplication myApp;

    public static MyApplication getInstance() {
        return myApp;
    }


    public Map<String, List<Spanned>> getMsgSave () {
        return this.msgSave;
    }

    public Map<Integer, Integer> getMsgCountMap () {
        return this.msgCountMap;
    }

    public Map<String, Integer> getMsgIdMap () {
        return this.msgIdMap;
    }

    public ArrayList<User> getCurrentUserList () {
        return this.currentUserList;
    }

    public ArrayList<QqFriend> getQqFriendArrayList () {
        return this.qqFriendArrayList;
    }

    public ArrayList<QqFriendGroup> getQqFriendGroups () {
        return this.qqFriendGroups;
    }

    public ArrayList<WechatFriend> getWechatFriendArrayList () {
        return this.WechatFriendArrayList;
    }

    public ArrayList<WechatFriendGroup> getWechatFriendGroups () {
        return this.WechatFriendGroups;
    }


    public static String getCurTime(){

        SimpleDateFormat formatter    =   new    SimpleDateFormat    ("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
        Date curDate    =   new    Date(System.currentTimeMillis());//获取当前时间
        return formatter.format(curDate);
    }

    public static String getColorMsgTime(String messageType,Boolean isSend){

        String    str    =    "";
        switch(messageType){
            case SYS:
                if(!isSend) {
                        str    =    "<font color='"+sysReceiveColor+"'><small>"+ getCurTime()+"</small></font><br>";
                }else {
                        str    =    "<font color='"+sysSendColor+"'><small>"+ getCurTime()+"</small></font><br>";
                }
        }
        if(!isSend) {
            if(messageType.equals(QQ)){
                str    =    "<font color='"+qqColor+"'><small>"+ getCurTime()+"</small></font><br>";
            }else if(messageType.equals(WEIXIN)){
                str    =    "<font color='"+wxColor+"'><small>"+ getCurTime()+"</small></font><br>";
            }
        }else {
            if(messageType.equals(QQ)){
                str    =    "<font color='"+wxColor+"'><small>"+ getCurTime()+"</small></font><br>";
            }else if(messageType.equals(WEIXIN)){
                str    =    "<font color='"+qqColor+"'><small>"+ getCurTime()+"</small></font><br>";
            }
        }
        return str;
    }

    /**
     *  转换文字格式
     *
     *
     */

    public  static Spanned toSpannedMessage(String message){

        Spanned tmpMsg;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tmpMsg= Html.fromHtml(message,FROM_HTML_MODE_COMPACT);
        } else {
            //noinspection deprecation
            tmpMsg=Html.fromHtml(message);
        }

        return tmpMsg;

    }


    @Override
    public void onCreate() {
        super.onCreate();
        //初始化全局变量
        myApp = this;
        miSettings = getSharedPreferences("mipush", Context.MODE_PRIVATE);
        mySettings = getSharedPreferences(PREF, Context.MODE_PRIVATE);

        //华为推送初始化
        String pushType=mySettings.getString("push_type","GCM");
        if(pushType.equals("HwPush")){
            HMSAgent.init(this);
        }

        //初始化通知分组（android O）及通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannelInit(R.string.notification_channel_sys_id,R.string.notification_channel_sys_name,R.string.notification_channel_sys_description,NotificationManager.IMPORTANCE_DEFAULT,Color.YELLOW);
            notificationChannelInit(R.string.notification_channel_qq_at_id,R.string.notification_channel_qq_at_name,R.string.notification_channel_qq_at_description,NotificationManager.IMPORTANCE_HIGH,Color.BLUE);
            notificationChannelInit(R.string.notification_channel_qq_discuss_id,R.string.notification_channel_qq_discuss_name,R.string.notification_channel_qq_discuss_descrption,NotificationManager.IMPORTANCE_DEFAULT,Color.BLUE);
            notificationChannelInit(R.string.notification_channel_qq_contact_id,R.string.notification_channel_qq_contact_name,R.string.notification_channel_qq_contact_descrption,NotificationManager.IMPORTANCE_HIGH,Color.BLUE);
            notificationChannelInit(R.string.notification_channel_qq_group_id,R.string.notification_channel_qq_group_name,R.string.notification_channel_qq_group_descrption,NotificationManager.IMPORTANCE_DEFAULT,Color.BLUE);
            notificationChannelInit(R.string.notification_channel_wechat_at_id,R.string.notification_channel_wechat_at_name,R.string.notification_channel_wechat_at_description,NotificationManager.IMPORTANCE_HIGH,Color.GREEN);
            notificationChannelInit(R.string.notification_channel_wechat_discuss_id,R.string.notification_channel_wechat_discuss_name,R.string.notification_channel_wechat_discuss_descrption,NotificationManager.IMPORTANCE_DEFAULT,Color.GREEN);
            notificationChannelInit(R.string.notification_channel_wechat_contact_id,R.string.notification_channel_wechat_contact_name,R.string.notification_channel_wechat_contact_descrption,NotificationManager.IMPORTANCE_HIGH,Color.GREEN);
            notificationChannelInit(R.string.notification_channel_wechat_group_id,R.string.notification_channel_wechat_group_name,R.string.notification_channel_wechat_group_descrption,NotificationManager.IMPORTANCE_DEFAULT,Color.GREEN);
            notificationGroupInit(R.string.notification_group_qq_id,R.string.notification_group_qq_name);
            notificationGroupInit(R.string.notification_group_wechat_id,R.string.notification_group_wechat_name);
            notificationGroupInit(R.string.notification_group_sys_id,R.string.notification_group_sys_name);
        }

    }

    // 提取微信UID中的数字并进行字符数量削减，以兼容notifyID
    // created by Alex Wang at 20180205
    public static int WechatUIDConvert(String UID) {
        String str = UID;
        str=str.trim();
        String str2="";
        if(str != null && !"".equals(str)){
            for(int i=0;i<str.length();i++) {
                if (str.charAt(i) >= 48 && str.charAt(i) <= 57) {
                    str2 += str.charAt(i);
                }
            }
        }
        //针对系统账号进行优化，以防闪退
        switch (str) {
            case "newsapp":
                str2 = "639727700";
                break;
            case "filehelper":
                str2 = "345343573";
                break;
        }
        return Integer.parseInt(str2.substring(0,9));
    }

    //为Kitkat及更低版本启用multidex支持
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    //首次运行时的通知渠道初始化
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void notificationChannelInit(int id_input, int name_input, int description_input, int importance_input,int color_input) {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // 通知渠道的id
        String id = getString(id_input);
        // 用户可以看到的通知渠道的名字.
        CharSequence name = getString(name_input);
        // 用户可以看到的通知渠道的描述
        String description = getString(description_input);
        int importance = importance_input;
        NotificationChannel mChannel = new NotificationChannel(id, name, importance);
        // 配置通知渠道的属性
        mChannel.setDescription(description);
        // 设置通知出现时的闪灯（如果 android 设备支持的话）
        mChannel.enableLights(true);
        mChannel.setLightColor(color_input);
        mNotificationManager.createNotificationChannel(mChannel);
    }

    //通知分组初始化
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void notificationGroupInit(int group_id, int group_name) {
        // 通知渠道组的id.
        String group = getString(group_id);
        // 用户可见的通知渠道组名称.
        CharSequence name = getString(group_name);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.createNotificationChannelGroup(new NotificationChannelGroup(group, name));
    }

}
