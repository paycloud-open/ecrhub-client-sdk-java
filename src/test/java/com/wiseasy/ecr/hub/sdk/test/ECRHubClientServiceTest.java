package com.wiseasy.ecr.hub.sdk.test;

import com.wiseasy.ecr.hub.sdk.device.ECRHubClientService;
import com.wiseasy.ecr.hub.sdk.device.ECRHubClientWebSocketService;
import com.wiseasy.ecr.hub.sdk.device.ECRHubDevice;
import com.wiseasy.ecr.hub.sdk.device.ECRHubDeviceEventListener;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;

import java.util.List;
import java.util.Scanner;

/**
 * @author wangyuxiang
 * @since 2023-10-20 11:48
 */
public class ECRHubClientServiceTest {

    public static void main(String[] args) throws ECRHubException {
        ECRHubClientService service = getEcrHubClientService();

        Scanner scanner = new Scanner(System.in);

        while (scanner.hasNext()) {
            String s = scanner.nextLine();
            switch (s) {
                case "1":
                    List<ECRHubDevice> pairedDeviceList = service.getPairedDeviceList();
                    for (ECRHubDevice ecrHubDevice : pairedDeviceList) {
                        System.out.println(ecrHubDevice);
                    }
                    break;
                case "2":
                    List<ECRHubDevice> unpairedDeviceList = service.getUnpairedDeviceList();
                    for (ECRHubDevice ecrHubDevice : unpairedDeviceList) {
                        System.out.println(ecrHubDevice);
                    }
                    break;
                case "3":
                    System.out.println(service.isRunning());
                    break;
                case "4":
                    service.stop();
                    break;
                case "5":
                    service.start();
                    break;
                case "6":
                    try {
                        for (ECRHubDevice ecrHubDevice : service.getUnpairedDeviceList()) {
                            service.pair(ecrHubDevice);
                        }
                    } catch (ECRHubException e) {
                        e.printStackTrace();
                    }
                    break;
                case "7":
                    for (ECRHubDevice ecrHubDevice : service.getPairedDeviceList()) {
                        service.unpair(ecrHubDevice);
                    }
                    break;
            }
        }


    }

    private static ECRHubClientService getEcrHubClientService() throws ECRHubException {
        ECRHubClientService service = ECRHubClientWebSocketService.getInstance();
        service.setDeviceEventListener(new ECRHubDeviceEventListener() {

            /**
             * Discover the devices in the LAN
             */
            @Override
            public void onAdded(ECRHubDevice device) {
                System.out.println("onAdded :" + device);
            }

            /**
             * Devices removed from the LAN
             */

            @Override
            public void onRemoved(ECRHubDevice device) {
                System.out.println("onRemoved :" + device);
            }

            /**
             * The device start pairing
             */
            @Override
            public boolean onPaired(ECRHubDevice device) {
                System.out.println("onPaired :" + device);
                return true;
            }

            /**
             * The device is unpaired
             */
            @Override
            public void onUnpaired(ECRHubDevice device) {
                System.out.println("unPaired :" + device);
            }

        });
        service.start();
        return service;
    }
}
