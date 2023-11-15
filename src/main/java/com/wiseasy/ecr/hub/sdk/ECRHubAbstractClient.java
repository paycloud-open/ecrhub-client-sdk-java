package com.wiseasy.ecr.hub.sdk;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSONObject;
import com.wiseasy.ecr.hub.sdk.enums.ETopic;
import com.wiseasy.ecr.hub.sdk.enums.ETransStatus;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubConnectionException;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubTimeoutException;
import com.wiseasy.ecr.hub.sdk.model.request.ECRHubRequest;
import com.wiseasy.ecr.hub.sdk.model.response.ECRHubResponse;
import com.wiseasy.ecr.hub.sdk.model.response.ECRHubResponse.DeviceData;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubProtobufHelper;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubRequestProto;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubResponseProto;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubResponseProto.ResponseBizData;
import com.wiseasy.ecr.hub.sdk.utils.NetHelper;

import java.util.Optional;

public abstract class ECRHubAbstractClient implements ECRHubClient {

    private final ECRHubConfig config;

    protected ECRHubAbstractClient(ECRHubConfig config) {
        this.config = config;
    }

    public ECRHubConfig getConfig() {
        return config;
    }

    @Override
    public <T extends ECRHubResponse> T execute(ECRHubRequest<T> request) throws ECRHubException {
        // Auto connect
        autoConnect();

        // Send request
        sendReq(request);

        // Sync receive response
        return getResp(request);
    }

    @Override
    public <T extends ECRHubResponse> void asyncExecute(ECRHubRequest<T> request, ECRHubResponseCallBack<T> callback) throws ECRHubException {
        // Auto connect
        autoConnect();

        // Send request
        sendReq(request);

        // Async receive response
        ThreadUtil.execute(() -> {
            try {
                callback.onResponse(getResp(request));
            } catch (ECRHubTimeoutException e) {
                callback.onTimeout(e);
            } catch (ECRHubException e) {
                callback.onError(e);
            } catch (Exception e) {
                callback.onError(new ECRHubException(e));
            }
        });
    }

    protected void autoConnect() throws ECRHubException {
    }

    protected abstract <T extends ECRHubResponse> void sendReq(ECRHubRequest<T> request) throws ECRHubException;

    protected abstract <T extends ECRHubResponse> T getResp(ECRHubRequest<T> request) throws ECRHubException;

    protected abstract ECRHubResponseProto.ECRHubResponse send(ECRHubRequestProto.ECRHubRequest request, long startTime) throws ECRHubException;

    protected ECRHubResponse pair(long startTime) throws ECRHubException {
        ECRHubResponseProto.ECRHubResponse resp = send(buildPairReq(), startTime);
        ECRHubResponse response = buildResp(ECRHubResponse.class, resp);
        if (!response.isSuccess()) {
            throw new ECRHubConnectionException(response.getError_msg());
        }
        return response;
    }

    protected ECRHubRequestProto.ECRHubRequest buildPairReq() {
        String hostName = Optional.ofNullable(config.getHostName()).orElse(NetHelper.getLocalHostName());
        String aliasName = Optional.ofNullable(config.getAliasName()).orElse(hostName);
        String macAddress = NetHelper.getLocalMacAddress();

        return ECRHubRequestProto.ECRHubRequest.newBuilder()
              .setTimestamp(String.valueOf(System.currentTimeMillis()))
              .setRequestId(IdUtil.fastSimpleUUID())
              .setTopic(ETopic.PAIR.getVal())
              .setDeviceData(ECRHubRequestProto.RequestDeviceData.newBuilder()
                            .setDeviceName(hostName)
                            .setAliasName(aliasName)
                            .setMacAddress(macAddress)
                            .build())
              .build();
    }

    protected <T extends ECRHubResponse> T buildResp(Class<T> respClass, byte[] respBuffer) throws ECRHubException {
        if (respBuffer != null) {
            ECRHubResponseProto.ECRHubResponse resp = ECRHubProtobufHelper.parseRespFrom(respBuffer);
            return buildResp(respClass, resp);
        } else {
            return null;
        }
    }

    protected <T extends ECRHubResponse> T buildResp(Class<T> respClass, ECRHubResponseProto.ECRHubResponse resp) throws ECRHubException {
        DeviceData deviceData = null;
        if (resp.hasDeviceData()) {
            JSONObject json = ECRHubProtobufHelper.toJson(resp.getDeviceData());
            deviceData = json.toJavaObject(DeviceData.class);
        }

        ResponseBizData respBizData = resp.getBizData();
        if (ETransStatus.TIMEOUT.getCode().equals(respBizData.getTransStatus())) {
            throw new ECRHubTimeoutException(resp.getErrorMsg());
        }

        JSONObject respDataJson = ECRHubProtobufHelper.toJson(respBizData);
        T response = respDataJson.toJavaObject(respClass);
        response.setRequest_id(resp.getRequestId());
        response.setSuccess(resp.getSuccess());
        response.setError_msg(resp.getErrorMsg());
        response.setDevice_data(deviceData);
        return response;
    }
}