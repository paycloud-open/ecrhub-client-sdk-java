package com.wiseasy.ecr.hub.sdk.sp.websocket;

import org.java_websocket.WebSocket;

/**
 * @author wangyuxiang@wiseasy.com
 * @since 2023-10-13 10:45
 */
public interface WebSocketClientListener {

    void onOpen(WebSocket socket);

    void onClose(WebSocket socket, int code, String reason, boolean remote);

}
