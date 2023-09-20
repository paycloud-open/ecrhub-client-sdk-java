package com.wiseasy.ecr.hub.sdk;

import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.model.request.ECRHubRequest;
import com.wiseasy.ecr.hub.sdk.model.response.ECRHubResponse;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubProtobufHelper;
import com.wiseasy.ecr.hub.sdk.spi.serialport.SerialPortEngine;
import com.wiseasy.ecr.hub.sdk.spi.serialport.SerialPortPacket;
import com.wiseasy.ecr.hub.sdk.utils.HexUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ECRHubSerialPortClient extends ECRHubAbstractClient {

    private static final Logger log = LoggerFactory.getLogger(ECRHubSerialPortClient.class);

    private final SerialPortEngine engine;

    public ECRHubSerialPortClient(String port, ECRHubConfig config) throws ECRHubException {
        super(config);
        this.engine = new SerialPortEngine(port, config);
    }

    @Override
    public boolean connect() throws ECRHubException {
        return engine.connect();
    }

    @Override
    public boolean isConnected() throws ECRHubException {
        return engine.isConnected();
    }

    @Override
    public boolean disconnect() throws ECRHubException {
        return engine.close();
    }

    @Override
    protected void sendReq(ECRHubRequest request) throws ECRHubException {
        byte[] msg = ECRHubProtobufHelper.pack(getConfig(), request);
        byte[] pack = new SerialPortPacket.MsgPacket(msg).encode();
        log.debug("Send data packet:{}", HexUtil.byte2hex(pack));
        engine.write(pack);
    }

    @Override
    protected <T extends ECRHubResponse> T getResp(ECRHubRequest<T> request) throws ECRHubException {
        ECRHubConfig config = request.getConfig();
        long timeout = config != null ? config.getSerialPortConfig().getReadTimeout() : DEF_READ_TIMEOUT;
        byte[] respPack = engine.read(request.getMsg_id(), timeout);
        return decodeRespPack(respPack, request.getResponseClass());
    }
}