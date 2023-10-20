package com.wiseasy.ecr.hub.sdk.test;

import com.wiseasy.ecr.hub.sdk.device.ECRHubDevice;
import com.wiseasy.ecr.hub.sdk.device.ECRHubDeviceEventListener;
import com.wiseasy.ecr.hub.sdk.device.ECRHubClientService;
import com.wiseasy.ecr.hub.sdk.device.ECRHubClientWebSocketService;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;

/**
 * @author wangyuxiang
 * @since 2023-10-20 11:48
 */
public class ECRHubClientServiceTest {

    public static void main(String[] args) throws ECRHubException {
        ECRHubClientService service = ECRHubClientWebSocketService.getInstance();

        service.start();

        service.setDeviceEventListener(new ECRHubDeviceEventListener() {

            /**
             * Discover the devices in the LAN
             */
            @Override
            public void onAdded(ECRHubDevice device) {

            }

            /**
             * Devices removed from the LAN
             */

            @Override
            public void onRemoved(ECRHubDevice device) {

            }

            /**
             * The device start pairing
             */
            @Override
            public boolean onPaired(ECRHubDevice device) {
                return false;
            }

            /**
             * The device is unpaired
             */
            @Override
            public void unPaired(ECRHubDevice device) {

            }

        });


    }
}
