package com.wiseasy.ecr.hub.sdk.sp.websocket;

import org.java_websocket.WebSocket;

import java.nio.ByteBuffer;

/**
 * @author wangyuxiang
 * @since 2023-10-13 10:45
 */
public interface WebSocketClientListener {

    default void onOpen(WebSocket socket) {

    }

    default void onClose(WebSocket socket, int code, String reason, boolean remote) {

    }

    void onMessage(WebSocket conn, String message);

    void onMessage(WebSocket conn, ByteBuffer message);
}
