package com.wiseasy.ecr.hub.sdk;

import cn.hutool.core.util.IdUtil;
import com.wiseasy.ecr.hub.sdk.enums.ETopic;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.model.request.ECRHubRequest;
import com.wiseasy.ecr.hub.sdk.model.response.ECRHubResponse;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubProtobufHelper;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubRequestProto;
import com.wiseasy.ecr.hub.sdk.sp.serialport.SerialPortEngine;
import com.wiseasy.ecr.hub.sdk.sp.serialport.SerialPortPacket;
import com.wiseasy.ecr.hub.sdk.utils.HexUtil;
import com.wiseasy.ecr.hub.sdk.utils.NetHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ECRHubSerialPortClient extends ECRHubAbstractClient {

    private static final Logger log = LoggerFactory.getLogger(ECRHubSerialPortClient.class);

    private volatile boolean isConnected = false;
    private final Lock lock = new ReentrantLock();
    private final SerialPortEngine engine;

    public ECRHubSerialPortClient(String port, ECRHubConfig config) throws ECRHubException {
        super(config);
        this.engine = new SerialPortEngine(port, config.getSerialPortConfig());
    }

    @Override
    public boolean connect() throws ECRHubException {
        return connect2().isSuccess();
    }

    @Override
    public ECRHubResponse connect2() throws ECRHubException {
        lock.lock();
        try {
            log.info("Connecting...");

            long startTime = System.currentTimeMillis();
            int timeout = getConfig().getSerialPortConfig().getConnTimeout();
            engine.connect(startTime, timeout);

            ECRHubResponse response = pair(startTime, timeout);
            isConnected = response.isSuccess();

            log.info("Connection successful");
            return response;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isConnected() throws ECRHubException {
        return engine.isOpen() && isConnected;
    }

    @Override
    public boolean disconnect() throws ECRHubException {
        lock.lock();
        try {
            log.info("Disconnecting...");
            boolean isClosed = engine.close();
            if (isClosed) {
                isConnected = false;
                log.info("Disconnect successful");
            }
            return isClosed;
        } finally {
            lock.unlock();
        }
    }

    @Override
    protected void sendReq(ECRHubRequest request) throws ECRHubException {
        if (!isConnected()) {
            throw new ECRHubException("The serial port is not connected.");
        }
        byte[] msg = ECRHubProtobufHelper.pack(getConfig(), request);
        byte[] pack = new SerialPortPacket.MsgPacket(msg).encode();
        log.debug("Send data packet:{}", HexUtil.byte2hex(pack));
        engine.write(pack);
    }

    @Override
    protected <T extends ECRHubResponse> T getResp(ECRHubRequest<T> request) throws ECRHubException {
        ECRHubConfig config = request.getConfig();
        long timeout = config != null ? config.getSerialPortConfig().getReadTimeout() : DEF_READ_TIMEOUT;
        long startTime = System.currentTimeMillis();
        byte[] respPack = engine.read(request.getMsg_id(), startTime, timeout);
        return decodeRespPack(respPack, request.getResponseClass());
    }

    private ECRHubResponse pair(long startTime, int timeout) throws ECRHubException {
        log.info("Start pairing");
        ECRHubRequestProto.ECRHubRequest request = buildPairRequest();
        byte[] pack = new SerialPortPacket.MsgPacket(request.toByteArray()).encode();
        log.debug("Send pairing packet:{}", HexUtil.byte2hex(pack));
        engine.write(pack);

        byte[] respPack = engine.read(request.getMsgId(), startTime, timeout);
        ECRHubResponse response = decodeRespPack(respPack, ECRHubResponse.class);
        if (response.isSuccess()) {
            log.info("Successful pairing");
            return response;
        } else {
            log.error("Failed pairing: {}", response.getError_msg());
            throw new ECRHubException(response.getError_msg());
        }
    }

    private ECRHubRequestProto.ECRHubRequest buildPairRequest() {
        String deviceName = Optional.ofNullable(getConfig().getDeviceName()).orElse(NetHelper.getLocalHostName());
        String aliasName = Optional.ofNullable(getConfig().getAliasName()).orElse(deviceName);
        String macAddress = NetHelper.getLocalMacAddress();

        return ECRHubRequestProto.ECRHubRequest.newBuilder()
                .setTimestamp(String.valueOf(System.currentTimeMillis()))
                .setMsgId(IdUtil.fastSimpleUUID())
                .setTopic(ETopic.PAIR.getValue())
                .setPairData(ECRHubRequestProto.RequestPairData.newBuilder()
                            .setDeviceName(deviceName)
                            .setAliasName(aliasName)
                            .setMacAddress(macAddress)
                            .build())
                .build();
    }
}