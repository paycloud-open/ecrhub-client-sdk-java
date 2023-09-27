package com.wiseasy.ecr.hub.sdk;

import cn.hutool.core.thread.ThreadUtil;
import com.alibaba.fastjson2.JSONObject;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubTimeoutException;
import com.wiseasy.ecr.hub.sdk.model.request.ECRHubRequest;
import com.wiseasy.ecr.hub.sdk.model.response.ECRHubResponse;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubProtobufHelper;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubResponseProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ECRHubAbstractClient implements ECRHubClient {

    private static final Logger log = LoggerFactory.getLogger(ECRHubAbstractClient.class);

    /**
     * Default synchronous read data timeout (milliseconds)
     */
    public static final long DEF_READ_TIMEOUT = 5 * 60 * 1000;

    private final ECRHubConfig config;

    public ECRHubAbstractClient(ECRHubConfig config) {
        this.config = config;
    }

    public ECRHubConfig getConfig() {
        return config;
    }

    @Override
    public <T extends ECRHubResponse> T execute(ECRHubRequest<T> request) throws ECRHubException {
        sendReq(request);

        return getResp(request);
    }

    @Override
    public <T extends ECRHubResponse> void asyncExecute(ECRHubRequest<T> request, ECRHubResponseCallBack<T> callback) throws ECRHubException {
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

    protected <T extends ECRHubResponse> T decodeRespPack(byte[] respPack, Class<T> respClass) {
        if (respPack == null || respPack.length == 0) {
            return null;
        }

        ECRHubResponseProto.ECRHubResponse respProto = null;
        try {
            respProto = ECRHubProtobufHelper.unpack(respPack);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }

        ECRHubResponseProto.ResponseBizData bizData = respProto.getBizData();
        JSONObject bizDataJson = null;
        try {
            bizDataJson = ECRHubProtobufHelper.proto2Json(bizData);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }

        T resp = bizDataJson.toJavaObject(respClass);
        resp.setMsg_id(respProto.getMsgId());
        resp.setSuccess(respProto.getSuccess());
        resp.setError_msg(respProto.getErrorMsg());
        return resp;
    }
}