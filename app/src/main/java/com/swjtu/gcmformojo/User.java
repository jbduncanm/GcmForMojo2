package com.swjtu.gcmformojo;

import java.io.Serializable;

/**
 * Created by HeiPi on 2017/2/14.
 * 消息列表数据结构
 */

public class User implements Serializable {

    public User(String userName,String userId,String userType,String userMessage,String userTime,String senderType,int notifyId,String msgCount){

        this.userName=userName;
        this.userId=userId;
        this.userType=userType;
        this.userMessage=userMessage;
        this.userTime=userTime;
        this.senderType=senderType;
        this.notifyId =notifyId;
        this.msgCount=msgCount;

    }

    private String userName;
    private String userId;
    private String userType;
    private String userMessage;
    private String userTime;
    private String senderType;
    private int notifyId;
    private String msgCount;

    public  String getUserName (){
        return userName;
    }
    public  String getUserId (){
        return userId;
    }
    public  String getUserType (){
        return userType;
    }
    public  String getUserMessage (){
        return userMessage;
    }
    public  String getUserTime (){
        return userTime;
    }
    public  String getSenderType (){
        return senderType;
    }
    public  int getNotifyId(){
        return notifyId;
    }
    public  String getMsgCount (){
        return msgCount;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public void setUserType(String userType) {
        this.userType = userType;
    }
    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }
    public void setUserTime(String userTime) {        this.userTime = userTime;    }
    public void setUSenderType(String senderType) {
        this.senderType = senderType;
    }
    public void setNotifyId(int NotificationId) {
        this.notifyId = NotificationId;
    }
    public void setMsgCount(String msgCount) {
        this.msgCount = msgCount;
    }
}
