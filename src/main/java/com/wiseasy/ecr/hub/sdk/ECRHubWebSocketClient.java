package com.wiseasy.ecr.hub.sdk;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import com.wiseasy.ecr.hub.sdk.enums.ETopic;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubTimeoutException;
import com.wiseasy.ecr.hub.sdk.model.request.ECRHubRequest;
import com.wiseasy.ecr.hub.sdk.model.response.ECRHubResponse;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubProtobufHelper;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubRequestProto;
import com.wiseasy.ecr.hub.sdk.sp.websocket.WebSocketClientEngine;
import com.wiseasy.ecr.hub.sdk.utils.NetHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class ECRHubWebSocketClient extends ECRHubAbstractClient {

    private static final Logger log = LoggerFactory.getLogger(ECRHubWebSocketClient.class);

    private volatile boolean connected = false;

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
        connect2();
        return connected;
    }

    @Override
    public ECRHubResponse connect2() throws ECRHubException {
        int connTimeout = getConfig().getSocketConfig().getConnTimeout();
        try {
            boolean b = engine.connectBlocking(connTimeout, TimeUnit.MILLISECONDS);
            if (!b) {
                throw new ECRHubException("connect failed");
            }
        } catch (InterruptedException e) {
            throw new ECRHubTimeoutException();
        }
        ECRHubResponse pair = pair(System.currentTimeMillis(), connTimeout);
        connected = pair.isSuccess();
        log.info("Connection successful");
        return pair;
    }

    private ECRHubRequestProto.ECRHubRequest buildPairRequest() {
        String hostName = Optional.ofNullable(getConfig().getHostName()).orElse(NetHelper.getLocalHostName());
        String aliasName = Optional.ofNullable(getConfig().getAliasName()).orElse(hostName);
        String macAddress = NetHelper.getLocalMacAddress();

        return ECRHubRequestProto.ECRHubRequest.newBuilder()
                .setTimestamp(String.valueOf(System.currentTimeMillis()))
                .setMsgId(IdUtil.fastSimpleUUID())
                .setTopic(ETopic.PAIR.getValue())
                .setDeviceData(ECRHubRequestProto.RequestDeviceData.newBuilder()
                        .setDeviceName(hostName)
                        .setAliasName(aliasName)
                        .setMacAddress(macAddress)
                        .build())
                .build();
    }

    private ECRHubResponse pair(long startTime, int timeout) throws ECRHubException {
        log.info("Start pairing");
        ECRHubRequestProto.ECRHubRequest request = buildPairRequest();
        engine.send(new String(request.toByteArray()));
        String receive = engine.receive(request.getMsgId(), startTime, timeout);
        ECRHubResponse response = decodeRespPack(receive.getBytes(StandardCharsets.UTF_8), ECRHubResponse.class);
        if (response.isSuccess()) {
            log.info("Successful pairing");
            return response;
        } else {
            log.error("Failed pairing: {}", response.getError_msg());
            throw new ECRHubException(response.getError_msg());
        }
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
    protected void sendReq(ECRHubRequest request) throws ECRHubException {
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