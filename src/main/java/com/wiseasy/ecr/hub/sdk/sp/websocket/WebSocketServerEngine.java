package com.wiseasy.ecr.hub.sdk.sp.websocket;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import com.wiseasy.ecr.hub.sdk.utils.HexUtil;
import com.wiseasy.ecr.hub.sdk.utils.NetHelper;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class WebSocketServerEngine extends WebSocketServer {

    private volatile boolean running = false;
    private static final Logger log = LoggerFactory.getLogger(WebSocketServerEngine.class);

    private WebSocketClientListener clientListener;

    public void setClientListener(WebSocketClientListener clientListener) {
        this.clientListener = clientListener;
    }

    public boolean isRunning() {
        return running;
    }

    public WebSocketServerEngine() {
        this(NetUtil.getUsableLocalPort());
    }

    public WebSocketServerEngine(int port) {
        this(NetHelper.getLocalhost(), NetHelper.getUsableLocalPort(port));
    }

    public WebSocketServerEngine(InetAddress siteLocalAddress, int port) {
        super(new InetSocketAddress(siteLocalAddress, port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        if (log.isDebugEnabled()) {
            log.debug("socket open success: {}", conn.getRemoteSocketAddress());
        }
        if (null != clientListener) {
            clientListener.onOpen(conn);
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        if (log.isDebugEnabled()) {
            log.debug("socket onClose. code: {},reason:{},remote:{}", code, reason, remote);
        }
        if (null != clientListener) {
            clientListener.onClose(conn, code, reason, remote);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (log.isDebugEnabled()) {
            log.debug("socket onMessage. {}", message);
        }
        if (null != clientListener) {
            clientListener.onMessage(conn, message);
        }
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        if (log.isDebugEnabled()) {
            log.debug("socket onMessage. {}", HexUtil.byte2hex(message.array()));
        }
        if (null != clientListener) {
            clientListener.onMessage(conn, message);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        log.error("socket onError. ", ex);
    }

    @Override
    public void onStart() {
        running = true;
        log.info("service started : {}", StrUtil.format("ws//{}:{}", this.getAddress().getAddress().getHostAddress(), this.getPort()));
    }

    @Override
    public void stop() throws InterruptedException {
        super.stop();
        log.info("service stopped : {}", StrUtil.format("ws//{}:{}", this.getAddress().getAddress().getHostAddress(), this.getPort()));
        running = false;
    }

}