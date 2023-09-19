package com.wiseasy.ecr.hub.sdk;

import cn.hutool.core.util.StrUtil;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;

public class ECRHubClientFactory {

    public static final String SERIAL_PORT_PROTOCOL_PREFIX = "sp://";
    public static final String WEB_SOCKET_PROTOCOL_PREFIX = "ws://";
    public static final String WEB_SOCKET_SSL_PROTOCOL_PREFIX = "wss://";

    public static ECRHubClient create(String url, ECRHubConfig config) throws ECRHubException {
        if (StrUtil.isBlank(url)) {
            throw new ECRHubException("url cannot be empty.");
        }
        if (config == null) {
            throw new ECRHubException("ECRHubConfig cannot be empty.");
        }
        if (StrUtil.isBlank(config.getAppId())) {
            throw new ECRHubException("AppId cannot be empty.");
        }

        if (url.startsWith(SERIAL_PORT_PROTOCOL_PREFIX)) {
            return new ECRHubSerialPortClient(url.substring(SERIAL_PORT_PROTOCOL_PREFIX.length()), config);
        }
        else if (url.startsWith(WEB_SOCKET_PROTOCOL_PREFIX) || url.startsWith(WEB_SOCKET_SSL_PROTOCOL_PREFIX)) {
            return new ECRHubWebSocketClient(url, config);
        }
        else {
            throw new ECRHubException("Invalid url:" + url);
        }
    }
}