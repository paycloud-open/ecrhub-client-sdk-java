package com.wiseasy.ecr.hub.sdk;

import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.model.request.ECRHubRequest;
import com.wiseasy.ecr.hub.sdk.model.request.PairRequest;
import com.wiseasy.ecr.hub.sdk.model.response.ECRHubResponse;
import com.wiseasy.ecr.hub.sdk.model.response.PairResponse;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubProtobufHelper;
import com.wiseasy.ecr.hub.sdk.spi.serialport.SerialPortEngine;
import com.wiseasy.ecr.hub.sdk.spi.serialport.SerialPortPacket;
import com.wiseasy.ecr.hub.sdk.utils.HexUtil;
import com.wiseasy.ecr.hub.sdk.utils.NetHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ECRHubSerialPortClient extends ECRHubAbstractClient {

    private static final Logger log = LoggerFactory.getLogger(ECRHubSerialPortClient.class);

    private final Lock lock = new ReentrantLock();
    private final SerialPortEngine engine;
    private volatile boolean isConnected = false;
    private volatile boolean isPaired = false;

    public ECRHubSerialPortClient(String port, ECRHubConfig config) throws ECRHubException {
        super(config);
        this.engine = new SerialPortEngine(port, config.getSerialPortConfig());
    }

    @Override
    public boolean connect() throws ECRHubException {
        log.info("Connecting...");
        lock.lock();
        try {
            long startTime = System.currentTimeMillis();
            int timeout = getConfig().getSerialPortConfig().getConnTimeout();
            if (!isConnected) {
                engine.connect(startTime, timeout);
                isConnected = true;
            }
            if (!isPaired) {
                doPair(startTime, timeout);
                isPaired = true;
            }
            if (isConnected && isPaired) {
                log.info("Connection successful");
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isConnected() throws ECRHubException {
        return isConnected && isPaired;
    }

    @Override
    public boolean disconnect() throws ECRHubException {
        lock.lock();
        try {
            boolean isClosed = engine.close();
            if (isClosed) {
                isConnected = false;
                isPaired = false;
            }
            return isClosed;
        } finally {
            lock.unlock();
        }
    }

    protected void doPair(long startTime, int timeout) throws ECRHubException {
        log.info("Start pairing");
        PairRequest request = buildPairRequest();
        byte[] msg = ECRHubProtobufHelper.pack(getConfig(), request);
        byte[] pack = new SerialPortPacket.MsgPacket(msg).encode();
        log.debug("Send pairing packet:{}", HexUtil.byte2hex(pack));
        engine.write(pack);

        byte[] respPack = engine.read(request.getMsg_id(), startTime, timeout);
        PairResponse response = decodeRespPack(respPack, request.getResponseClass());
        if (response.isSuccess()) {
            log.info("Successful pairing");
        } else {
            log.error("Failed pairing: {}", response.getError_msg());
            throw new ECRHubException(response.getError_msg());
        }
    }

    private PairRequest buildPairRequest() {
        ECRHubConfig config = getConfig();
        String deviceName = Optional.ofNullable(config.getDeviceName()).orElse(NetHelper.getLocalHostName());
        String aliasName = Optional.ofNullable(config.getAliasName()).orElse(deviceName);

        PairRequest request = new PairRequest();
        request.setDevice_name(deviceName);
        request.setAlias_name(aliasName);
        request.setMac_address(NetHelper.getLocalMacAddress());
        return request;
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
}