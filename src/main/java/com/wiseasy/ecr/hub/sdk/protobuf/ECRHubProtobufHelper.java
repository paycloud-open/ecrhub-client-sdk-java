package com.wiseasy.ecr.hub.sdk.protobuf;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.model.request.ECRHubRequest;
import com.wiseasy.ecr.hub.sdk.utils.NetHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class ECRHubProtobufHelper {

    private static final Logger log = LoggerFactory.getLogger(ECRHubProtobufHelper.class);

    public static byte[] pack(ECRHubRequest request) throws ECRHubException {
        return ECRHubRequestProto.ECRHubRequest.newBuilder()
                .setTimestamp(String.valueOf(System.currentTimeMillis()))
                .setRequestId(request.getRequest_id())
                .setVersion(request.getVersion())
                .setAppId(Optional.ofNullable(request.getApp_id()).orElse(""))
                .setTopic(request.getTopic())
                .setDeviceData(buildDeviceData())
                .setBizData(buildBizData(request))
                .setVoiceData(buildVoiceData(request))
                .setPrinterData(buildPrintData(request))
                .setNotifyData(buildNotifyData(request))
                .build().toByteArray();
    }

    public static ECRHubResponseProto.ECRHubResponse unpack(byte[] pack) throws ECRHubException {
        try {
            return ECRHubResponseProto.ECRHubResponse.parseFrom(pack);
        } catch (Exception e) {
            log.error("Invalid ProtocolBuffer Message:", e);
            throw new ECRHubException("Invalid ProtocolBuffer Message:", e);
        }
    }

    public static JSONObject proto2Json(MessageOrBuilder message) throws ECRHubException {
        try {
            JsonFormat.Printer printer = JsonFormat.printer().omittingInsignificantWhitespace();
            return JSONObject.parseObject(printer.print(message));
        } catch (Exception e) {
            log.error("Invalid ProtocolBuffer Message:", e);
            throw new ECRHubException("Invalid ProtocolBuffer Message:", e);
        }
    }

    public static ECRHubRequestProto.RequestDeviceData buildDeviceData() {
        return ECRHubRequestProto.RequestDeviceData.newBuilder()
                .setMacAddress(NetHelper.getLocalMacAddress())
                .build();
    }

    public static ECRHubRequestProto.RequestBizData buildBizData(ECRHubRequest request) throws ECRHubException {
        try {
            ECRHubRequestProto.RequestBizData.Builder builder = ECRHubRequestProto.RequestBizData.newBuilder();
            JsonFormat.parser().ignoringUnknownFields().merge(JSON.toJSONString(request), builder);
            return builder.build();
        } catch (Exception e) {
            log.error("Build BizData Error:", e);
            throw new ECRHubException("Build BizData Error:", e);
        }
    }

    public static ECRHubRequestProto.VoiceData buildVoiceData(ECRHubRequest request) throws ECRHubException {
        try {
            ECRHubRequestProto.VoiceData.Builder builder = ECRHubRequestProto.VoiceData.newBuilder();
            JsonFormat.parser().ignoringUnknownFields().merge(JSON.toJSONString(request.getVoice_data()), builder);
            return builder.build();
        } catch (Exception e) {
            log.error("Build VoiceData Error:", e);
            throw new ECRHubException("Build VoiceData Error:", e);
        }
    }

    public static ECRHubRequestProto.PrinterData buildPrintData(ECRHubRequest request) throws ECRHubException {
        try {
            ECRHubRequestProto.PrinterData.Builder builder = ECRHubRequestProto.PrinterData.newBuilder();
            JsonFormat.parser().ignoringUnknownFields().merge(JSON.toJSONString(request.getPrinter_data()), builder);
            return builder.build();
        } catch (Exception e) {
            log.error("Build PrinterData Error:", e);
            throw new ECRHubException("Build PrinterData Error:", e);
        }
    }

    public static ECRHubRequestProto.NotifyData buildNotifyData(ECRHubRequest request) throws ECRHubException {
        try {
            ECRHubRequestProto.NotifyData.Builder builder = ECRHubRequestProto.NotifyData.newBuilder();
            JsonFormat.parser().ignoringUnknownFields().merge(JSON.toJSONString(request.getNotify_data()), builder);
            return builder.build();
        } catch (Exception e) {
            log.error("Build NotifyData Error:", e);
            throw new ECRHubException("Build NotifyData Error:", e);
        }
    }
}