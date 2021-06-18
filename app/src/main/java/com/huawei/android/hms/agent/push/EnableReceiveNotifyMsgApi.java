package com.huawei.android.hms.agent.push;

import com.huawei.android.hms.agent.common.ApiClientMgr;
import com.huawei.android.hms.agent.common.BaseApiAgent;
import com.huawei.android.hms.agent.common.HMSAgentLog;
import com.huawei.hms.api.HuaweiApiClient;
import com.huawei.hms.support.api.push.HuaweiPush;

/**
 * 打开自呈现消息开关的接口。
 */
public class EnableReceiveNotifyMsgApi extends BaseApiAgent {

    /**
     * 是否打开开关
     */
    boolean enable;

    /**
     * HuaweiApiClient 连接结果回调
     *
     * @param rst    结果码
     * @param client HuaweiApiClient 实例
     */
    @Override
    public void onConnect(int rst, final HuaweiApiClient client) {
        //需要在子线程中执行开关的操作
        new Thread() {
            public void run() {
                if (client == null || !ApiClientMgr.INST.isConnect(client)) {
                    HMSAgentLog.e("client not connted");
                    return;
                }

                // 开启/关闭自呈现消息
                HuaweiPush.HuaweiPushApi.enableReceiveNotifyMsg(client, enable);
            }
        }.start();
    }

    /**
     * 打开/关闭自呈现消息
     * @param enable 打开/关闭
     */
    public void enableReceiveNotifyMsg(boolean enable) {
        this.enable = enable;
        connect();
    }
}
