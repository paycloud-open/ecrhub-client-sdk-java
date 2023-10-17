package com.wiseasy.ecr.hub.sdk.test;

import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.support.ECRHubDevicePairSupport;

/**
 * @author wangyuxiang@wiseasy.com
 * @since 2023-10-17 12:46
 */
public class ECRHubDevicePairSupportTest {

    public static void main(String[] args) throws ECRHubException {
        ECRHubDevicePairSupport support = ECRHubDevicePairSupport.getInstance();

        support.start();
        support.setPairListener(device -> {
            // Pair request listening
            System.out.println("Pair device info:" + device);
            return true;
        });
        support.stop();

    }
}
