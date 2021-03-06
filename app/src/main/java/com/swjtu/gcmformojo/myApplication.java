package com.swjtu.gcmformojo;

import android.app.Application;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;

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
    final public static String QQ="Mojo-Webqq";
    final public static String WEIXIN="Mojo-Weixin";
    final public static String SYS="Mojo-Sys";
    final public static String qqColor="#1296DB";
    final public static String wxColor="#62B900";

    private final Map<String, List<Spanned>> msgSave = new HashMap<>();
    private final Map<Integer, Integer> msgCountMap = new HashMap<>();
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
            if(messageType.equals("Mojo-Webqq")){
                str    =    "<font color='"+qqColor+"'><small>"+ getCurTime()+"</small></font><br>";
            }else if(messageType.equals("Mojo-Weixin")){
                str    =    "<font color='"+wxColor+"'><small>"+ getCurTime()+"</small></font><br>";
            }
        }else {
            if(messageType.equals("Mojo-Webqq")){
                str    =    "<font color='"+wxColor+"'><small>"+ getCurTime()+"</small></font><br>";
            }else if(messageType.equals("Mojo-Weixin")){
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

    public static Spanned toSpannedMessage(String message){

        Spanned tmpMsg;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tmpMsg= Html.fromHtml(message,FROM_HTML_MODE_COMPACT);
        } else {
            tmpMsg=Html.fromHtml(message);
        }

        return tmpMsg;

    }

    @Deprecated
    public static String msgColor(String message,String messageType,Boolean isSend) {

        String str = "";
        if(!isSend) {
            if (messageType.equals(QQ)) {
                str = "<font color='"+qqColor+"'>" + message + "</font>";
            } else if (messageType.equals(WEIXIN)) {
                str = "<font color='"+wxColor+"'>" + message + "</font>";
            }
        }else {
            if (messageType.equals(QQ)) {
                str = "<font color='"+wxColor+"'>" + message + "</font>";
            } else if (messageType.equals(WEIXIN)) {
                str = "<font color='"+qqColor+"'>" + message + "</font>";
            }
        }
        return str;
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        //初始化全局变量
        myApp = this;
    }

}
