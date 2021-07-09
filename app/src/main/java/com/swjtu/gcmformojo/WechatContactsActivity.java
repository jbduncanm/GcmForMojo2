package com.swjtu.gcmformojo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
//import android.support.design.widget.FloatingActionButton;
//import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
//import android.support.v7.app.AppCompatActivity;
//import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
//import android.view.Window;
//import android.view.WindowManager;
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
import static com.swjtu.gcmformojo.MyApplication.WEIXIN;
import static com.swjtu.gcmformojo.MyApplication.getCurTime;
import static com.swjtu.gcmformojo.MyApplication.WechatUIDConvert;

public class WechatContactsActivity extends Activity implements View.OnClickListener {

    private ArrayList<WechatFriend> WechatFriendArrayList;
    private ArrayList<WechatFriendGroup> WechatFriendGroups;

    private WechatFriendAdapter WechatFriendAdapter;

    private Button btn_wechat_contacts_update;
    private ExpandableListView WechatFriendExpandListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wechat_contacts);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        WechatFriendArrayList = MyApplication.getInstance().getWechatFriendArrayList();
        WechatFriendGroups = MyApplication.getInstance().getWechatFriendGroups();

        init();

        // Android 7.1 用于提升Shortcut启动性能
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            ShortcutManager mShortcutManager = getSystemService(ShortcutManager.class);
            mShortcutManager.reportShortcutUsed("static_wechat_contacts");
        }

    }

    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.wechat_contacts_update:
                ContactUpdateTask update = new ContactUpdateTask();
                update.execute();
        }
    }

    //获取好友信息
    private void getWechatFriendData(final String URL, final HashMap<String, String> data) {
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
                    jsonObject.put("name", getString(R.string.text_empty));
                if(!jsonObject.has("markname") || jsonObject.getString("markname")==null || jsonObject.getString("markname").length()==0 )
                    jsonObject.put("markname", getString(R.string.text_empty));

                WechatFriendArrayList.add(new WechatFriend(jsonObject.getString("account"), jsonObject.getString("category"), jsonObject.getString("city"), jsonObject.getString("display"), jsonObject.getString("displayname"), jsonObject.getString("id"),
                        jsonObject.getString("markname"), jsonObject.getString("name"), jsonObject.getString("province"), jsonObject.getString("sex"), jsonObject.getString("signature"),jsonObject.getString("uid")));
            }

        } catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    //定义初始化界面时执行的其他命令
    private void init() {
        btn_wechat_contacts_update = (Button) this.findViewById(R.id.wechat_contacts_update);
        WechatFriendExpandListView = (ExpandableListView) findViewById(R.id.wechat_friend_ExpandListView);

        btn_wechat_contacts_update.setOnClickListener(this);

        WechatFriendAdapter = new WechatFriendAdapter(WechatContactsActivity.this, WechatFriendGroups);
        WechatFriendExpandListView.setAdapter(WechatFriendAdapter);

        WechatFriendExpandListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener()
        {

            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id)
            {
                Intent intentSendWechat = new Intent(getApplicationContext(), DialogActivity.class);
                WechatFriend p = (WechatFriend) WechatFriendAdapter.getChild(groupPosition, childPosition);
                intentSendWechat.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                String displayname = p.get_displayname();

                Bundle msgDialogBundle = new Bundle();
                msgDialogBundle.putString("msgId", p.get_id());
                msgDialogBundle.putString("senderType", "1");
                msgDialogBundle.putString("msgType", WEIXIN);
                msgDialogBundle.putString("msgBody", getString(R.string.text_chat_initiative));
                msgDialogBundle.putInt("notifyId", WechatUIDConvert(p.get_id()));
                msgDialogBundle.putString("msgTime", getCurTime());
                //针对腾讯新闻进行优化
                if (p.get_id().equals("newsapp")) {
                    msgDialogBundle.putString("msgTitle", getString(R.string.text_wechat_contacts_newsapp));
                } else {
                    msgDialogBundle.putString("msgTitle", displayname);
                }


                intentSendWechat.putExtras(msgDialogBundle);
                startActivity(intentSendWechat);
                return true;
            }
        });
    }

    // 使用AsyncTask优化前台点击更新按钮后的用户交互性能
    // 目前阶段仅供测试，因为已发现返回上级页面时命令不能继续执行的问题
    public class ContactUpdateTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
           String WechatServer, WechatFriendUrl;
           HashMap<String, String> msgSendRequest = new HashMap<>();
           SharedPreferences Settings = getSharedPreferences(PREF, Context.MODE_PRIVATE);
           WechatServer = Settings.getString("edit_text_preference_wx_replyurl", "");
           WechatFriendUrl = WechatServer + "/openwx/get_friend_info";
           if(Settings.getBoolean("check_box_preference_wx_validation",false)) {
               try {
                   msgSendRequest.put("sign", DialogActivity.getMD5(Settings.getString("edit_text_preference_wx_salt", "")));
               } catch (Exception e) {
                   e.printStackTrace();
               }
           }
           WechatFriendGroups.clear();
           WechatFriendArrayList.clear();

           //获取好友数据存到list中
           if (WechatFriendArrayList.size() == 0)
           {
               //HashMap emptyMap = new HashMap<>();
               getWechatFriendData(WechatFriendUrl, msgSendRequest);
           }

           //存储所有组名

           ArrayList<String> groupName = new ArrayList<>();
           HashSet<String> groupNameHashSet = new HashSet<>();

           for (int i = 0; i < WechatFriendArrayList.size(); i++)
               groupNameHashSet.add(WechatFriendArrayList.get(i).get_category());

           groupName.addAll(groupNameHashSet);

           //qqFriendGroups = new ArrayList<QqFriendGroup>();// 实例化
           for (int j = 0; j < groupName.size(); ++j)
           {// 根据大组的数量，循环给各大组分配成员
               List<WechatFriend> child = new ArrayList<>();// 装小组成员的list
               WechatFriendGroup groupInfo = new WechatFriendGroup(groupName.get(j), child);// 我们自定义的大组成员对象

               for (int k = 0; k < WechatFriendArrayList.size(); k++)
               {

                   if (WechatFriendArrayList.get(k).get_category().equals(groupName.get(j)))
                       child.add(WechatFriendArrayList.get(k));

               }

               WechatFriendGroups.add(groupInfo);// 把自定义大组成员对象放入一个list中，传递给适配器
           }
           //  Snackbar.make(v, "更新成功", Snackbar.LENGTH_SHORT).show();
           return null;
       }

       @Override
       protected void onPreExecute() {
           super.onPreExecute();
       }

       @Override
       protected void onPostExecute(Void aVoid) {
           WechatFriendAdapter.notifyDataSetChanged();
           super.onPostExecute(aVoid);
       }
   }

}
