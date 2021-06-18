package com.huawei.android.hms.agent.push;

import com.huawei.android.hms.agent.common.ApiClientMgr;
import com.huawei.android.hms.agent.common.BaseApiAgent;
import com.huawei.android.hms.agent.common.HMSAgentLog;
import com.huawei.hms.api.HuaweiApiClient;
import com.huawei.hms.support.api.push.HuaweiPush;

/**
 * 获取push协议展示的接口。
 */
public class QueryAgreementApi extends BaseApiAgent {

    /**
     * HuaweiApiClient 连接结果回调
     *
     * @param rst    结果码
     * @param client HuaweiApiClient 实例
     */
    @Override
    public void onConnect(int rst, final HuaweiApiClient client) {
        //需要在子线程中执行获取push协议展示的操作
        new Thread() {
            public void run() {
                if (client == null || !ApiClientMgr.INST.isConnect(client)) {
                    HMSAgentLog.e("client not connted");
                    return;
                }

                HuaweiPush.HuaweiPushApi.queryAgreement(client);
            }
        }.start();
    }

    /**
     * 请求push协议展示
     */
    public void queryAgreement() {
        connect();
    }
}
