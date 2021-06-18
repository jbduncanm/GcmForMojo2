package com.huawei.android.hms.agent.push;

import com.huawei.android.hms.agent.common.ApiClientMgr;
import com.huawei.android.hms.agent.common.BaseApiAgent;
import com.huawei.android.hms.agent.common.HMSAgentLog;
import com.huawei.hms.api.HuaweiApiClient;
import com.huawei.hms.support.api.push.HuaweiPush;

/**
 * 获取push状态的接口。
 */
public class GetPushStateApi extends BaseApiAgent {

    /**
     * HuaweiApiClient 连接结果回调
     *
     * @param rst    结果码
     * @param client HuaweiApiClient 实例
     */
    @Override
    public void onConnect(int rst, final HuaweiApiClient client) {
        //需要在子线程中执行获取push状态的操作
        new Thread() {
            public void run() {
                if (client == null || !ApiClientMgr.INST.isConnect(client)) {
                    HMSAgentLog.e("client not connted");
                    return;
                }

                HuaweiPush.HuaweiPushApi.getPushState(client);
            }
        }.start();
    }

    /**
     * 获取push状态，push状态的回调通过广播发送。
     * 要监听的广播，请参见HMS-SDK开发准备中PushReceiver的注册
     */
    public void getPushState() {
        connect();
    }
}
