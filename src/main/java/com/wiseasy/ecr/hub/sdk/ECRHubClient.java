package com.wiseasy.ecr.hub.sdk;

import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.model.request.ECRHubRequest;
import com.wiseasy.ecr.hub.sdk.model.response.ECRHubResponse;

public interface ECRHubClient {

    boolean connect() throws ECRHubException;

    ECRHubResponse connect2() throws ECRHubException;

    boolean isConnected() throws ECRHubException;

    boolean disconnect() throws ECRHubException;

    <T extends ECRHubResponse> T execute(ECRHubRequest<T> request) throws ECRHubException;

    <T extends ECRHubResponse> void asyncExecute(ECRHubRequest<T> request, ECRHubResponseCallBack<T> callback) throws ECRHubException;

}