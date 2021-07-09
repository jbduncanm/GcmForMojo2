package com.huawei.android.hms.agent;


import android.app.Activity;
import android.app.Application;

import com.huawei.android.hms.agent.common.ActivityMgr;
import com.huawei.android.hms.agent.common.ApiClientMgr;
import com.huawei.android.hms.agent.common.HMSAgentLog;
import com.huawei.android.hms.agent.common.IClientConnectCallback;
import com.huawei.android.hms.agent.common.INoProguard;
import com.huawei.android.hms.agent.common.handler.ConnectHandler;
import com.huawei.android.hms.agent.push.DeleteTokenApi;
import com.huawei.android.hms.agent.push.EnableReceiveNormalMsgApi;
import com.huawei.android.hms.agent.push.EnableReceiveNotifyMsgApi;
import com.huawei.android.hms.agent.push.GetPushStateApi;
import com.huawei.android.hms.agent.push.GetTokenApi;
import com.huawei.android.hms.agent.push.QueryAgreementApi;
import com.huawei.android.hms.agent.push.handler.GetTokenHandler;
import com.huawei.hms.api.HuaweiApiAvailability;
import com.huawei.hms.api.HuaweiApiClient;

/**
 * HMSAgent 封装入口类。 提供了HMS SDK 功能的封装，使开发者更聚焦业务的处理。
 */
public final class HMSAgent implements INoProguard {

    /**
     * 基础版本
     */
    private static final String VER_020503001 = "020503001";

    /**
     * 2.5.3 版本2
     * 自身优化：
     *      1、增加了升级时被其他界面覆盖的处理，解决错误回调成功，增加重试次数3次
     *      2、增强异常分支日志
	 *      3、提供了多种HMSAgent初始化方法
     */
    private static final String VER_020503002 = "020503002";

    /**
     * 2.5.3 版本3
     * 自身优化：
     *      1、示例代码manifest增加升级相关的配置
     *      2、代码提取脚本兼容路径有空格的情景
     */
    private static final String VER_020503003 = "020503003";

    /**
     * 当前版本号
     */
    public static final String CURVER = VER_020503003;

    public static final class AgentResultCode {

        /**
         * HMSAgent 成功
         */
        public static final int HMSAGENT_SUCCESS = 0;

        /**
         * HMSAgent 没有初始化
         */
        public static final int HMSAGENT_NO_INIT = -1000;

        /**
         * 请求需要activity，但当前没有可用的activity
         */
        public static final int NO_ACTIVITY_FOR_USE = -1001;

        /**
         * 结果为空
         */
        public static final int RESULT_IS_NULL = -1002;

        /**
         * 状态为空
         */
        public static final int STATUS_IS_NULL = -1003;

        /**
         * 拉起activity异常，需要检查activity有没有在manifest中配置
         */
        public static final int START_ACTIVITY_ERROR = -1004;

        /**
         * onActivityResult 回调结果错误
         */
        public static final int ON_ACTIVITY_RESULT_ERROR = -1005;

        /**
         * 重复请求
         */
        public static final int REQUEST_REPEATED = -1006;

        /**
         * 连接client 超时
         */
        public static final int APICLIENT_TIMEOUT = -1007;
    }

    private HMSAgent(){}


    /**
     * 初始化方法，传入第一个界面的activity
     * @param activity 当前界面
     * @return true：成功 false：失败
     */
    public static boolean init(Activity activity) {
        return init(null, activity);
    }

    /**
     * 初始化方法，建议在Application onCreate里面调用
     * @param app 应用程序
     * @return true：成功 false：失败
     */
    public static boolean init(Application app) {
        return init(app, null);
    }

