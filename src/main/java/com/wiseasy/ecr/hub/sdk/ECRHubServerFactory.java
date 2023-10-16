package com.wiseasy.ecr.hub.sdk;

import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;

public class ECRHubServerFactory {


    public static ECRHubServer createSocketServer(ECRHubConfig config) throws ECRHubException {
        if (config == null) {
            throw new ECRHubException("ECRHubConfig cannot be empty.");
        }

        return new ECRHubSocketServer(config);
    }

}