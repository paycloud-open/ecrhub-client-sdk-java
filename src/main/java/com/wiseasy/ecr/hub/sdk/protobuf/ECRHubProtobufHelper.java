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
        ECRHubRequestProto.ECRHubRequest.Builder builder = ECRHubRequestProto.ECRHubRequest.newBuilder();
        builder.setTimestamp(String.valueOf(System.currentTimeMillis()));
        builder.setVersion(request.getVersion());
        builder.setRequestId(request.getRequest_id());
        builder.setAppId(Optional.ofNullable(request.getApp_id()).orElse(""));
        builder.setTopic(request.getTopic());
        builder.setDeviceData(buildDeviceData());
        builder.setBizData(buildBizData(request));

        ECRHubRequest.VoiceData voiceData = request.getVoice_data();
        if (voiceData != null) {
            builder.setVoiceData(buildVoiceData(voiceData));
        }

        ECRHubRequest.PrinterData printerData = request.getPrinter_data();
        if (printerData != null) {
            builder.setPrinterData(buildPrintData(printerData));
        }

        ECRHubRequest.NotifyData notifyData = request.getNotify_data();
        if (notifyData != null) {
            builder.setNotifyData(buildNotifyData(notifyData));
        }

        return builder.build().toByteArray();
    }

    public static ECRHubRequestProto.ECRHubRequest parseReqFrom(byte[] buffer) throws ECRHubException {
        try {
            return ECRHubRequestProto.ECRHubRequest.parseFrom(buffer);
        } catch (Exception e) {
            log.error("Invalid ProtocolBuffer Message:", e);
            throw new ECRHubException("Invalid ProtocolBuffer Message:", e);
        }
    }

    public static ECRHubResponseProto.ECRHubResponse parseRespFrom(byte[] buffer) throws ECRHubException {
        try {
            return ECRHubResponseProto.ECRHubResponse.parseFrom(buffer);
        } catch (Exception e) {
            log.error("Invalid ProtocolBuffer Message:", e);
            throw new ECRHubException("Invalid ProtocolBuffer Message:", e);
        }
    }

    public static JSONObject toJson(MessageOrBuilder message) throws ECRHubException {
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

    public static ECRHubRequestProto.VoiceData buildVoiceData(ECRHubRequest.VoiceData voiceData) throws ECRHubException {
        try {
            ECRHubRequestProto.VoiceData.Builder builder = ECRHubRequestProto.VoiceData.newBuilder();
            JsonFormat.parser().ignoringUnknownFields().merge(JSON.toJSONString(voiceData), builder);
            return builder.build();
        } catch (Exception e) {
            log.error("Build VoiceData Error:", e);
            throw new ECRHubException("Build VoiceData Error:", e);
        }
    }

    public static ECRHubRequestProto.PrinterData buildPrintData(ECRHubRequest.PrinterData printerData) throws ECRHubException {
        try {
            ECRHubRequestProto.PrinterData.Builder builder = ECRHubRequestProto.PrinterData.newBuilder();
            JsonFormat.parser().ignoringUnknownFields().merge(JSON.toJSONString(printerData), builder);
            return builder.build();
        } catch (Exception e) {
            log.error("Build PrinterData Error:", e);
            throw new ECRHubException("Build PrinterData Error:", e);
        }
    }

    public static ECRHubRequestProto.NotifyData buildNotifyData(ECRHubRequest.NotifyData notifyData) throws ECRHubException {
        try {
            ECRHubRequestProto.NotifyData.Builder builder = ECRHubRequestProto.NotifyData.newBuilder();
            JsonFormat.parser().ignoringUnknownFields().merge(JSON.toJSONString(notifyData), builder);
            return builder.build();
        } catch (Exception e) {
            log.error("Build NotifyData Error:", e);
            throw new ECRHubException("Build NotifyData Error:", e);
        }
    }
}