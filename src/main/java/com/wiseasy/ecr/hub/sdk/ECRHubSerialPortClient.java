package com.wiseasy.ecr.hub.sdk;

import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.model.request.ECRHubRequest;
import com.wiseasy.ecr.hub.sdk.model.response.ECRHubResponse;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubProtobufHelper;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubRequestProto;
import com.wiseasy.ecr.hub.sdk.sp.serialport.SerialPortEngine;
import com.wiseasy.ecr.hub.sdk.sp.serialport.SerialPortPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    protected byte[] sendPairReq(ECRHubRequestProto.ECRHubRequest request, long startTime, int timeout) throws ECRHubException {
        SerialPortPacket pack = new SerialPortPacket.MsgPacket(request.toByteArray());
        engine.addQueue(pack);
        return engine.read(request.getMsgId(), startTime, timeout);
    }

    @Override
    protected <T extends ECRHubResponse> void sendReq(ECRHubRequest<T> request) throws ECRHubException {
        if (!isConnected()) {
            throw new ECRHubException("The serial port is not connected.");
        }
        byte[] msg = ECRHubProtobufHelper.pack(request);
        SerialPortPacket pack = new SerialPortPacket.MsgPacket(msg);
        log.debug("Send data packet:\n{}", pack);
        engine.addQueue(pack);
    }

    @Override
    protected <T extends ECRHubResponse> T getResp(ECRHubRequest<T> request) throws ECRHubException {
        ECRHubConfig config = request.getConfig();
        long timeout = config != null ? config.getSerialPortConfig().getReadTimeout() : DEF_READ_TIMEOUT;
        byte[] respPack = engine.read(request.getMsg_id(), System.currentTimeMillis(), timeout);
        return decodeRespPack(respPack, request.getResponseClass());
    }
}