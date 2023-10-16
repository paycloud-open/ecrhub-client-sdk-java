package com.wiseasy.ecr.hub.sdk;

import cn.hutool.core.thread.ThreadUtil;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.model.request.ECRHubRequest;
import com.wiseasy.ecr.hub.sdk.model.response.ECRHubResponse;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubProtobufHelper;
import com.wiseasy.ecr.hub.sdk.sp.websocket.WebSocketClientEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

public class ECRHubWebSocketClient extends ECRHubAbstractClient {

    private static final Logger log = LoggerFactory.getLogger(ECRHubWebSocketClient.class);

    private final WebSocketClientEngine engine;

    public ECRHubWebSocketClient(String url, ECRHubConfig config) throws ECRHubException {
        super(config);
        try {
            this.engine = new WebSocketClientEngine(new URI(url));
        } catch (URISyntaxException e) {
            throw new ECRHubException("ecrWebSocketClient error", e);
        }

    }

    @Override
    public boolean connect() throws ECRHubException {
        engine.connect();

        int timeout = getConfig().getSocketConfig().getConnTimeout();
        long before = System.currentTimeMillis();
        while (!isConnected()) {
            if (System.currentTimeMillis() - before > timeout) {
                throw new ECRHubException("Connection timeout");
            } else {
                log.info("connecting...");
                ThreadUtil.safeSleep(1000);
            }
        }

        log.info("Connection successful");

        return true;
    }

    @Override
    public boolean isConnected() throws ECRHubException {
        return engine.isOpen();
    }

    @Override
    public boolean disconnect() throws ECRHubException {
        try {
            engine.closeBlocking();
            return true;
        } catch (InterruptedException e) {
            throw new ECRHubException("disconnect error", e);
        }
    }

    @Override
    protected void sendReq(ECRHubRequest request) throws ECRHubException {
        byte[] msg = ECRHubProtobufHelper.pack(getConfig(), request);
        engine.send(new String(msg));
    }

    @Override
    protected <T extends ECRHubResponse> T getResp(ECRHubRequest<T> request) throws ECRHubException {
        ECRHubConfig config = request.getConfig();
        long timeout = config != null ? config.getSocketConfig().getSocketTimeout() : DEF_READ_TIMEOUT;
        String msg = engine.receive(request.getMsg_id(), timeout);
        return decodeRespPack(msg.getBytes(StandardCharsets.UTF_8), request.getResponseClass());
    }
}