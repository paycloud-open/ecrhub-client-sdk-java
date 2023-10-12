package com.wiseasy.ecr.hub.sdk.utils;

import cn.hutool.core.net.NetUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NetHelper {

    public static String getLocalHostName() {
        String hostName = null;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            // ignore
        }
        return hostName;
    }

    public static String getLocalMacAddress() {
        return NetUtil.getLocalMacAddress();
    }
}