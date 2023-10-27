package com.wiseasy.ecr.hub.sdk.utils;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashSet;

public class NetHelper {

    private static String localMacAddress;

    public static int getUsableLocalPort(int port) {
        while (!NetUtil.isUsableLocalPort(port)) {
            if (port >= NetUtil.PORT_RANGE_MAX) {
                throw new IllegalArgumentException("The maximum port of 65535 cannot be exceeded");
            }
            port = port + 1;
        }
        return port;
    }

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
        if (StrUtil.isBlank(localMacAddress)) {
            InetAddress address = getLocalhost();
            if (address != null) {
                localMacAddress = NetUtil.getMacAddress(address);
            } else {
                localMacAddress = NetUtil.getLocalMacAddress();
            }
            if (StrUtil.isBlank(localMacAddress)) {
                localMacAddress = NetUtil.getLocalHostName();
            }
        }
        return localMacAddress;
    }

    public static InetAddress getLocalhost() {
        LinkedHashSet<InetAddress> addressList = NetUtil.localAddressList(address -> !address.isLoopbackAddress() && address instanceof Inet4Address);
        for (InetAddress address : addressList) {
            if (address.isSiteLocalAddress()) {
                // Local address: 10.0.0.0 ~ 10.255.255.255、172.16.0.0 ~ 172.31.255.255、192.168.0.0 ~ 192.168.255.255
                return address;
            }
        }
        return NetUtil.getLocalhost();
    }
}