package com.wiseasy.ecr.hub.sdk.test;

import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.device.ECRHubDevice;
import com.wiseasy.ecr.hub.sdk.device.ECRHubDeviceManage;

/**
 * @author wangyuxiang
 * @since 2023-10-18 16:50
 */
public class ECRHubDeviceManageTest {

    public static void main(String[] args) throws ECRHubException {
        ECRHubDeviceManage manage = ECRHubDeviceManage.getInstance();

        manage.start();

        // Set a device listener
        manage.setDeviceEventListener(new ECRHubDeviceManage.DeviceEventListener() {
            @Override
            public void onAdded(ECRHubDevice device) {

            }

            @Override
            public boolean onPaired(ECRHubDevice device) {
                return false;
            }

            @Override
            public void onRemoved(ECRHubDevice device) {

            }

        });

        // ECR Device do a pairing
        manage.doPair(null);

        manage.stop();
    }

}
