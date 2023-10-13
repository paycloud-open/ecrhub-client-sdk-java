package com.wiseasy.ecr.hub.sdk;

import com.wiseasy.ecr.hub.sdk.model.ECRHubTerminal;

/**
 * @author wangyuxiang@wiseasy.com
 * @since 2023-10-13 09:59
 */
public interface ClientListener {

    /**
     * New terminal device connection
     */
    void onConnect(ECRHubTerminal terminal);

    /**
     * terminal disconnect
     */
    void onDisconnect(ECRHubTerminal terminal, int code, String reason, boolean remote);

}
