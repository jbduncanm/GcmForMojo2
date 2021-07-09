package com.huawei.android.hms.agent.push;

import android.text.TextUtils;

import com.huawei.android.hms.agent.common.ApiClientMgr;
import com.huawei.android.hms.agent.common.BaseApiAgent;
import com.huawei.android.hms.agent.common.HMSAgentLog;
import com.huawei.hms.api.HuaweiApiClient;
import com.huawei.hms.support.api.push.HuaweiPush;
import com.huawei.hms.support.api.push.PushException;

/**
 * 删除pushtoken的接口。
 */
public class DeleteTokenApi extends BaseApiAgent {

    /**
     * 待删除的push token
     */
    private String token;

    /**
     * HuaweiApiClient 连接结果回调
     *
     * @param rst    结果码
     * @param client HuaweiApiClient 实例
     */
    @Override
    public void onConnect(int rst, final HuaweiApiClient client) {
        //需要在子线程中执行删除TOKEN操作
        new Thread() {
            public void run() {
                //调用删除TOKEN需要传入通过getToken接口获取到TOKEN，并且需要对TOKEN进行非空判断
                if (!TextUtils.isEmpty(token)){
                    try {
                        if (client == null || !ApiClientMgr.INST.isConnect(client)) {
                            HMSAgentLog.e("client not connted");
                            return;
                        }

                        HuaweiPush.HuaweiPushApi.deleteToken(client, token);
                    } catch (PushException e) {
                        HMSAgentLog.e("删除TOKEN失败:" + e.getMessage());
                    }
                }
            }
        }.start();
    }

    /**
     * 删除指定的pushtoken
     * 该接口只在EMUI5.1以及更高版本的华为手机上调用该接口后才不会收到PUSH消息。
     * @param token 要删除的token
     */
    public void deleteToken(String token) {
        this.token = token;
        connect();
    }
}
