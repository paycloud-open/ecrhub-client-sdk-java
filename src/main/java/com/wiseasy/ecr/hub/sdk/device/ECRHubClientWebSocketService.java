package com.wiseasy.ecr.hub.sdk.device;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import com.alibaba.fastjson2.JSONObject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.wiseasy.ecr.hub.sdk.ECRHubClient;
import com.wiseasy.ecr.hub.sdk.ECRHubClientFactory;
import com.wiseasy.ecr.hub.sdk.enums.ETopic;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.model.response.ECRHubResponse;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubRequestProto;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubResponseProto;
import com.wiseasy.ecr.hub.sdk.sp.websocket.WebSocketClientEngine;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author wangyuxiang
 * @since 2023-10-18 16:50
 */
public class ECRHubClientWebSocketService implements WebSocketClientListener, ECRHubClientService {

    private static final Logger log = LoggerFactory.getLogger(ECRHubClientWebSocketService.class);

    private static final String ECR_HUB_CLIENT_MDNS_SERVICE_TYPE = "_ecr-hub-client._tcp.local.";
    private static final String ECR_HUB_SERVER_MDNS_SERVICE_TYPE = "_ecr-hub-server._tcp.local.";

    private final ECRHubDeviceStorage storage;
    private final Map<String, ECRHubDevice> deviceMap;

    private volatile boolean running;
    private ECRHubDeviceEventListener ecrHubDeviceEventListener;
    private WebSocketServerEngine engine;
    private JmDNS jmDNS;
    private DeviceServiceListener deviceServiceListener;


