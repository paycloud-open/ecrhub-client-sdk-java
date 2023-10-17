package com.wiseasy.ecr.hub.sdk;

import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubTimeoutException;
import com.wiseasy.ecr.hub.sdk.model.request.ECRHubRequest;
import com.wiseasy.ecr.hub.sdk.model.response.ECRHubResponse;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubProtobufHelper;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubRequestProto;
import com.wiseasy.ecr.hub.sdk.sp.websocket.WebSocketClientEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class ECRHubWebSocketClient extends ECRHubAbstractClient {

    private static final Logger log = LoggerFactory.getLogger(ECRHubWebSocketClient.class);

    private volatile boolean connected = false;

    private final WebSocketClientEngine engine;

    public ECRHubWebSocketClient(String url, ECRHubConfig config) throws ECRHubException {
        super(config);
        try {
            this.engine = new WebSocketClientEngine(new URI(url));
        } catch (Exception e) {
            throw new ECRHubException("ecrWebSocketClient error", e);
        }
    }

    @Override
    public boolean connect() throws ECRHubException {
        return connect2().isSuccess();
    }

    @Override
    public ECRHubResponse connect2() throws ECRHubException {
        int connTimeout = getConfig().getSocketConfig().getConnTimeout();
        log.info("Connecting...");

        boolean success;
        try {
            success = engine.connectBlocking(connTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new ECRHubTimeoutException();
        }
        if (!success) {
            throw new ECRHubException("Connection failed");
        }

        ECRHubResponse response = pair(System.currentTimeMillis(), connTimeout);
        connected = response.isSuccess();
        log.info("Connection successful");

        return response;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public boolean disconnect() throws ECRHubException {
        try {
            engine.closeBlocking();
            connected = false;
            return true;
        } catch (InterruptedException e) {
            throw new ECRHubException("disconnect error", e);
        }
    }

    @Override
    protected byte[] sendPairReq(ECRHubRequestProto.ECRHubRequest request, long startTime, int timeout) throws ECRHubException {
        engine.send(new String(request.toByteArray()));
        String receive = engine.receive(request.getMsgId(), startTime, timeout);
        return receive.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected <T extends ECRHubResponse> void sendReq(ECRHubRequest<T> request) throws ECRHubException {
        byte[] msg = ECRHubProtobufHelper.pack(request);
        engine.send(new String(msg));
    }

    @Override
    protected <T extends ECRHubResponse> T getResp(ECRHubRequest<T> request) throws ECRHubException {
        ECRHubConfig config = request.getConfig();
        long timeout = config != null ? config.getSocketConfig().getSocketTimeout() : DEF_READ_TIMEOUT;
        String msg = engine.receive(request.getMsg_id(), System.currentTimeMillis(), timeout);
        return decodeRespPack(msg.getBytes(StandardCharsets.UTF_8), request.getResponseClass());
    }
}