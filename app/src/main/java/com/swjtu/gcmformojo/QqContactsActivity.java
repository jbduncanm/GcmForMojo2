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

import static com.swjtu.gcmformojo.MyApplication.PREF;
import static com.swjtu.gcmformojo.MyApplication.QQ;
import static com.swjtu.gcmformojo.MyApplication.getCurTime;

public class QqContactsActivity extends AppCompatActivity implements View.OnClickListener
{

    // public final static ArrayList<QqFriend> qqFriendArrayList = new ArrayList<>();
    // public final static ArrayList<QqFriendGroup> qqFriendGroups= new ArrayList<>();
    private ArrayList<QqFriend> qqFriendArrayList;
    private ArrayList<QqFriendGroup> qqFriendGroups;

    private QqFriendAdapter qqFriendAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qq_contacts);

        qqFriendArrayList = MyApplication.getInstance().getQqFriendArrayList();
        qqFriendGroups = MyApplication.getInstance().getQqFriendGroups();

        ExpandableListView qqFriendExpandListView = (ExpandableListView) findViewById(R.id.qq_friend_ExpandListView);
        Button qqContactsUpdateButton = (Button) findViewById(R.id.qq_contacts_update);

        qqFriendAdapter = new QqFriendAdapter(this, qqFriendGroups);
        qqFriendExpandListView.setAdapter(qqFriendAdapter);

        qqContactsUpdateButton.setOnClickListener(this);

        qqFriendExpandListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener()
        {

            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id)
            {
                Intent intentSend = new Intent(getApplicationContext(), DialogActivity.class);
                QqFriend p = (QqFriend) qqFriendAdapter.getChild(groupPosition, childPosition);
                intentSend.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                String name = p.get_name();
                if (!p.get_markname().equals("null")) name = p.get_markname();

                Bundle msgDialogBundle = new Bundle();
                msgDialogBundle.putString("msgId", p.get_id());
                msgDialogBundle.putString("senderType", "1");
                msgDialogBundle.putString("msgType", QQ);
                msgDialogBundle.putString("msgTitle", name);
                msgDialogBundle.putString("msgBody", "主动聊天");
                msgDialogBundle.putInt("notifyId", Integer.parseInt(p.get_id().substring(0, 9)));
                msgDialogBundle.putString("msgTime", getCurTime());

                intentSend.putExtras(msgDialogBundle);
                startActivity(intentSend);
                return true;
            }
        });


    }

    @Override
    public void onClick(View v)
    {

        switch (v.getId())
        {
            case R.id.qq_contacts_update:
                String qqServer, qqFriendUrl;
                HashMap<String, String> msgSendRequest = new HashMap<>();
                SharedPreferences Settings = getSharedPreferences(PREF, Context.MODE_PRIVATE);
                qqServer = Settings.getString("edit_text_preference_qq_replyurl", "");
                qqFriendUrl = qqServer + "/openqq/get_friend_info";
                if(Settings.getBoolean("check_box_preference_wx_validation",false)) {
                    try {
                        msgSendRequest.put("sign", DialogActivity.getMD5(Settings.getString("edit_text_preference_qq_salt", "")));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                qqFriendGroups.clear();
                qqFriendArrayList.clear();

                //获取好友数据存到list中
                if (qqFriendArrayList.size() == 0)
                {
                    //HashMap emptyMap = new HashMap<>();
                    getQqFriendData(qqFriendUrl, msgSendRequest);
                }
                //存储所有组名

                ArrayList<String> groupName = new ArrayList<>();
                HashSet<String> groupNameHashSet = new HashSet<>();

                for (int i = 0; i < qqFriendArrayList.size(); i++)
                    groupNameHashSet.add(qqFriendArrayList.get(i).get_category());

                groupName.addAll(groupNameHashSet);


                //qqFriendGroups = new ArrayList<QqFriendGroup>();// 实例化
                for (int j = 0; j < groupName.size(); ++j)
                {// 根据大组的数量，循环给各大组分配成员
                    List<QqFriend> child = new ArrayList<>();// 装小组成员的list
                    QqFriendGroup groupInfo = new QqFriendGroup(groupName.get(j), child);// 我们自定义的大组成员对象

                    for (int k = 0; k < qqFriendArrayList.size(); k++)
                    {

                        if (qqFriendArrayList.get(k).get_category().equals(groupName.get(j)))
                            child.add(qqFriendArrayList.get(k));

                    }

                    qqFriendGroups.add(groupInfo);// 把自定义大组成员对象放入一个list中，传递给适配器
                }

                qqFriendAdapter.notifyDataSetChanged();
              //  Snackbar.make(v, "更新成功", Snackbar.LENGTH_SHORT).show();
                break;

        }
    }

    //获取好友信息
    private void getQqFriendData(final String URL, final HashMap<String, String> data)
    {

        String getResultJson = "";
        ExecutorService threadPool = Executors.newSingleThreadExecutor();
        Future<String> future =
                threadPool.submit(
                        new Callable<String>()
                        {
                            public String call() throws Exception
                            {
                                Thread.sleep(1000);
                                return NetUtil.doGetRequest(URL, data);
                            }

                        }
                );
        try
        {
            getResultJson = future.get();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        //解析返回结构

        try
        {
            JSONArray jsonArray = new JSONArray(getResultJson);
            for (int i = 0; i < jsonArray.length(); i++)
            {

                JSONObject jsonObject = jsonArray.getJSONObject(i);

                if (!jsonObject.has("uid"))
                    jsonObject.put("uid", "0");

                if(!jsonObject.has("name") || jsonObject.getString("name")==null || jsonObject.getString("name").length()==0 )
                    jsonObject.put("name", "空");
                if(!jsonObject.has("markname") || jsonObject.getString("markname")==null || jsonObject.getString("markname").length()==0 )
                    jsonObject.put("markname", "空");

                qqFriendArrayList.add(new QqFriend(jsonObject.getString("category"), jsonObject.getString("client_type"), jsonObject.getString("face"), jsonObject.getString("flag"), jsonObject.getString("id"), jsonObject.getString("is_vip"),
                        jsonObject.getString("markname"), jsonObject.getString("name"), jsonObject.getString("state"), jsonObject.getString("uid"), jsonObject.getString("vip_level")));
            }

        } catch (JSONException e)
        {
            e.printStackTrace();
        }

    }
}
