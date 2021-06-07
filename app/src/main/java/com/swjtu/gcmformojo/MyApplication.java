package com.swjtu.gcmformojo;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;

import com.google.firebase.iid.FirebaseInstanceId;
import com.huawei.android.pushagent.api.PushManager;
import com.xiaomi.mipush.sdk.MiPushClient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.text.Html.FROM_HTML_MODE_COMPACT;

/**
 * 用于存储全局变量
 * Created by HeiPi on 2017/2/24.
 */

public class MyApplication extends Application {

    final public static  String MYTAG = "GcmForMojo";
    final public static String PREF = "com.swjtu.gcmformojo_preferences";
    final public static String QQ="Mojo-Webqq";
    final public static String WEIXIN="Mojo-Weixin";
    final public static String SYS="Mojo-Sys";
    final public static String qqColor="#1296DB";
    final public static String wxColor="#62B900";

    public static String deviceGcmToken;
    public static String deviceMiToken;
    public static String deviceHwToken;

    public static int isQqOnline = 1;
    public static int isWxOnline = 1;

    private final Map<String, List<Spanned>> msgSave = new HashMap<>();
    private final Map<Integer, Integer> msgCountMap = new HashMap<>();
    private final Map<String, Integer> msgIdMap = new HashMap<>();
    private final ArrayList<User> currentUserList = new ArrayList<>();

    private final ArrayList<QqFriend> qqFriendArrayList = new ArrayList<>();
    private final ArrayList<QqFriendGroup> qqFriendGroups= new ArrayList<>();

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




    public static String getCurTime(){

        SimpleDateFormat formatter    =   new    SimpleDateFormat    ("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
        Date curDate    =   new    Date(System.currentTimeMillis());//获取当前时间
        return formatter.format(curDate);
    }

    public static String getColorMsgTime(String messageType,Boolean isSend){

        String    str    =    "";
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


    private   void getMyToken() {

        SharedPreferences Settings =   getSharedPreferences(PREF, Context.MODE_PRIVATE);

        String mi_APP_ID = "2882303761517557334";
        String mi_APP_KEY = "5631755784334";

        //获取并显示最新注册码
        String pushType=Settings.getString("push_type","GCM");

        switch (pushType) {
            case "GCM":
                deviceGcmToken = FirebaseInstanceId.getInstance().getToken();
                break;
            case "MiPush":
                //接收器中更新deviceToken
                MiPushClient.registerPush(this, mi_APP_ID, mi_APP_KEY);
                break;
            case "HwPush":
                //接收器中获得deviceToke
                PushManager.requestToken(this);
                break;
            default:
                deviceGcmToken = FirebaseInstanceId.getInstance().getToken();
        }


    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        //初始化全局变量
        myApp = this;

        //设置推送通道并注册token

        getMyToken();



    }

}
