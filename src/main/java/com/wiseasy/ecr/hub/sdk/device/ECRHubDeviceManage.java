package com.wiseasy.ecr.hub.sdk.device;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.google.protobuf.InvalidProtocolBufferException;
import com.wiseasy.ecr.hub.sdk.ECRHubClient;
import com.wiseasy.ecr.hub.sdk.ECRHubClientFactory;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.model.response.ECRHubResponse;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubRequestProto;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubResponseProto;
import com.wiseasy.ecr.hub.sdk.sp.websocket.WebSocketClientListener;
import com.wiseasy.ecr.hub.sdk.sp.websocket.WebSocketServerEngine;
import com.wiseasy.ecr.hub.sdk.utils.NetHelper;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author wangyuxiang
 * @since 2023-10-18 16:50
 */
public class ECRHubDeviceManage implements WebSocketClientListener {

    private static final Logger log = LoggerFactory.getLogger(ECRHubDeviceManage.class);

    private static final String ECR_HUB_CLIENT_MDNS_SERVICE_TYPE = "_ecr-hub-client._tcp.local.";
    private static final String ECR_HUB_SERVER_MDNS_SERVICE_TYPE = "_ecr-hub-server._tcp.local.";

    private volatile boolean running;
    private WebSocketServerEngine engine;
    private JmDNS jmDNS;
    private DeviceServiceListener deviceServiceListener;
    private DeviceEventListener deviceEventListener;

    private ECRHubDeviceManage() {
        this.deviceServiceListener = new DeviceServiceListener();
        this.engine = new WebSocketServerEngine();
    }

    private static class ECRHubDeviceManageHolder {
        private static final ECRHubDeviceManage INSTANCE = new ECRHubDeviceManage();
    }

    public static ECRHubDeviceManage getInstance() {
        return ECRHubDeviceManageHolder.INSTANCE;
    }

    public void start() throws ECRHubException {
        if (running) {
            return;
        }
        try {
            ServiceInfo serviceInfo = ServiceInfo.create(ECR_HUB_CLIENT_MDNS_SERVICE_TYPE,
                                                        NetHelper.getLocalHostName(),
                                                        engine.getPort(),
                                                        "");
            jmDNS = JmDNS.create(NetHelper.getLocalhost());
            jmDNS.registerService(serviceInfo);
            jmDNS.addServiceListener(ECR_HUB_SERVER_MDNS_SERVICE_TYPE, deviceServiceListener);
            log.info("mdns register success, service name: {}", serviceInfo.getName());

            engine.setClientListener(this);
            engine.start();

            running = true;
        } catch (Exception e) {
            throw new ECRHubException(e);
        }
    }

    public void stop() {
        if (!running) {
            return;
        }
        try {
            jmDNS.unregisterAllServices();
            jmDNS.removeServiceListener(ECR_HUB_SERVER_MDNS_SERVICE_TYPE, deviceServiceListener);
            jmDNS.close();

            engine.stop();

            running = false;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void setDeviceEventListener(DeviceEventListener deviceEventListener) {
        this.deviceEventListener = deviceEventListener;
        this.deviceServiceListener.setDeviceListener(deviceEventListener);
    }

    public boolean doPair(ECRHubDevice device) throws ECRHubException {
        if (null == device) {
            throw new ECRHubException("device must not null");
        }
        if (StrUtil.isBlank(device.getWs_address())) {
            throw new ECRHubException("ws_address must not blank");
        }
        ECRHubClient client = ECRHubClientFactory.create(device.getWs_address());
        ECRHubResponse response = client.connect2();
        return response.isSuccess();
    }

    @Override
    public void onMessage(WebSocket socket, String message) {
        onPair(socket, message);
    }

    private void onPair(WebSocket socket, String message) {
        ECRHubRequestProto.ECRHubRequest ecrHubRequest;
        try {
            ecrHubRequest = ECRHubRequestProto.ECRHubRequest.parseFrom(message.getBytes(StandardCharsets.UTF_8));
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }

        if (null != deviceEventListener) {
            ECRHubRequestProto.RequestDeviceData deviceData = ecrHubRequest.getDeviceData();
            ECRHubDevice device = new ECRHubDevice();
            device.setMac_address(deviceData.getMacAddress());
            device.setIp_address(deviceData.getIpAddress());
            device.setTerminal_sn(deviceData.getDeviceName());
            device.setWs_address(buildWSAddress(deviceData.getIpAddress(), Integer.parseInt(deviceData.getPort())));

            boolean success = deviceEventListener.onPaired((device));

            socket.send(new String(ECRHubResponseProto.ECRHubResponse.newBuilder()
                    .setTimestamp(String.valueOf(System.currentTimeMillis()))
                    .setMsgId(IdUtil.fastSimpleUUID())
                    .setTopic(ecrHubRequest.getTopic())
                    .setSuccess(success)
                    .build().toByteArray()));
        }
    }

    private String buildWSAddress(String host, int port) {
        return StrUtil.format("ws://{}:{}", host, port);
    }

    public interface DeviceEventListener {
        /**
         * Discover ECR Hub devices in your LAN / WLAN
         */
        void onAdded(ECRHubDevice device);

        /**
         * ECR Hub devices is removed
         */
        void onRemoved(ECRHubDevice device);

        /**
         * ECR Hub send a pairing request
         */
        boolean onPaired(ECRHubDevice device);
    }

    private class DeviceServiceListener implements ServiceListener {

        private DeviceEventListener deviceEventListener;

        public void setDeviceListener(DeviceEventListener deviceEventListener) {
            this.deviceEventListener = deviceEventListener;
        }

        @Override
        public void serviceAdded(ServiceEvent event) {
        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            if (null != deviceEventListener) {
                deviceEventListener.onRemoved(buildECRHubDevice(event));
            }
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            if (null != deviceEventListener) {
                deviceEventListener.onAdded(buildECRHubDevice(event));
            }
        }

        private ECRHubDevice buildECRHubDevice(ServiceEvent event) {
            ServiceInfo info = event.getInfo();
            ECRHubDevice device = new ECRHubDevice();
            device.setTerminal_sn(info.getName());
            device.setWs_address(buildWSAddress(info.getHostAddresses()[0], info.getPort()));
            return device;
        }
    }
}