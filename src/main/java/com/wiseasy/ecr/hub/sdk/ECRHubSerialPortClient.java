package com.wiseasy.ecr.hub.sdk;

import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.model.request.ECRHubRequest;
import com.wiseasy.ecr.hub.sdk.model.response.ECRHubResponse;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubProtobufHelper;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubRequestProto;
import com.wiseasy.ecr.hub.sdk.sp.serialport.SerialPortEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ECRHubSerialPortClient extends ECRHubAbstractClient {

    private static final Logger log = LoggerFactory.getLogger(ECRHubSerialPortClient.class);

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
        long startTime = System.currentTimeMillis();
        lock.lock();
        try {
            log.info("Serial port connecting...");

            engine.connect(startTime);
            ECRHubResponse response = pair(startTime);

            log.info("Serial port connection successful.");

            return response;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isConnected() throws ECRHubException {
        lock.lock();
        try {
            return engine.isOpen();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean disconnect() throws ECRHubException {
        lock.lock();
        try {
            log.info("Serial port disconnecting...");

            boolean isClosed = engine.close();
            if (isClosed) {
                log.info("Serial port disconnect successful.");
            }

            return isClosed;
        } finally {
            lock.unlock();
        }
    }

    private void autoConnect() throws ECRHubException {
        if (!isConnected()) {
            boolean success = connect();
            if (!success) {
                throw new ECRHubException("Serial port is not connected.");
            }
        }
    }

    @Override
    protected byte[] sendPairReq(ECRHubRequestProto.ECRHubRequest request, long startTime) throws ECRHubException {
        long timeout = getConfig().getSerialPortConfig().getConnTimeout();
        engine.write(request.toByteArray(), startTime, timeout);
        return engine.read(request.getRequestId(), startTime, timeout);
    }

    @Override
    protected <T extends ECRHubResponse> void sendReq(ECRHubRequest<T> request) throws ECRHubException {
        autoConnect();

        ECRHubConfig config = Optional.ofNullable(request.getConfig()).orElse(super.getConfig());
        long timeout = config.getSerialPortConfig().getWriteTimeout();

        byte[] buffer = ECRHubProtobufHelper.pack(request);
        engine.write(buffer, System.currentTimeMillis(), timeout);
    }

    @Override
    protected <T extends ECRHubResponse> T getResp(ECRHubRequest<T> request) throws ECRHubException {
        ECRHubConfig config = Optional.ofNullable(request.getConfig()).orElse(super.getConfig());
        long timeout = config.getSerialPortConfig().getReadTimeout();

        byte[] buffer = engine.read(request.getRequest_id(), System.currentTimeMillis(), timeout);
        return decodeRespPack(buffer, request.getResponseClass());
    }
}