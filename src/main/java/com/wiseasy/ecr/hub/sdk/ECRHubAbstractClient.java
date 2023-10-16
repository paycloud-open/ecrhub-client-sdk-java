package com.wiseasy.ecr.hub.sdk;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubTimeoutException;
import com.wiseasy.ecr.hub.sdk.model.request.ECRHubRequest;
import com.wiseasy.ecr.hub.sdk.model.response.ECRHubResponse;
import com.wiseasy.ecr.hub.sdk.model.response.ECRHubResponse.DeviceData;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubProtobufHelper;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubResponseProto;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubResponseProto.ResponseDeviceData;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubResponseProto.ResponseBizData;

public abstract class ECRHubAbstractClient implements ECRHubClient {

    /**
     * Default synchronous read data timeout (milliseconds)
     */
    protected static final long DEF_READ_TIMEOUT = 5 * 60 * 1000;

    private final ECRHubConfig config;

    public ECRHubAbstractClient(ECRHubConfig config) {
        this.config = config;
    }

    public ECRHubConfig getConfig() {
        return config;
    }

    @Override
    public ECRHubResponse connect2() throws ECRHubException {
        return null;
    }

    @Override
    public <T extends ECRHubResponse> T execute(ECRHubRequest<T> request) throws ECRHubException {
        if (StrUtil.isBlank(request.getApp_id())) {
            throw new ECRHubException("AppId cannot be empty.");
        }
        sendReq(request);
        return getResp(request);
    }

    @Override
    public <T extends ECRHubResponse> void asyncExecute(ECRHubRequest<T> request, ECRHubResponseCallBack<T> callback) throws ECRHubException {
        if (StrUtil.isBlank(request.getApp_id())) {
            throw new ECRHubException("AppId cannot be empty.");
        }
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

    protected abstract void sendReq(ECRHubRequest request) throws ECRHubException;

    protected abstract <T extends ECRHubResponse> T getResp(ECRHubRequest<T> request) throws ECRHubException;

    protected <T extends ECRHubResponse> T decodeRespPack(byte[] respPack, Class<T> respClass) throws ECRHubException {
        if (respPack == null || respPack.length == 0) {
            return null;
        } else {
            ECRHubResponseProto.ECRHubResponse respProto = ECRHubProtobufHelper.unpack(respPack);

            ResponseDeviceData deviceData = respProto.getDeviceData();
            JSONObject deviceDataJson = ECRHubProtobufHelper.proto2Json(deviceData);
            DeviceData device = deviceDataJson.toJavaObject(DeviceData.class);

            ResponseBizData bizData = respProto.getBizData();
            JSONObject bizDataJson = ECRHubProtobufHelper.proto2Json(bizData);

            T resp = bizDataJson.toJavaObject(respClass);
            resp.setMsg_id(respProto.getMsgId());
            resp.setSuccess(respProto.getSuccess());
            resp.setError_msg(respProto.getErrorMsg());
            resp.setDevice_data(device);
            return resp;
        }
    }
}