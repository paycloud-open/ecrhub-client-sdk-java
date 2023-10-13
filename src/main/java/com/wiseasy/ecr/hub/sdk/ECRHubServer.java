package com.wiseasy.ecr.hub.sdk;

import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.model.ECRHubTerminal;
import com.wiseasy.ecr.hub.sdk.model.request.ECRHubRequest;
import com.wiseasy.ecr.hub.sdk.model.response.ECRHubResponse;

import java.util.List;

/**
 * @author wangyuxiang@wiseasy.com
 * @since 2023-10-13 09:27
 */
public interface ECRHubServer {

    boolean start() throws ECRHubException;

    boolean stop() throws ECRHubException;

    void setClientListener(ClientListener clientListener);

    List<ECRHubTerminal> getTerminalList();

    boolean disconnect(ECRHubTerminal terminal) throws ECRHubException;

    <T extends ECRHubResponse> T execute(ECRHubTerminal terminal, ECRHubRequest<T> request) throws ECRHubException;

    <T extends ECRHubResponse> void asyncExecute(ECRHubTerminal terminal, ECRHubRequest<T> request, ECRHubResponseCallBack<T> callback) throws ECRHubException;

}
