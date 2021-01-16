package com.swjtu.gcmformojo;

/**
 * Created by HeiPi on 2017/2/21.
 */

public class QqFriend {
    private String category;
    private String client_type;
    private String face;
    private String flag;
    private String id;
    private String is_vip;
    private String markname;
    private String name;
    private String state;
    private String uid;
    private String vip_level;

    public QqFriend(String category, String client_type, String face, String flag, String id, String is_vip, String markname, String name, String state, String uid, String vip_level) {

        this.category=category;
        this.client_type=client_type;
        this.face=face;
        this.flag=flag;
        this.id=id;
        this.is_vip=is_vip;
        this.markname=markname;
        this.name=name;
        this.state=state;
        this.uid=uid;
        this.vip_level=vip_level;

    }

    public  String get_category (){
        return category;
    }
    public  String get_client_type (){
        return client_type;
    }
    public  String get_face (){
        return face;
    }
    public  String get_flag (){
        return flag;
    }
    public  String get_id (){
        return id;
    }
    public  String get_is_vip (){
        return is_vip;
    }
    public  String get_markname (){
        return markname;
    }
    public  String get_name (){
        return name;
    }
    public  String get_state (){
        return state;
    }
    public  String get_uid (){
        return uid;
    }
    public  String get_vip_level (){
        return vip_level;
    }

}
