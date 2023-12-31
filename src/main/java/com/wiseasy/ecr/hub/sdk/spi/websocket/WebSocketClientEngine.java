package com.wiseasy.ecr.hub.sdk.spi.websocket;

import cn.hutool.cache.impl.FIFOCache;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubTimeoutException;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubProtobufHelper;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubResponseProto;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.charset.StandardCharsets;

public class WebSocketClientEngine extends WebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(WebSocketClientEngine.class);

    private final FIFOCache<String, String> MSG_CACHE = new FIFOCache<>(20, 10 * 60 * 1000);

    public WebSocketClientEngine(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        log.info("socket open success:{}", handshakedata);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.info("socket onClose. code:{},reason:{},remote:{}", code, reason, remote);
    }

    @Override
    public void onError(Exception ex) {
        log.error("socket onError. ", ex);
    }

    @Override
    public void onMessage(String message) {
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

    public String receive(String msgId, long timeout) throws ECRHubTimeoutException {
        long before = System.currentTimeMillis();
        while (true) {
            String msg = MSG_CACHE.get(msgId);
            if (StrUtil.isNotBlank(msg)) {
                MSG_CACHE.remove(msgId);
                return msg;
            } else {
                ThreadUtil.safeSleep(20);
                if (System.currentTimeMillis() - before > timeout) {
                    throw new ECRHubTimeoutException();
                }
            }
        }
    }
}