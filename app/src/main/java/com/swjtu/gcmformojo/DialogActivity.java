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

import static com.swjtu.gcmformojo.MyFirebaseMessagingService.QQ;
import static com.swjtu.gcmformojo.MyFirebaseMessagingService.SYS;
import static com.swjtu.gcmformojo.MyFirebaseMessagingService.WEIXIN;
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
    private String msgId;
    private String msgTitle;
    private String msgBody;
    private String senderType;
    private String msgType;
    private String msgTime;
    private String wxPackgeName;
    private String qqPackgeName;
    private String wxReplyUrl;
    private String qqReplyUrl;
    private static ArrayAdapter<Spanned> msgAdapter;

    public static Handler msgHandler;
    public static int notifyId;

//    public static final String msgIdReply="msgIdReply";
  //  public static final String qqReplyUrl="qqReplyUrl";
    //public static final String ReplyType="ReplyType";
    //public static final String msgType="msgType";
    //public static final String wxReplyUrl="wxReplyUrl";
    //public static final String messageTitle="messageTitle";
    //public static final String messageBody="messageBody";
 //   public static final String NotificationId="NotificationId";
    //public static final String RecivedTime="RecivedTime";
    //public static final String wxPackgeName="wxPackgeName";
    //public static final String qqPackgeName="qqPackgeName";


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
                    msgListView.setSelection(msgSave.get(msgId).size());
                }

                super.handleMessage(msg);
            }
        };

        Intent intent = this.getIntent();
        Bundle msgDialogBundle = intent.getExtras();

        notifyId = msgDialogBundle.getInt("notifyId");
        msgId = msgDialogBundle.getString("msgId");
        msgTitle =msgDialogBundle.getString("msgTitle");
        msgBody =msgDialogBundle.getString("msgBody");
        senderType =msgDialogBundle.getString("senderType");
        msgType =msgDialogBundle.getString("msgType");
        msgTime =msgDialogBundle.getString("msgTime");
        if(msgDialogBundle.containsKey("qqPackgeName")) qqPackgeName =msgDialogBundle.getString("qqPackgeName");
       // if(msgDialogBundle.containsKey("qqReplyUrl"))  qqReplyUrl =msgDialogBundle.getString("qqReplyUrl");
        if(msgDialogBundle.containsKey("wxPackgeName"))  wxPackgeName =msgDialogBundle.getString("wxPackgeName");
      //  if(msgDialogBundle.containsKey("wxReplyUrl"))  wxReplyUrl =msgDialogBundle.getString("wxReplyUrl");

        //重新计数并清除通知
        if(msgCountMap.get(notifyId)!=null)
        msgCountMap.put(notifyId, 0);
        if (notifyId != -1) {
            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(notifyId);
        }

        //显示弹窗界面

        setContentView(R.layout.activity_dialog);

        //清除列表未读计数
        for(int    i=0;    i<currentUserList.size();    i++){
            if(currentUserList.get(i).getUserId().equals(msgId)){
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
        if(msgId.equals("0")) {

            msgListLinearLayout.setVisibility(View.GONE);
            imageButton_send.setVisibility(View.GONE);
            sysTextView.setVisibility(View.VISIBLE);
            sysTextView.setText("\t\t首次使用,请点击右上角选项获取设备码(卸载重装以及清除数据需要重新获取)，更多请阅读使用帮助并参考酷安发布的教程！");

        }

        //弹窗图标和是否开启发送按钮
        if(msgType.equals(QQ)) {
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

        }else if(msgType.equals(WEIXIN)){
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
            switch (msgId) {
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
       if(msgSave.get(msgId)==null) {

           List<Spanned> msgList = new ArrayList<>();
           String    str    =    "";
           if(msgType.equals(QQ)){
               str    =    "<font color='"+qqColor+"'><small>"+ msgTime +"</small></font><br>";
           }else if(msgType.equals(WEIXIN)){
               str    =    "<font color='"+wxColor+"'><small>"+ msgTime +"</small></font><br>";
           }else if(msgType.equals(SYS)){
               str    =    "<small>"+ msgTime +"</small><br>";
           }
           if(!msgBody.equals("主动聊天")) {
               msgList.add(toSpannedMessage(str + msgBody));

           }else {
               msgList.add(toSpannedMessage(""));
           }
           msgSave.put(msgId, msgList);
        }

        textView_sender.setText(msgTitle); //弹窗标题
        msgAdapter = new ArrayAdapter<>(DialogActivity.this,R.layout.dialog_msglist_item,R.id.text_message_item,msgSave.get(msgId));
        msgListView.setAdapter(msgAdapter);
        imageButton_send.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        //窗口清除时将消息ID设为-1，供消息处理判断是否弹出通知
        notifyId =-1;
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        //窗口清除时将消息ID设为-1，供消息处理判断是否弹出通知
        notifyId =-1;
        super.onStop();
    }

    @Override
    protected void onPause() {
        //窗口清除时将消息ID设为-1，供消息处理判断是否弹出通知
        notifyId =-1;
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
                String sendResult = sendMessage(editText_content.getText().toString(), msgId, senderType, msgType);
                String isSucess = "";
                if(sendResult.equals("发送成功")) {
                    isSucess = "";
                }else if(sendResult.equals("success")) {
                    isSucess = "";
                }else
                    isSucess = "<font color='#CC0000'>!</font> ";

                //将发送信息加入聊天记录
                Spanned mySendMsg;
                mySendMsg=toSpannedMessage(msgTime(msgType,true) + isSucess + editText_content.getText().toString());
                if(msgSave.get(msgId)==null) {
                    List<Spanned> msgList = new ArrayList<>();
                    msgList.add(mySendMsg);
                    msgSave.put(msgId,msgList);
                } else {
                    List<Spanned> msgList=msgSave.get(msgId);
                    msgList.add(mySendMsg);
                    msgSave.put(msgId,msgList);
                }

                //将发送信息加入会话列表
                User currentUser = new User(msgTitle, msgId, msgType,editText_content.getText().toString(),curTime(), senderType, notifyId,"0");
                for(int    i=0;    i<currentUserList.size();    i++){
                    if(currentUserList.get(i).getUserId().equals(msgId)){
                        currentUserList.remove(i);
                        break;
                    }
                }
                currentUserList.add(0,currentUser);

                //更新会话列表界面
                if(CurrentUserActivity.userHandler!=null)
                    new userThread().start();

                msgAdapter.notifyDataSetChanged();
                msgListView.setSelection(msgSave.get(msgId).size());
                editText_content.setText("");
            //    DialogActivity.this.finish();
                break;
        }
    }

    private String sendMessage(String msgSend, String msgId ,String senderType,String msgType) {

        SharedPreferences Settings = getSharedPreferences("com.swjtu.gcmformojo_preferences", Context.MODE_PRIVATE);
        String urlServer="";
        String urlType="";
        String urlQX="";
        String urlSend="";

        HashMap<String, String> msgSendRequest = new HashMap<>();

        if(msgType.equals(QQ)){
            urlServer=Settings.getString("edit_text_preference_qq_replyurl","");
            urlQX="openqq";
        }else if(msgType.equals(WEIXIN)){
            urlServer=Settings.getString("edit_text_preference_wx_replyurl","");
            urlQX="openwx";
        }

        if(senderType.equals("1")){
            urlType="/"+urlQX+"/send_friend_message";
        }else if(senderType.equals("2")){
            urlType="/"+urlQX+"/send_group_message";
        }else if(senderType.equals("3")){
            urlType="/"+urlQX+"/send_discuss_message";
        }

        urlSend=urlServer+urlType;
        msgSendRequest.put("id",msgId);
        msgSendRequest.put("content",msgSend);

        return doGetRequestResutl(urlSend,msgSendRequest);

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
                if(msgType.equals(QQ)) {
                    //打开QQ
                    // 通过包名获取要跳转的app，创建intent对象
                    Intent intentNewQq = this.getPackageManager().getLaunchIntentForPackage(qqPackgeName);

                    if (intentNewQq != null) {
                        this.startActivity(intentNewQq);
                    }  else {
                        // 没有安装要跳转的app应用进行提醒
                        Toast.makeText(this.getApplicationContext(), "未检测到"+qqPackgeName, Toast.LENGTH_LONG).show();
                    }

                }else if(msgType.equals(WEIXIN)){
                    //打开微信

                    Intent intentNewWx = this.getPackageManager().getLaunchIntentForPackage(wxPackgeName);

                    if (intentNewWx != null) {
                        this.startActivity(intentNewWx);
                    }  else {
                        // 没有安装要跳转的app应用进行提醒
                        Toast.makeText(this.getApplicationContext(), "未检测到"+wxPackgeName, Toast.LENGTH_LONG).show();
                    }

                }else if(msgType.equals(SYS)) {
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
