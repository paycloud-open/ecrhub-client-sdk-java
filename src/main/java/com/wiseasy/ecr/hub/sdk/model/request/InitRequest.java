package com.wiseasy.ecr.hub.sdk.model.request;

import com.wiseasy.ecr.hub.sdk.model.response.InitResponse;

public class InitRequest extends ECRHubRequest<InitResponse> {

    @Override
    public String getTopic() {
        return "ecrhub.init";
    }
}