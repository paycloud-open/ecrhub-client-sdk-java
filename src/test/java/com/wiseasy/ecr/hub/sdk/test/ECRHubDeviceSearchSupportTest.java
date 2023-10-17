package com.wiseasy.ecr.hub.sdk.test;

import com.wiseasy.ecr.hub.sdk.model.ECRHubDevice;
import com.wiseasy.ecr.hub.sdk.support.ECRHubDeviceSearchSupport;

/**
 * @author wangyuxiang@wiseasy.com
 * @since 2023-10-17 13:09
 */
public class ECRHubDeviceSearchSupportTest {

    public static void main(String[] args) {
        ECRHubDeviceSearchSupport support = ECRHubDeviceSearchSupport.getInstance();
        support.start();

        support.setDeviceListener(new ECRHubDeviceSearchSupport.DeviceListener() {
            @Override
            public void added(ECRHubDevice device) {
                System.out.println("added: " + device);
            }

            @Override
            public void remove(ECRHubDevice device) {
                System.out.println("remove: " + device);
            }
        });
        support.stop();
    }
}
