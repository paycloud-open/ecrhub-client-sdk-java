package com.wiseasy.ecr.hub.sdk;

import cn.hutool.core.util.StrUtil;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;

public class ECRHubServerFactory {


    public static ECRHubServer createSocketServer(ECRHubConfig config) throws ECRHubException {
        if (config == null) {
            throw new ECRHubException("ECRHubConfig cannot be empty.");
        }
        if (StrUtil.isBlank(config.getAppId())) {
            throw new ECRHubException("AppId cannot be empty.");
        }

        return new ECRHubSocketServer(config);
    }

}