package com.wiseasy.ecr.hub.sdk.device;

import cn.hutool.core.util.RandomUtil;
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
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/**
 * @author wangyuxiang
 * @since 2023-10-18 16:50
 */
public class ECRHubDeviceManage implements WebSocketClientListener {

    private static final String ECR_HUB_CLIENT_MDNS_SERVICE_TYPE = "_ecr-hub-client._tcp.local.";
    private static final String ECR_HUB_SERVER_MDNS_SERVICE_TYPE = "_ecr-hub-server._tcp.local.";

    private static final Logger log = LoggerFactory.getLogger(ECRHubDeviceManage.class);

    private static class ECRHubDeviceManageHolder {
        private static final ECRHubDeviceManage INSTANCE = new ECRHubDeviceManage();
    }

    public static ECRHubDeviceManage getInstance() {
        return ECRHubDeviceManageHolder.INSTANCE;
    }


    private WebSocketServerEngine engine;

    private volatile boolean running;

    private JmDNS jmDNS;

    private DeviceServiceListener deviceServiceListener;
    private DeviceEventListener deviceEventListener;

    public void setDeviceEventListener(DeviceEventListener deviceEventListener) {
        this.deviceEventListener = deviceEventListener;
        deviceServiceListener.setDeviceListener(deviceEventListener);
    }

    private ECRHubDeviceManage() {

    }


    public void start() throws ECRHubException {

        if (running) {
            return;
        }

        try {

            InetAddress siteLocalAddress = NetHelper.getLocalhost();
            jmDNS = JmDNS.create(siteLocalAddress);
            deviceServiceListener = new DeviceServiceListener();
            this.engine = new WebSocketServerEngine();
            this.engine.setClientListener(this);
            engine.start();

            String localHostName = NetHelper.getLocalHostName();

            // JSONObject info = new JSONObject();
            // info.put("mac_address", NetUtil.getMacAddress(siteLocalAddress));
            // info.put("os_name", SystemUtil.getOsInfo().getName());
            // info.put("os_version", SystemUtil.getOsInfo().getVersion());
            // info.put("host_name", localHostName);
            ServiceInfo serviceInfo = ServiceInfo.create(ECR_HUB_CLIENT_MDNS_SERVICE_TYPE, localHostName, engine.getPort(), "");
            jmDNS.registerService(serviceInfo);
            log.info("mdns register success, service name: {}", serviceInfo.getName());
        } catch (IOException e) {
            throw new ECRHubException(e);
        }

        jmDNS.addServiceListener(ECR_HUB_SERVER_MDNS_SERVICE_TYPE, deviceServiceListener);
        running = true;
    }

    public void stop() {
        if (!running) {
            return;
        }
        try {
            engine.stop();
            jmDNS.unregisterAllServices();
            jmDNS.removeServiceListener(ECR_HUB_SERVER_MDNS_SERVICE_TYPE, deviceServiceListener);
            jmDNS.close();
            running = false;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean doPair(ECRHubDevice device) throws ECRHubException {
        if (null == device) {
            throw new ECRHubException("device must not null");
        }
        if (StrUtil.isBlank(device.getWs_address())) {
            throw new ECRHubException("ws_address must not blank");
        }
        ECRHubClient ecrHubClient = ECRHubClientFactory.create(device.getWs_address());
        ECRHubResponse ecrHubResponse = ecrHubClient.connect2();
        return ecrHubResponse.isSuccess();
    }

    private void handlePair(WebSocket conn, String message) {
        ECRHubRequestProto.RequestDeviceData deviceData;
        ECRHubRequestProto.ECRHubRequest ecrHubRequest;
        try {
            ecrHubRequest = ECRHubRequestProto.ECRHubRequest.parseFrom(message.getBytes(StandardCharsets.UTF_8));
            deviceData = ecrHubRequest.getDeviceData();
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }

        if (null != deviceEventListener) {
            ECRHubDevice device = new ECRHubDevice();
            device.setMac_address(deviceData.getMacAddress());
            device.setIp_address(deviceData.getIpAddress());
            device.setTerminal_sn(deviceData.getDeviceName());
            device.setWs_address(StrUtil.format("ws://{}:{}", deviceData.getIpAddress(), deviceData.getPort()));
            if (deviceEventListener.onPaired((device))) {
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


    @Override
    public void onMessage(WebSocket conn, String message) {
        handlePair(conn, message);
    }

    public interface DeviceEventListener {

        /**
         * Discover ECR Hub devices in your LAN / WLAN
         */
        void onAdded(ECRHubDevice device);

        /**
         * ECR Hub send a pairing request
         */
        boolean onPaired(ECRHubDevice device);

        /**
         * ECR Hub devices is removed
         */
        void onRemoved(ECRHubDevice device);

    }

    private static class DeviceServiceListener implements ServiceListener {

        public void setDeviceListener(DeviceEventListener deviceEventListener) {
            this.deviceEventListener = deviceEventListener;
        }

        private DeviceEventListener deviceEventListener;


        @Override
        public void serviceAdded(ServiceEvent event) {

        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            if (null != deviceEventListener) {
                ECRHubDevice ecrHubDevice = new ECRHubDevice();
                ServiceInfo info = event.getInfo();
                ecrHubDevice.setTerminal_sn(info.getName());
                ecrHubDevice.setWs_address(StrUtil.format("ws://{}:{}", info.getHostAddresses()[0], info.getPort()));
                deviceEventListener.onRemoved(ecrHubDevice);
            }
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            if (null != deviceEventListener) {
                ECRHubDevice device = new ECRHubDevice();
                ServiceInfo info = event.getInfo();
                device.setTerminal_sn(info.getName());
                device.setWs_address(StrUtil.format("ws://{}:{}", info.getHostAddresses()[0], info.getPort()));
                deviceEventListener.onAdded(device);
            }
        }

    }

}
