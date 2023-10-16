package com.wiseasy.ecr.hub.sdk;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.net.NetUtil;
import cn.hutool.system.SystemUtil;
import com.alibaba.fastjson2.JSONObject;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.model.ECRHubDevice;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author wangyuxiang@wiseasy.com
 * @since 2023-10-16 11:07
 */
public class ECRDeviceSocketRegisterServer implements ECRDeviceRegisterServer, WebSocketClientListener {

    private static final Logger log = LoggerFactory.getLogger(ECRDeviceSocketRegisterServer.class);


    private JmDNS jmDNS;

    private DeviceListener deviceListener;


    private final WebSocketServerEngine engine;


    private final Set<ECRHubDevice> hubDevices = new ConcurrentHashSet<>(1);


    public ECRDeviceSocketRegisterServer() {
        this.engine = new WebSocketServerEngine();
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (null != deviceListener) {
            if (deviceListener.pairConfirm(null)) {
                conn.send("success");
                // 响应成功 TODO
                ECRHubDevice hubDevice = new ECRHubDevice();
                hubDevice.setIp_address(conn.getRemoteSocketAddress().getAddress().getHostAddress());
                hubDevices.add(hubDevice);
            } else {
                conn.send("failed");
            }


        }
    }

    @Override
    public void start() throws ECRHubException {
        try {
            engine.start();
            InetAddress siteLocalAddress = NetHelper.getLocalhost();
            jmDNS = JmDNS.create(siteLocalAddress);
            JSONObject info = new JSONObject();
            info.put("mac_address", NetUtil.getMacAddress(siteLocalAddress));
            info.put("os_name", SystemUtil.getOsInfo().getName());
            info.put("os_version", SystemUtil.getOsInfo().getVersion());
            String localHostName = NetHelper.getLocalHostName();
            info.put("host_name", localHostName);
            ServiceInfo serviceInfo = ServiceInfo.create("_ecr-client._tcp.local.", localHostName, engine.getPort(), info.toJSONString());
            jmDNS.registerService(serviceInfo);
            log.info("mdns register success, service name: {}", serviceInfo.getName());

        } catch (IOException e) {
            throw new ECRHubException(e);
        }
    }


    @Override
    public void stop() throws ECRHubException {
        try {
            engine.stop();
            if (null != jmDNS) {
                try {
                    jmDNS.close();
                } catch (IOException ignored) {

                }
            }
        } catch (InterruptedException e) {
            throw new ECRHubException(e);
        }
    }

    @Override
    public List<ECRHubDevice> getAvailableDevice() {
        return new ArrayList<>(hubDevices);
    }

    @Override
    public void setDeviceListener(DeviceListener listener) {
        this.deviceListener = listener;
    }
}
