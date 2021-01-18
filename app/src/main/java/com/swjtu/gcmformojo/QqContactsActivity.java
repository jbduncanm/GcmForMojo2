package com.swjtu.gcmformojo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.swjtu.gcmformojo.MyFirebaseMessagingService.QQ;
import static com.swjtu.gcmformojo.MyFirebaseMessagingService.curTime;

public class QqContactsActivity extends AppCompatActivity implements View.OnClickListener{

    public final static ArrayList<QqFriend> qqFriendArrayList = new ArrayList<>();

    ExpandableListView qqFriendExpandListView;
    Button qqContactsUpdateButton;
    QqFriendAdapter qqFriendAdapter;

    public final static List<QqFriendGroup> qqFriendGroups= new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qq_contacts);

        qqFriendExpandListView = (ExpandableListView) findViewById(R.id.qq_friend_ExpandListView);
        qqContactsUpdateButton = (Button) findViewById(R.id.qq_contacts_update);

        qqFriendAdapter = new QqFriendAdapter(this, qqFriendGroups);
        qqFriendExpandListView.setAdapter(qqFriendAdapter);

        qqContactsUpdateButton.setOnClickListener(this);

        qqFriendExpandListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {

            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
            int groupPosition, int childPosition, long id) {
                Intent intent = new Intent(getApplicationContext(), DialogActivity.class);
                QqFriend p= (QqFriend) qqFriendAdapter.getChild(groupPosition,childPosition);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(DialogActivity.msgIdReply, p.get_id());
                intent.putExtra(DialogActivity.qqReplyUrl, "");
                intent.putExtra(DialogActivity.wxReplyUrl, "");
                intent.putExtra(DialogActivity.ReplyType, "1");
                intent.putExtra(DialogActivity.msgType, QQ);
                String name=p.get_name();
                String markname=p.get_markname();
                if(!markname.equals("null")) name=markname;
                intent.putExtra(DialogActivity.messageTitle, name);
                intent.putExtra(DialogActivity.messageBody, "主动聊天");
                intent.putExtra(DialogActivity.NotificationId,p.get_id() );
                intent.putExtra(DialogActivity.RecivedTime, curTime());
                intent.putExtra(DialogActivity.qqPackgeName, "");
                intent.putExtra(DialogActivity.wxPackgeName, "");
                startActivity(intent);
                return true;
            }
        });


    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.qq_contacts_update:
            String qqServer, qqFriendUrl;
            SharedPreferences Settings = getSharedPreferences("com.swjtu.gcmformojo_preferences", Context.MODE_PRIVATE);
            qqServer = Settings.getString("edit_text_preference_qq_replyurl", "");
            qqFriendUrl = qqServer + "/openqq/get_friend_info";
                qqFriendGroups.clear();
                qqFriendArrayList.clear();

            //获取好友数据存到list中
            if (qqFriendArrayList.size() == 0) {
                HashMap emptyMap = new HashMap<>();
                getQqFriendData(qqFriendUrl, emptyMap);
            }
            //存储所有组名

            ArrayList<String> groupName = new ArrayList<>();
            HashSet<String> groupNameHashSet = new HashSet<>();

            for (int i = 0; i < qqFriendArrayList.size(); i++)
                groupNameHashSet.add(qqFriendArrayList.get(i).get_category());

            groupName.addAll(groupNameHashSet);


            //qqFriendGroups = new ArrayList<QqFriendGroup>();// 实例化
            for (int j = 0; j < groupName.size(); ++j) {// 根据大组的数量，循环给各大组分配成员
                List<QqFriend> child = new ArrayList<>();// 装小组成员的list
                QqFriendGroup groupInfo = new QqFriendGroup(groupName.get(j), child);// 我们自定义的大组成员对象

                for (int k = 0; k < qqFriendArrayList.size(); k++) {

                    if (qqFriendArrayList.get(k).get_category().equals(groupName.get(j)))
                        child.add(qqFriendArrayList.get(k));

                }

                qqFriendGroups.add(groupInfo);// 把自定义大组成员对象放入一个list中，传递给适配器
            }

                qqFriendAdapter.notifyDataSetChanged();
                break;

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
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

                qqFriendArrayList.add(new QqFriend(jsonObject.getString("category"),jsonObject.getString("client_type"),jsonObject.getString("face"),jsonObject.getString("flag"),jsonObject.getString("id"),jsonObject.getString("is_vip"),
                        jsonObject.getString("markname"),jsonObject.getString("name"),jsonObject.getString("state"),jsonObject.getString("uid"),jsonObject.getString("vip_level")));
            }

        }catch (JSONException e)
        {
            e.printStackTrace();
        }

    }
}
