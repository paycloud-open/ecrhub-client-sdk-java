package com.wiseasy.ecr.hub.sdk;

import cn.hutool.core.util.ArrayUtil;
import com.alibaba.fastjson2.JSONObject;
import com.wiseasy.ecr.hub.sdk.model.response.ECRHubResponse;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubProtobufHelper;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubResponseProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author wangyuxiang@wiseasy.com
 * @since 2023-10-13 09:39
 */
public abstract class ECRHubAbstractServer implements ECRHubServer {

    private static final Logger log = LoggerFactory.getLogger(ECRHubAbstractServer.class);

    protected final ECRHubConfig config;
    public static final long DEF_READ_TIMEOUT = 5 * 60 * 1000;


    public ECRHubAbstractServer(ECRHubConfig config) {
        this.config = config;
    }

    public ECRHubConfig getConfig() {
        return config;
    }

    protected <T extends ECRHubResponse> T decodeRespPack(byte[] respPack, Class<T> respClass) {
        if (ArrayUtil.isEmpty(respPack)) {
            return null;
        }

        ECRHubResponseProto.ECRHubResponse respProto;
        try {
            respProto = ECRHubProtobufHelper.unpack(respPack);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }

        ECRHubResponseProto.ResponseBizData bizData = respProto.getBizData();
        JSONObject bizDataJson;
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
