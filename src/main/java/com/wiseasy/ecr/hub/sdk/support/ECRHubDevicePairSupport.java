package com.wiseasy.ecr.hub.sdk.support;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import com.alibaba.fastjson2.JSONObject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.wiseasy.ecr.hub.sdk.ECRHubConfig;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.model.ECRHubDevice;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubRequestProto;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubResponseProto;
import com.wiseasy.ecr.hub.sdk.sp.websocket.WebSocketClientListener;
import com.wiseasy.ecr.hub.sdk.sp.websocket.WebSocketServerEngine;
import com.wiseasy.ecr.hub.sdk.utils.NetHelper;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/**
 * Socket pairing support <br>
 * 1. After calling the start() method, the ECRHub terminal can search for the ECR device  <br>
 * 2. ECRHub can initiate pairing requests, ECR devices need to implement {@link PairListener} <br>
 * 3. After confirming the pairing, obtain {@link ECRHubDevice#ws_address} through the device information  <br>
 * 4. Create a connection through {@link com.wiseasy.ecr.hub.sdk.ECRHubClientFactory#create(String, ECRHubConfig)} <br>
 *
 * <pre>
 *  ECRHubDevicePairSupport support = ECRHubDevicePairSupport.getInstance();
 *  try {
 *      support.start();
 *      support.setPairListener(device -> {
 *          // Pair request listening
 *          System.out.println("Pair device info:" + device);
 *          return true;
 *      });
 *  } finally {
 *      support.stop();
 *  }
 * </pre>
 *
 * @author wangyuxiang@wiseasy.com
 * @since 2023-10-16 11:07
 */
public class ECRHubDevicePairSupport implements WebSocketClientListener {

    public static final String ECR_CLIENT_MDNS_SERVICE_TYPE = "_ecr-client._tcp.local.";

    public interface PairListener {

        boolean confirm(ECRHubDevice device);

    }

    private static class ECRHubDevicePairSupportHolder {
        private static final ECRHubDevicePairSupport INSTANCE = new ECRHubDevicePairSupport();
    }

    public static ECRHubDevicePairSupport getInstance() {
        return ECRHubDevicePairSupportHolder.INSTANCE;
    }


    private static final Logger log = LoggerFactory.getLogger(ECRHubDevicePairSupport.class);

    private JmDNS jmDNS;

    private PairListener pairListener;

    private volatile boolean running;

    private final WebSocketServerEngine engine;


    private ECRHubDevicePairSupport() {
        this.engine = new WebSocketServerEngine();
    }

    public void setPairListener(PairListener listener) {
        this.pairListener = listener;
    }


    @Override
    public void onMessage(WebSocket conn, String message) {

        ECRHubRequestProto.RequestDeviceData deviceData;
        ECRHubRequestProto.ECRHubRequest ecrHubRequest;
        try {
            ecrHubRequest = ECRHubRequestProto.ECRHubRequest.parseFrom(message.getBytes(StandardCharsets.UTF_8));
            deviceData = ecrHubRequest.getDeviceData();
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }

        if (null != pairListener) {
            ECRHubDevice device = new ECRHubDevice();
            device.setMac_address(deviceData.getMacAddress());
            device.setIp_address(deviceData.getIpAddress());
            device.setTerminal_sn(deviceData.getDeviceName());
            device.setWs_address(StrUtil.format("ws://{}:{}", deviceData.getIpAddress(), deviceData.getPort()));
            if (pairListener.confirm(device)) {
                conn.send(new String(ECRHubResponseProto.ECRHubResponse.newBuilder()
                        .setTopic(ecrHubRequest.getTopic())
                        .setMsgId(RandomUtil.randomNumbers(20))
                        .setAppId(ecrHubRequest.getAppId())
                        .setTimestamp(String.valueOf(System.currentTimeMillis()))
                        .setSuccess(true)
                        .build().toByteArray()));
            } else {
                conn.send(new String(ECRHubResponseProto.ECRHubResponse.newBuilder()
                        .setTopic(ecrHubRequest.getTopic())
                        .setMsgId(RandomUtil.randomNumbers(20))
                        .setAppId(ecrHubRequest.getAppId())
                        .setTimestamp(String.valueOf(System.currentTimeMillis()))
                        .setSuccess(false)
                        .build().toByteArray()));
            }
        }
    }

    public void start() throws ECRHubException {
        try {
            if (running) {
                return;
            }
            engine.start();
            InetAddress siteLocalAddress = NetHelper.getLocalhost();
            jmDNS = JmDNS.create(siteLocalAddress);
            JSONObject info = new JSONObject();
            info.put("mac_address", NetUtil.getMacAddress(siteLocalAddress));
            info.put("os_name", SystemUtil.getOsInfo().getName());
            info.put("os_version", SystemUtil.getOsInfo().getVersion());
            String localHostName = NetHelper.getLocalHostName();
            info.put("host_name", localHostName);
            ServiceInfo serviceInfo = ServiceInfo.create(ECR_CLIENT_MDNS_SERVICE_TYPE, localHostName, engine.getPort(), info.toJSONString());
            jmDNS.registerService(serviceInfo);
            log.info("mdns register success, service name: {}", serviceInfo.getName());
            running = true;

        } catch (IOException e) {
            throw new ECRHubException(e);
        }
    }


    public void stop() throws ECRHubException {
        if (!running) {
            return;
        }
        try {
            engine.stop();
            if (null != jmDNS) {
                try {
                    jmDNS.close();
                } catch (IOException ignored) {

                }
            }
            running = false;
        } catch (InterruptedException e) {
            throw new ECRHubException(e);
        }
    }

}