    private ECRHubClientWebSocketService() {
        storage = ECRHubDeviceStorage.getInstance();
        deviceMap = new ConcurrentHashMap<>(1);
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void setDeviceEventListener(ECRHubDeviceEventListener listener) {
        this.ecrHubDeviceEventListener = listener;
    }


    private static class ECRHubDeviceManageHolder {
        private static final ECRHubClientWebSocketService INSTANCE = new ECRHubClientWebSocketService();
    }

    public static ECRHubClientWebSocketService getInstance() {
        return ECRHubDeviceManageHolder.INSTANCE;
    }

    @Override
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

            JSONObject info = new JSONObject();
            info.put("mac_address", NetUtil.getMacAddress(siteLocalAddress));
            info.put("os_name", SystemUtil.getOsInfo().getName());
            info.put("os_version", SystemUtil.getOsInfo().getVersion());
            info.put("host_name", localHostName);
            ServiceInfo serviceInfo = ServiceInfo.create(ECR_HUB_CLIENT_MDNS_SERVICE_TYPE, localHostName, engine.getPort(), info.toJSONString());
            jmDNS.registerService(serviceInfo);
            log.info("mdns register success, service name: {}", serviceInfo.getName());
        } catch (IOException e) {
            throw new ECRHubException(e);
        }
        jmDNS.addServiceListener(ECR_HUB_SERVER_MDNS_SERVICE_TYPE, deviceServiceListener);
        running = true;
    }

    @Override
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

    @Override
    public boolean pair(ECRHubDevice device) throws ECRHubException {
        if (null == device) {
            throw new ECRHubException("device must not null");
        }
        if (StrUtil.isBlank(device.getWs_address())) {
            throw new ECRHubException("ws_address must not blank");
        }
        ECRHubClient ecrHubClient = ECRHubClientFactory.create(device.getWs_address());
        try {
            ECRHubResponse ecrHubResponse = ecrHubClient.connect2();
            boolean success = ecrHubResponse.isSuccess();
            if (success) {
                storage.addPairedDevice(device.getTerminal_sn());
                if (null != ecrHubDeviceEventListener) {
                    ecrHubDeviceEventListener.onPaired(device);
                }
            }
            return success;
        } finally {
            ecrHubClient.disconnect();
        }
    }

    @Override
    public void unpair(ECRHubDevice device) throws ECRHubException {
        if (null == device) {
            throw new ECRHubException("device must not null");
        }
        if (StrUtil.isBlank(device.getTerminal_sn())) {
            throw new ECRHubException("terminal_sn must not blank");
        }

        // The device is not online
        if (StrUtil.isBlank(device.getWs_address())) {
            doUnpaired(device);
            return;
        }

        String wsAddress = device.getWs_address();
        try {
            WebSocketClientEngine engine = new WebSocketClientEngine(new URI(wsAddress));
            engine.connectBlocking(30, TimeUnit.SECONDS);
            String msgId = IdUtil.fastSimpleUUID();
            engine.send(new String(ECRHubRequestProto.ECRHubRequest.newBuilder()
                    .setTopic(ETopic.UN_PAIR.getValue())
                    .setRequestId(msgId)
                    .setDeviceData(ECRHubRequestProto.RequestDeviceData.newBuilder()
                            .setMacAddress(NetHelper.getLocalMacAddress())
                            .build())
                    .build()
                    .toByteArray()
            ));

            // The default wait is 60s
            String receive = engine.receive(msgId, System.currentTimeMillis(), 60 * 1000);
            ECRHubResponseProto.ECRHubResponse ecrHubResponse = ECRHubResponseProto.ECRHubResponse.parseFrom(receive.getBytes(StandardCharsets.UTF_8));
            if (ecrHubResponse.getSuccess()) {
                // unpaired succeeded
                doUnpaired(device);
            }
        } catch (ECRHubException | InvalidProtocolBufferException | URISyntaxException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void doUnpaired(ECRHubDevice device) {
        storage.removePairedDevice(device.getTerminal_sn());
        if (null != ecrHubDeviceEventListener) {
            ecrHubDeviceEventListener.onUnpaired(device);
        }
    }

    @Override
    public List<ECRHubDevice> getPairedDeviceList() {
        List<String> devices = storage.queryPairedDevice();
        List<ECRHubDevice> ecrHubDevices = new ArrayList<>();
        for (String device_sn : devices) {
            ECRHubDevice e = deviceMap.get(device_sn);
            // The device is not online
            if (null == e) {
                e = new ECRHubDevice();
                e.setTerminal_sn(device_sn);
            }
            ecrHubDevices.add(e);
        }
        return ecrHubDevices;
    }

    @Override
    public List<ECRHubDevice> getUnpairedDeviceList() {
        List<String> strings = storage.queryPairedDevice();

        List<ECRHubDevice> ecrHubDevices = new ArrayList<>();
        List<String> all_devices = new ArrayList<>(deviceMap.keySet());
        strings.forEach(all_devices::remove);
        for (String terminal_sn : all_devices) {
            ecrHubDevices.add(deviceMap.get(terminal_sn));
        }
        return ecrHubDevices;
    }

    @Override
    public void onMessage(WebSocket conn, String message) {

        ECRHubRequestProto.ECRHubRequest ecrHubRequest;
        try {
            ecrHubRequest = ECRHubRequestProto.ECRHubRequest.parseFrom(message.getBytes(StandardCharsets.UTF_8));
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }

        ECRHubRequestProto.RequestDeviceData deviceData = ecrHubRequest.getDeviceData();
        ECRHubDevice device = new ECRHubDevice();
        device.setMac_address(deviceData.getMacAddress());
        device.setIp_address(deviceData.getIpAddress());
        device.setTerminal_sn(deviceData.getDeviceName());
        device.setWs_address(buildWSAddress(deviceData.getIpAddress(), Integer.parseInt(deviceData.getPort())));

        String topic = ecrHubRequest.getTopic();
        if (ETopic.PAIR.getValue().equals(topic)) {
            doPair(conn, device);
        } else if (ETopic.UN_PAIR.getValue().equals(topic)) {
            cancelPair(conn, device);
        }
    }

    private void doPair(WebSocket socket, ECRHubDevice device) {

        if (null != ecrHubDeviceEventListener) {
            boolean success = ecrHubDeviceEventListener.onPaired((device));
            if (success) {
                storage.addPairedDevice(device.getTerminal_sn());
            }

            socket.send(new String(ECRHubResponseProto.ECRHubResponse.newBuilder()
                    .setRequestId(IdUtil.fastSimpleUUID())
                    .setTopic(ETopic.PAIR.getValue())
                    .setSuccess(success)
                    .build().toByteArray()));
        } else {
            // The default pairing is not set successfully
            storage.addPairedDevice(device.getTerminal_sn());
            socket.send(new String(ECRHubResponseProto.ECRHubResponse.newBuilder()
                    .setRequestId(IdUtil.fastSimpleUUID())
                    .setTopic(ETopic.PAIR.getValue())
                    .setSuccess(true)
                    .build().toByteArray()));
        }
    }

    private void cancelPair(WebSocket socket, ECRHubDevice device) {
        socket.send(new String(ECRHubResponseProto.ECRHubResponse.newBuilder()
                .setRequestId(IdUtil.fastSimpleUUID())
                .setTopic(ETopic.UN_PAIR.getValue())
                .setSuccess(true)
                .build().toByteArray()));

        storage.removePairedDevice(device.getTerminal_sn());
        if (null != ecrHubDeviceEventListener) {
            ecrHubDeviceEventListener.onUnpaired(device);
        }
    }

    private String buildWSAddress(String host, int port) {
        return StrUtil.format("ws://{}:{}", host, port);
    }

    private class DeviceServiceListener implements ServiceListener {

        @Override
        public void serviceAdded(ServiceEvent event) {

        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            ECRHubDevice device = buildFromServiceInfo(event.getInfo());
            log.info("removed device:{}", device);
            deviceMap.remove(device.getTerminal_sn());
            if (null != ecrHubDeviceEventListener) {
                ecrHubDeviceEventListener.onRemoved(device);
            }
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            ECRHubDevice device = buildFromServiceInfo(event.getInfo());

            ECRHubDevice ecrHubDevice = deviceMap.get(device.getTerminal_sn());
            if (null != ecrHubDevice) {
                String wsAddress = ecrHubDevice.getWs_address();
                if (StrUtil.equalsAnyIgnoreCase(wsAddress, device.getWs_address())) {
                    log.warn("Discover the same service, ignore:{}", device);
                    return;
                }
            }

            log.info("find device:{}", device);
            deviceMap.put(device.getTerminal_sn(), device);
            if (null != ecrHubDeviceEventListener) {
                ecrHubDeviceEventListener.onAdded(device);
            }
        }
    }

    private ECRHubDevice buildFromServiceInfo(ServiceInfo info) {
        ECRHubDevice device = new ECRHubDevice();
        device.setTerminal_sn(info.getName());
        device.setApp_version(info.getPropertyString("app_version"));
        device.setApp_name(info.getPropertyString("app_name"));
        device.setWs_address(buildWSAddress(info.getHostAddresses()[0], info.getPort()));
        return device;
    }
}