    /**
     * 初始化方法，建议在Application onCreate里面调用
     * @param app 应用程序
     * @param activity 当前界面
     * @return true：成功 false：失败
     */
    public static boolean init(Application app, Activity activity) {

        Application appTmp = app;
        Activity activityTmp = activity;

        // 两个参数都为null，直接抛异常
        if (appTmp == null && activityTmp == null) {
            HMSAgentLog.e("the param of method HMSAgent.init can not be null !!!");
            return false;
        }

        // 如果application实例为null，则从activity里面取
        if (appTmp == null) {
            appTmp = activityTmp.getApplication();
        }

        // 如果application实例仍然为null，抛异常
        if (appTmp == null) {
            HMSAgentLog.e("the param of method HMSAgent.init app can not be null !!!");
            return false;
        }

        // activity 已经失效，则赋值null
        if (activityTmp != null && activityTmp.isFinishing()) {
            activityTmp = null;
        }

        HMSAgentLog.i("init HMSAgent " + CURVER + " with hmssdkver " + HuaweiApiAvailability.HMS_SDK_VERSION_CODE);

        // 初始化activity管理类
        ActivityMgr.INST.init(appTmp, activityTmp);

        // 初始化HuaweiApiClient管理类
        ApiClientMgr.INST.init(appTmp);

        return true;
    }

    /**
     * 释放资源，这里一般不需要调用
     */
    public static void destroy() {
        ActivityMgr.INST.release();
        ApiClientMgr.INST.release();
    }

    /**
     * 连接HMS SDK， 可能拉起界面(包括升级引导等)，建议在第一个界面进行连接。
     * 此方法可以重复调用，没必要为了只调用一次做复杂处理
     * @param activity 当前界面的activity， 不能传空
     * @param callback 连接结果回调
     */
    public static void connect(Activity activity, final ConnectHandler callback) {
        ApiClientMgr.INST.connect(new IClientConnectCallback() {
            @Override
            public void onConnect(int rst, HuaweiApiClient client) {
                if (callback != null) {
                    callback.onConnect(rst);
                }
            }
        }, true);
    }

    /**
     * 检查本应用的升级
     * @param activity 上下文
     */
    public static void checkUpdate (final Activity activity) {
        HMSAgentLog.d("start checkUpdate");
        ApiClientMgr.INST.connect(new IClientConnectCallback() {
            @Override
            public void onConnect(int rst, HuaweiApiClient client) {
                Activity activityCur = ActivityMgr.INST.getLastActivity();

                if (activityCur != null && client != null) {
                    client.checkUpdate(activityCur);
                } else if (activity != null && client != null){
                    client.checkUpdate(activity);
                } else {
                    // 跟SE确认：activity 为 null ， 不处理
                    HMSAgentLog.e("no activity to checkUpdate");
                }
            }
        }, true);
    }





    /**
     * push接口封装
     */
    public static final class Push {
        /**
         * 获取pushtoken接口
         * pushtoken通过广播下发，要监听的广播，请参见HMS-SDK开发准备中PushReceiver的注册
         * @param handler pushtoken接口调用回调
         */
        public static void getToken(GetTokenHandler handler){
            new GetTokenApi().getToken(handler);
        }

        /**
         * 删除指定的pushtoken
         * 该接口只在EMUI5.1以及更高版本的华为手机上调用该接口后才不会收到PUSH消息。
         * @param token 要删除的token
         */
        public static void deleteToken(String token){
            new DeleteTokenApi().deleteToken(token);
        }

        /**
         * 获取push状态，push状态的回调通过广播发送。
         * 要监听的广播，请参见HMS-SDK开发准备中PushReceiver的注册
         */
        public static void getPushState(){
            new GetPushStateApi().getPushState();
        }

        /**
         * 打开/关闭通知栏消息
         * @param enable 打开/关闭
         */
        public static void enableReceiveNotifyMsg(boolean enable){
            new EnableReceiveNotifyMsgApi().enableReceiveNotifyMsg(enable);
        }

        /**
         * 打开/关闭透传消息
         * @param enable 打开/关闭
         */
        public static void enableReceiveNormalMsg(boolean enable){
            new EnableReceiveNormalMsgApi().enableReceiveNormalMsg(enable);
        }

        /**
         * 请求push协议展示
         */
        public static void queryAgreement(){
            new QueryAgreementApi().queryAgreement();
        }
    }
}
