package com.wiseasy.ecr.hub.sdk.support;

import com.wiseasy.ecr.hub.sdk.model.ECRHubDevice;
import com.wiseasy.ecr.hub.sdk.utils.NetHelper;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.InetAddress;

/**
 * @author wangyuxiang
 * @since 2023-10-17 12:53
 */
public class ECRHubDeviceSearchSupport {

    private static final String ECR_HUB_SERVER_MDNS_TYPE = "_ecr-hub-server._tcp.local.";

    private static class ECRHubDeviceSearchSupportHolder {
        private static final ECRHubDeviceSearchSupport INSTANCE = new ECRHubDeviceSearchSupport();
    }

    public static ECRHubDeviceSearchSupport getInstance() {
        return ECRHubDeviceSearchSupportHolder.INSTANCE;
    }


    private final JmDNS jmDNS;

    public interface DeviceListener {

        void added(ECRHubDevice device);

        void removed(ECRHubDevice device);

    }

    private volatile boolean running;

    private final DeviceServiceListener deviceServiceListener;


    public void setDeviceListener(DeviceListener deviceListener) {
        deviceServiceListener.setDeviceListener(deviceListener);
    }

    private static class DeviceServiceListener implements ServiceListener {

        public void setDeviceListener(DeviceListener deviceListener) {
            this.deviceListener = deviceListener;
        }

        private DeviceListener deviceListener;


        @Override
        public void serviceAdded(ServiceEvent event) {

        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            if (null != deviceListener) {
                deviceListener.removed(new ECRHubDevice());
            }
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            if (null != deviceListener) {
                deviceListener.added(new ECRHubDevice());
            }
        }


    }

    public void start() {
        if (running) {
            return;
        }
        jmDNS.addServiceListener(ECR_HUB_SERVER_MDNS_TYPE, deviceServiceListener);
        running = true;
    }

    public void stop() {
        if (!running) {
            return;
        }
        jmDNS.removeServiceListener(ECR_HUB_SERVER_MDNS_TYPE, deviceServiceListener);
        try {
            jmDNS.close();
            running = true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private ECRHubDeviceSearchSupport() {
        InetAddress siteLocalAddress = NetHelper.getLocalhost();
        try {
            jmDNS = JmDNS.create(siteLocalAddress);
            deviceServiceListener = new DeviceServiceListener();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
