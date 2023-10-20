package com.wiseasy.ecr.hub.sdk.device;

import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;

import java.util.List;

/**
 * @author wangyuxiang
 * @since 2023-10-20 11:34
 */
public interface ECRHubClientService {

    void start() throws ECRHubException;

    void stop();

    boolean isRunning();

    void setDeviceEventListener(ECRHubDeviceEventListener listener);

    boolean pair(ECRHubDevice device) throws ECRHubException;

    void unpair(ECRHubDevice device) throws ECRHubException;

    List<ECRHubDevice> getPairedDeviceList();

    List<ECRHubDevice> getUnpairedDeviceList();

}
