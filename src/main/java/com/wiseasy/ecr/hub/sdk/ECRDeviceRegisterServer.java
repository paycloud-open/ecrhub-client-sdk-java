package com.wiseasy.ecr.hub.sdk;

import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.model.ECRHubDevice;

import java.util.List;

/**
 * @author wangyuxiang@wiseasy.com
 * @since 2023-10-16 09:47
 */
public interface ECRDeviceRegisterServer {


    interface DeviceListener {

        boolean pairConfirm(ECRHubDevice device);

    }

    void start() throws ECRHubException;

    void stop() throws ECRHubException;

    List<ECRHubDevice> getAvailableDevice() throws ECRHubException;

    void setDeviceListener(DeviceListener listener) throws ECRHubException;

}
