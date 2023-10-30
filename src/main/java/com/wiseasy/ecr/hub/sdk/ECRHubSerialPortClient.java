package com.wiseasy.ecr.hub.sdk;

import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.model.request.ECRHubRequest;
import com.wiseasy.ecr.hub.sdk.model.response.ECRHubResponse;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubProtobufHelper;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubRequestProto;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubResponseProto;
import com.wiseasy.ecr.hub.sdk.sp.serialport.SerialPortEngine;

import java.util.Base64;
import java.util.Optional;

public class ECRHubSerialPortClient extends ECRHubAbstractClient {

    private final SerialPortEngine engine;

    public ECRHubSerialPortClient(String port, ECRHubConfig config) throws ECRHubException {
        super(config);
        this.engine = new SerialPortEngine(port, config.getSerialPortConfig());
    }

    @Override
    public boolean connect() throws ECRHubException {
        long startTime = System.currentTimeMillis();
        engine.connect(startTime);
        return true;
    }

    @Override
    public ECRHubResponse connect2() throws ECRHubException {
        long startTime = System.currentTimeMillis();
        engine.connect(startTime);
        return pair(startTime);
    }

    @Override
    public boolean isConnected() throws ECRHubException {
        return engine.isConnected() && engine.isReceivedHeart();
    }

    @Override
    public boolean disconnect() throws ECRHubException {
         engine.disconnect();
         return true;
    }

    @Override
    protected void autoConnect() throws ECRHubException {
        if (engine.isConnected()) {
            if (!engine.isReceivedHeart()) {
                throw new ECRHubException("Serial port is not connected, " +
                        "please check the USB cable is connected and POS terminal cashier App is launched");
            }
        } else {
            if (!this.connect()) {
                throw new ECRHubException("Serial port connection failed");
            }
        }
    }

    @Override
    protected ECRHubResponseProto.ECRHubResponse sendReq(ECRHubRequestProto.ECRHubRequest request, long startTime) throws ECRHubException {
        long timeout = getConfig().getSerialPortConfig().getConnTimeout();

        byte[] sendBuffer = Base64.getEncoder().encode(request.toByteArray());
        engine.write(sendBuffer, startTime, timeout);

        byte[] readBuffer = engine.read(request.getRequestId(), startTime, timeout);
        return ECRHubProtobufHelper.unpack(readBuffer);
    }

    @Override
    protected <T extends ECRHubResponse> void sendReq(ECRHubRequest<T> request) throws ECRHubException {
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
        return buildResp(request.getResponseClass(), buffer);
    }
}