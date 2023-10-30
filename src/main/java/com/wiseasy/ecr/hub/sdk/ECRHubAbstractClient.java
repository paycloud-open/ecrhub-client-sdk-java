package com.wiseasy.ecr.hub.sdk;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSONObject;
import com.wiseasy.ecr.hub.sdk.enums.ETopic;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubTimeoutException;
import com.wiseasy.ecr.hub.sdk.model.request.ECRHubRequest;
import com.wiseasy.ecr.hub.sdk.model.response.ECRHubResponse;
import com.wiseasy.ecr.hub.sdk.model.response.ECRHubResponse.DeviceData;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubProtobufHelper;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubRequestProto;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubResponseProto;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubResponseProto.ResponseDeviceData;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubResponseProto.ResponseBizData;
import com.wiseasy.ecr.hub.sdk.utils.NetHelper;

import java.util.Optional;

public abstract class ECRHubAbstractClient implements ECRHubClient {

    private final ECRHubConfig config;

    public ECRHubAbstractClient(ECRHubConfig config) {
        this.config = config;
    }

    public ECRHubConfig getConfig() {
        return config;
    }

    @Override
    public <T extends ECRHubResponse> T execute(ECRHubRequest<T> request) throws ECRHubException {
        autoConnect();
        sendReq(request);
        return getResp(request);
    }

    @Override
    public <T extends ECRHubResponse> void asyncExecute(ECRHubRequest<T> request, ECRHubResponseCallBack<T> callback) throws ECRHubException {
        autoConnect();
        sendReq(request);
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

    protected abstract ECRHubResponseProto.ECRHubResponse sendReq(ECRHubRequestProto.ECRHubRequest request, long startTime) throws ECRHubException;

    protected ECRHubResponse pair(long startTime) throws ECRHubException {
        ECRHubResponseProto.ECRHubResponse resp = sendReq(buildPairReq(), startTime);
        ECRHubResponse response = buildResp(ECRHubResponse.class, resp);
        if (!response.isSuccess()) {
            throw new ECRHubException(response.getError_msg());
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
        if (respBuffer == null) {
            return null;
        } else {
            ECRHubResponseProto.ECRHubResponse resp = ECRHubProtobufHelper.unpack(respBuffer);
            return buildResp(respClass, resp);
        }
    }

    protected <T extends ECRHubResponse> T buildResp(Class<T> respClass, ECRHubResponseProto.ECRHubResponse resp) throws ECRHubException {
        ResponseDeviceData respDeviceData = resp.getDeviceData();
        JSONObject deviceDataJson = ECRHubProtobufHelper.proto2Json(respDeviceData);
        DeviceData deviceData = deviceDataJson.toJavaObject(DeviceData.class);

        ResponseBizData respBizData = resp.getBizData();
        JSONObject respDataJson = ECRHubProtobufHelper.proto2Json(respBizData);

        T response = respDataJson.toJavaObject(respClass);
        response.setRequest_id(resp.getRequestId());
        response.setSuccess(resp.getSuccess());
        response.setError_msg(resp.getErrorMsg());
        response.setDevice_data(deviceData);
        return response;
    }
}