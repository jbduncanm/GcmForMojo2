package com.swjtu.gcmformojo;

/**
 * 微信好友
 * Created by Think on 2018/2/4.
 * 由QqFriend复制粘贴而来，可能会出现一堆Bug或者无用代码
 */

public class WechatFriend {

    private String account;
    private String category;
    private String city;
    private String display;
    private String displayname;
    private String id;
    private String markname;
    private String name;
    private String province;
    private String sex;
    private String signature;
    private String uid;

    public String get_account() {
        return account;
    }
    public  String get_category (){
        return category;
    }
    public String get_city() {
        return city;
    }
    public String get_display () {
        return display;
    }
    public String get_displayname() {
        return displayname;
    }
    public  String get_id (){
        return id;
    }
    public  String get_markname (){
        return markname;
    }
    public  String get_name (){
        return name;
    }
    public  String get_province (){
        return province;
    }
    public String get_sex() {
        return sex;
    }
    public String get_signature() {
        return signature;
    }
    public  String get_uid (){
        return uid;
    }

    public WechatFriend(String account, String category, String city, String display, String displayname, String id, String markname, String name, String province, String sex, String signature, String uid) {

        this.account=account;
        this.category=category;
        this.city=city;
        this.display=display;
        this.displayname=displayname;
        this.id=id;
        this.markname=markname;
        this.name=name;
        this.province=province;
        this.sex=sex;
        this.signature=signature;
        this.uid=uid;

    }

}
