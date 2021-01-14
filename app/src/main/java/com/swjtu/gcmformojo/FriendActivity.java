package com.swjtu.gcmformojo;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ExpandableListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FriendActivity extends AppCompatActivity {

    public final static ArrayList<Friend> QQ_FRIEND_LIST = new ArrayList<>();

    ExpandableListView qQfriendExpandListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend);

        qQfriendExpandListView = (ExpandableListView) findViewById(R.id.qq_friend_ExpandListView);





    }

    @Override
    protected void onResume() {

        super.onResume();
        String qqServer,qqFriendUrl;
        SharedPreferences Settings = getSharedPreferences("com.swjtu.gcmformojo_preferences", Context.MODE_PRIVATE);
        qqServer = Settings.getString("edit_text_preference_qq_replyurl","");
        qqFriendUrl=qqServer+"/openqq/get_friend_info";
        //wxServer = Settings.getString("edit_text_preference_qq_replyurl","");

        //获取好友数据存到list中
        if(QQ_FRIEND_LIST.size()==0) {
            HashMap emptyMap = new HashMap<>();
            getQqFriendData(qqFriendUrl, emptyMap);
        }



    }

    //获取好友信息
    private void getQqFriendData(final String URL, final HashMap<String, String> data){

        String getResultJson="";
        String getResult="";
        ExecutorService threadPool =  Executors.newSingleThreadExecutor();
        Future<String> future =
                threadPool.submit(
                        new Callable<String>() {
                            public String call() throws Exception {
                                Thread.sleep(1000);
                                return NetUtil.doGetRequest(URL, data);
                            };
                        }
                );
        try {
            getResultJson =  future.get();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        //解析返回结构

        try {
            JSONArray jsonArray = new JSONArray(getResultJson);
            for(int i=0;i<jsonArray.length();i++){

                JSONObject jsonObject=jsonArray.getJSONObject(i);

                if(!jsonObject.has("uid"))
                    jsonObject.put("uid","0");

                QQ_FRIEND_LIST.add(new Friend(jsonObject.getString("category"),jsonObject.getString("client_type"),jsonObject.getString("face"),jsonObject.getString("flag"),jsonObject.getString("id"),jsonObject.getString("is_vip"),
                        jsonObject.getString("markname"),jsonObject.getString("name"),jsonObject.getString("state"),jsonObject.getString("uid"),jsonObject.getString("vip_level")));
            }

        }catch (JSONException e)
        {
            e.printStackTrace();
        }

    }
}
