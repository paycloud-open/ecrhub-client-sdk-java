package com.wiseasy.ecr.hub.sdk.spi.websocket;

import cn.hutool.cache.impl.FIFOCache;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubTimeoutException;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubProtobufHelper;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubResponseProto;
import com.wiseasy.ecr.hub.sdk.utils.NetHelper;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class WebSocketServerEngine extends WebSocketServer {

    private boolean running = false;

    private final FIFOCache<String, String> MSG_CACHE = new FIFOCache<>(20, 10 * 60 * 1000);

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
        this(NetHelper.getSiteLocalAddress(), NetHelper.getUsableLocalPort(port));
    }

    public WebSocketServerEngine(InetAddress siteLocalAddress, int port) {
        super(new InetSocketAddress(siteLocalAddress, port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        log.info("socket open success: {}", conn.getRemoteSocketAddress());
        if (null != clientListener) {
            clientListener.onOpen(conn);
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        log.info("socket onClose. code: {},reason:{},remote:{}", code, reason, remote);
        if (null != clientListener) {
            clientListener.onClose(conn, code, reason, remote);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        log.info("onMessage:{}", Base64.encode(bytes));
        ECRHubResponseProto.ECRHubResponse respProto;
        try {
            respProto = ECRHubProtobufHelper.unpack(bytes);
        } catch (ECRHubException e) {
            throw new RuntimeException(e);
        }
        MSG_CACHE.put(respProto.getMsgId(), message);
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
        running = false;
    }

    public String receive(String msgId, long timeout) throws ECRHubTimeoutException {
        long before = System.currentTimeMillis();
        while (true) {
            String msg = MSG_CACHE.get(msgId);
            if (StrUtil.isNotBlank(msg)) {
                MSG_CACHE.remove(msgId);
                return msg;
            } else {
                ThreadUtil.safeSleep(200);
                if (System.currentTimeMillis() - before > timeout) {
                    throw new ECRHubTimeoutException();
                }
            }
        }
    }
}