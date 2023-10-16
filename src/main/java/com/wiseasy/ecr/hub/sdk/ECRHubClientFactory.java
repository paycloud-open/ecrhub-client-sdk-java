package com.wiseasy.ecr.hub.sdk;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.model.ECRHubDevice;

import java.util.ArrayList;
import java.util.List;

public class ECRHubClientFactory {

    public static final String SERIAL_PORT_PROTOCOL_PREFIX = "sp://";
    public static final String WEB_SOCKET_PROTOCOL_PREFIX = "ws://";
    public static final String WEB_SOCKET_SSL_PROTOCOL_PREFIX = "wss://";

    public static ECRHubClient create(String url) throws ECRHubException {
        return create(url, new ECRHubConfig());
    }

    public static ECRHubClient createSocketHubClient(ECRHubConfig config) throws ECRHubException {
        ECRDeviceRegisterServer server = new ECRDeviceSocketRegisterServer();
        try {
            server.start();
            List<ECRHubDevice> list = new ArrayList<>();
            server.setDeviceListener(device -> {
                list.add(device);
                return true;
            });
            long before = System.currentTimeMillis();
            int timeout = config.getSocketConfig().getConnTimeout();
            while (list.isEmpty()) {
                if (System.currentTimeMillis() - before > timeout) {
                    throw new ECRHubException("no ECRHub device.");
                } else {
                    ThreadUtil.safeSleep(1000);
                }
            }
            return create(list.get(0).getWs_address(), config);
        } finally {
            server.stop();
        }
    }

    public static ECRHubClient createSocketHubClient() throws ECRHubException {
        return createSocketHubClient(new ECRHubConfig());
    }

    public static ECRHubClient create(String url, ECRHubConfig config) throws ECRHubException {
        if (StrUtil.isBlank(url)) {
            throw new ECRHubException("url cannot be empty.");
        }
        if (config == null) {
            throw new ECRHubException("ECRHubConfig cannot be empty.");
        }

        if (url.startsWith(SERIAL_PORT_PROTOCOL_PREFIX)) {
            return new ECRHubSerialPortClient(url.substring(SERIAL_PORT_PROTOCOL_PREFIX.length()), config);
        } else if (url.startsWith(WEB_SOCKET_PROTOCOL_PREFIX) || url.startsWith(WEB_SOCKET_SSL_PROTOCOL_PREFIX)) {
            return new ECRHubWebSocketClient(url, config);
        } else {
            throw new ECRHubException("Invalid url:" + url);
        }
    }
}