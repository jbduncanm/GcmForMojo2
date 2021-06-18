package com.huawei.android.hms.agent.push.handler;

import com.huawei.hms.support.api.push.TokenResult;

/**
 * 获取 pushtoken 回调
 */
public interface GetTokenHandler {
    /**
     * 获取pushtoken调用结果回调
     * @param rtnCode 结果码
     * @param tokenResult 登录结果
     */
    void onResult(int rtnCode, TokenResult tokenResult);
}
