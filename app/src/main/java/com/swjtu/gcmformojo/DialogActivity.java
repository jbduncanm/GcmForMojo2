package com.swjtu.gcmformojo;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spanned;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.swjtu.gcmformojo.MyFirebaseMessagingService.curTime;
import static com.swjtu.gcmformojo.CurrentUserActivity.currentUserList;
import static com.swjtu.gcmformojo.MyFirebaseMessagingService.isQqOnline;
import static com.swjtu.gcmformojo.MyFirebaseMessagingService.isWxOnline;
import static com.swjtu.gcmformojo.MyFirebaseMessagingService.msgCountMap;
import static com.swjtu.gcmformojo.MyFirebaseMessagingService.msgSave;
import static com.swjtu.gcmformojo.MyFirebaseMessagingService.msgTime;
import static com.swjtu.gcmformojo.MyFirebaseMessagingService.qqColor;
import static com.swjtu.gcmformojo.MyFirebaseMessagingService.toSpannedMessage;
import static com.swjtu.gcmformojo.MyFirebaseMessagingService.wxColor;

public class DialogActivity extends Activity  implements View.OnClickListener {

    private EditText editText_content;
    private ListView msgListView;
    private String msgIdReplyDo;
    private String messageTitleDo;
    private String ReplyTypeDo;
    private String msgTypeDo;
    private String wxPackgeNameDo;
    private String qqPackgeNameDo;
    static int NotificationIdDo;
    private static ArrayAdapter<Spanned> msgAdapter;
    static Handler msgHandler;

    public static final String msgIdReply="msgIdReply";
    public static final String qqReplyUrl="qqReplyUrl";
    public static final String ReplyType="ReplyType";
    public static final String msgType="msgType";
    public static final String wxReplyUrl="wxReplyUrl";
    public static final String messageTitle="messageTitle";
    public static final String messageBody="messageBody";
    public static final String NotificationId="NotificationId";
    public static final String RecivedTime="RecivedTime";
    public static final String wxPackgeName="wxPackgeName";
    public static final String qqPackgeName="qqPackgeName";


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        TextView textView_sender;
        ImageButton imageButton_send;
        ImageView imgMsgType;

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); //hide activity title
        setFinishOnTouchOutside(true);//

        msgHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                String string = (String)msg.obj;

                if(msgAdapter!=null){
                    msgAdapter.notifyDataSetChanged();
                    msgListView.setSelection(msgSave.get(msgIdReplyDo).size());
                }

                super.handleMessage(msg);
            }
        };

        Intent intent = this.getIntent();

        NotificationIdDo = intent.getIntExtra(NotificationId, -1);
        msgIdReplyDo = intent.getStringExtra(msgIdReply);
        messageTitleDo = intent.getStringExtra(messageTitle);
        String messageBodyDo = intent.getStringExtra(messageBody);
        ReplyTypeDo = intent.getStringExtra(ReplyType);
        msgTypeDo = intent.getStringExtra(msgType);
        String recivedTimeDo = intent.getStringExtra(RecivedTime);
        qqPackgeNameDo = intent.getStringExtra(qqPackgeName);
        wxPackgeNameDo = intent.getStringExtra(wxPackgeName);

        //重新计数并清除通知
        if(msgCountMap.get(NotificationIdDo)!=null)
        msgCountMap.put(NotificationIdDo, 0);
        if (NotificationIdDo != -1) {
            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(NotificationIdDo);
        }



        //显示弹窗界面

        setContentView(R.layout.activity_dialog);

        //清除列表未读计数
        for(int    i=0;    i<currentUserList.size();    i++){
            if(currentUserList.get(i).getUserId().equals(msgIdReplyDo)){
                currentUserList.get(i).setMsgCount("0");
                if(CurrentUserActivity.userHandler!=null)
                    new userThread().start();
                break;
            }
        }

        //如果未开启回复功能，则屏蔽发送按钮
        SharedPreferences Settings = getSharedPreferences("com.swjtu.gcmformojo_preferences", Context.MODE_PRIVATE);
        Boolean qqIsReply=Settings.getBoolean("check_box_preference_qq_reply",false);
        Boolean wxIsReply=Settings.getBoolean("check_box_preference_wx_reply",false);

        textView_sender = (TextView) findViewById(R.id.textView_sender);
        msgListView = (ListView) findViewById(R.id.msg_list_view);
        imgMsgType = (ImageView) findViewById(R.id.msgType_imageView);
        imageButton_send = (ImageButton) findViewById(R.id.imagebutton_send);
        editText_content = (EditText) findViewById(R.id.edittext_content);
        LinearLayout msgListLinearLayout = (LinearLayout) findViewById(R.id.msg_list_ll);
        TextView sysTextView = (TextView) findViewById(R.id.msgType_text);

        //纯系统消息选择屏蔽Listview消息记录，单独显示Textview
        if(msgIdReplyDo.equals("0")) {

            msgListLinearLayout.setVisibility(View.GONE);
            imageButton_send.setVisibility(View.GONE);
            sysTextView.setVisibility(View.VISIBLE);
            sysTextView.setText("\t\t首次使用,请点击右上角选项获取设备码(卸载重装以及清除数据需要重新获取)，更多请阅读使用帮助并参考酷安发布的教程！");

        }

        //弹窗图标和是否开启发送按钮
        if(msgTypeDo.equals("Mojo-Webqq")) {
            imgMsgType.setImageResource(R.mipmap.qq);
            if(!qqIsReply) {
                imageButton_send.setEnabled(false);
                editText_content.setEnabled(false);
                editText_content.setText("未开启回复功能");
            }
            if(isQqOnline==0) {
                imageButton_send.setEnabled(false);
                editText_content.setEnabled(false);
                editText_content.setText("服务端未登录");
            }

        }else if(msgTypeDo.equals("Mojo-Weixin")){
            imgMsgType.setImageResource(R.mipmap.weixin);
            if(!wxIsReply) {
                imageButton_send.setEnabled(false);
                editText_content.setEnabled(false);
                editText_content.setText("未开启回复功能");
            }
            if(isWxOnline==0) {
                imageButton_send.setEnabled(false);
                editText_content.setEnabled(false);
                editText_content.setText("服务端未登录");
            }
        }else {
            //系统消息中的QQ和微信服务通知图标
            switch (msgIdReplyDo) {
                case "0":
                    imgMsgType.setImageResource(R.mipmap.pin);
                    break;
                case "1":
                    imgMsgType.setImageResource(R.mipmap.qq);
                    break;
                case "2":
                    imgMsgType.setImageResource(R.mipmap.weixin);
                    break;
                default:
                    imgMsgType.setImageResource(R.mipmap.pin);
            }

            imageButton_send.setEnabled(false);
            editText_content.setEnabled(false);
            editText_content.setText("系统控制");

        }

        //应用杀掉后读取最后一条通知内容作为聊天记录
       if(msgSave.get(msgIdReplyDo)==null) {

           List<Spanned> msgList = new ArrayList<>();
           String    str    =    "";
           if(msgTypeDo.equals("Mojo-Webqq")){
               str    =    "<font color='"+qqColor+"'><small>"+ recivedTimeDo +"</small></font><br>";
           }else if(msgTypeDo.equals("Mojo-Weixin")){
               str    =    "<font color='"+wxColor+"'><small>"+ recivedTimeDo +"</small></font><br>";
           }else if(msgTypeDo.equals("Mojo-Sys")){
               str    =    "<small>"+ recivedTimeDo +"</small><br>";
           }
           if(!messageBodyDo.equals("主动聊天")) {
               msgList.add(toSpannedMessage(str + messageBodyDo));

           }else {
               msgList.add(toSpannedMessage(""));
           }
           msgSave.put(msgIdReplyDo, msgList);
        }

        textView_sender.setText(messageTitleDo); //弹窗标题
        msgAdapter = new ArrayAdapter<>(DialogActivity.this,R.layout.dialog_msglist_item,R.id.text_message_item,msgSave.get(msgIdReplyDo));
        msgListView.setAdapter(msgAdapter);
        imageButton_send.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        //窗口清除时将消息ID设为-1，供消息处理判断是否弹出通知
        NotificationIdDo=-1;
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        //窗口清除时将消息ID设为-1，供消息处理判断是否弹出通知
        NotificationIdDo=-1;
        super.onStop();
    }

    @Override
    protected void onPause() {
        //窗口清除时将消息ID设为-1，供消息处理判断是否弹出通知
        NotificationIdDo=-1;
        super.onPause();
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            //case R.id.imagebutton_cancel:
            //    DialogActivity.this.finish();
            //    break;
            case R.id.imagebutton_send:

                if(editText_content.getText().toString().length()==0)
                    break;
                String sendResult = sendMessage(editText_content.getText().toString(), msgIdReplyDo,ReplyTypeDo,msgTypeDo);
                String isSucess = "";
                if(sendResult.equals("发送成功")) {
                    isSucess = "";
                }else if(sendResult.equals("success")) {
                    isSucess = "";
                }else
                    isSucess = "<font color='#CC0000'>!</font> ";

                //将发送信息加入聊天记录
                Spanned mySendMsg;
                mySendMsg=toSpannedMessage(msgTime(msgTypeDo,true) + isSucess + editText_content.getText().toString());
                if(msgSave.get(msgIdReplyDo)==null) {
                    List<Spanned> msgList = new ArrayList<>();
                    msgList.add(mySendMsg);
                    msgSave.put(msgIdReplyDo,msgList);
                } else {
                    List<Spanned> msgList=msgSave.get(msgIdReplyDo);
                    msgList.add(mySendMsg);
                    msgSave.put(msgIdReplyDo,msgList);
                }

                //将发送信息加入会话列表
                User currentUser = new User(messageTitleDo,msgIdReplyDo,msgTypeDo,editText_content.getText().toString(),curTime(),ReplyTypeDo,NotificationIdDo,"0");
                for(int    i=0;    i<currentUserList.size();    i++){
                    if(currentUserList.get(i).getUserId().equals(msgIdReplyDo)){
                        currentUserList.remove(i);
                        break;
                    }
                }
                currentUserList.add(0,currentUser);

                //更新会话列表界面
                if(CurrentUserActivity.userHandler!=null)
                    new userThread().start();

                msgAdapter.notifyDataSetChanged();
                msgListView.setSelection(msgSave.get(msgIdReplyDo).size());
                editText_content.setText("");
            //    DialogActivity.this.finish();
                break;
        }
    }

    private String sendMessage(String content, String msgIdReply ,String senderTypeReply,String msgTypeReply) {

        SharedPreferences Settings = getSharedPreferences("com.swjtu.gcmformojo_preferences", Context.MODE_PRIVATE);
        String urlServer="";
        String urlType="";
        String urlQX="";
        String urlSend="";

        HashMap<String, String> request = new HashMap<>();

        if(msgTypeReply.equals("Mojo-Webqq")){
            urlServer=Settings.getString("edit_text_preference_qq_replyurl","");
            urlQX="openqq";
        }else if(msgTypeReply.equals("Mojo-Weixin")){
            urlServer=Settings.getString("edit_text_preference_wx_replyurl","");
            urlQX="openwx";
        }

        if(senderTypeReply.equals("1")){
            urlType="/"+urlQX+"/send_friend_message";
        }else if(senderTypeReply.equals("2")){
            urlType="/"+urlQX+"/send_group_message";
        }else if(senderTypeReply.equals("3")){
            urlType="/"+urlQX+"/send_discuss_message";
        }

        urlSend=urlServer+urlType;
        request.put("id",msgIdReply);
        request.put("content",content);

        return doGetRequestResutl(urlSend,request);

    }

    //子线程处理发送消息
    private void  doGetRequest(final String URL, final HashMap<String, String> data){
        new Thread(new Runnable() {
            @Override
            public void run() {
               NetUtil.doGetRequest(URL, data);
            }
        }).start();
    }

    private String  doGetRequestResutl(final String URL, final HashMap<String, String> data){

        String sendResultJson="";
        String sendResult="";
        ExecutorService threadPool =  Executors.newSingleThreadExecutor();
        Future<String> future =
                threadPool.submit(
                        new Callable<String>() {
                            public String call() throws Exception {
                               //Thread.sleep(2000);
                               return NetUtil.doGetRequest(URL, data);
                            };
                        }
                );
        try {
            sendResultJson =  future.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //解析返回结构
        try
        {
            JSONObject jsonObject = new JSONObject(sendResultJson);
            sendResult = jsonObject.getString("status");
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        return sendResult;


    }

    @Override
    protected void onNewIntent(Intent intent) {
        // TODO Auto-generated method stub
        super.onNewIntent(intent);
        setIntent(intent);
    }

    //对话框标题栏点击事件
    public void onTitleClick(View v) {
        switch (v.getId()){
            case R.id.title_relativeLayout:
                //System.out.println("整个布局被点击");
                if(msgTypeDo.equals("Mojo-Webqq")) {
                    //打开QQ
                    // 通过包名获取要跳转的app，创建intent对象
                    Intent intentNewQq = this.getPackageManager().getLaunchIntentForPackage(qqPackgeNameDo);

                    if (intentNewQq != null) {
                        this.startActivity(intentNewQq);
                    }  else {
                        // 没有安装要跳转的app应用进行提醒
                        Toast.makeText(this.getApplicationContext(), "未检测到"+qqPackgeName, Toast.LENGTH_LONG).show();
                    }

                }else if(msgTypeDo.equals("Mojo-Weixin")){
                    //打开微信

                    Intent intentNewWx = this.getPackageManager().getLaunchIntentForPackage(wxPackgeNameDo);

                    if (intentNewWx != null) {
                        this.startActivity(intentNewWx);
                    }  else {
                        // 没有安装要跳转的app应用进行提醒
                        Toast.makeText(this.getApplicationContext(), "未检测到"+wxPackgeName, Toast.LENGTH_LONG).show();
                    }

                }else if(msgTypeDo.equals("Mojo-Sys")) {
                    //打开主界面
                    Intent intentNewSys = new Intent(this, CurrentUserActivity.class);
                    this.startActivity(intentNewSys);
                }

                break;
        }
    }

    public Handler getHandler(){
        return msgHandler;
    }

    //子线程处理ui更新
    class userThread extends Thread {
        private Context context;
        private Handler handler;
        public userThread(){

        }

        public userThread(Context context) {
            this.context = context;
            this.handler = ((CurrentUserActivity) context).getHandler();
        }

        @Override
        public void run() {
            Message msg = new Message();
            msg.obj = "Update currentUserList!";
            CurrentUserActivity.userHandler.sendMessage(msg);
            super.run();
        }
    }

}
