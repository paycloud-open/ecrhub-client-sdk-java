package com.wiseasy.ecr.hub.sdk.test;

import com.wiseasy.ecr.hub.sdk.ECRDeviceSocketRegisterServer;
import com.wiseasy.ecr.hub.sdk.ECRHubClient;
import com.wiseasy.ecr.hub.sdk.ECRHubClientFactory;
import com.wiseasy.ecr.hub.sdk.ECRHubConfig;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.model.request.PurchaseRequest;
import com.wiseasy.ecr.hub.sdk.model.response.PurchaseResponse;

/**
 * @author wangyuxiang@wiseasy.com
 * @since 2023-10-16 11:43
 */
public class ECRDeviceSocketRegisterServerTest {

    public static void main(String[] args) throws ECRHubException {
        ECRDeviceSocketRegisterServer deviceSocketRegisterServer = new ECRDeviceSocketRegisterServer();
        deviceSocketRegisterServer.start();
        deviceSocketRegisterServer.setDeviceListener(device -> {
            String wsAddress = device.getWs_address();


            ECRHubConfig requestConfig = new ECRHubConfig();
            try {
                ECRHubClient client = ECRHubClientFactory.create(wsAddress, requestConfig);

                // Purchase
                PurchaseRequest request = new PurchaseRequest();
                request.setMerchant_order_no("O" + System.currentTimeMillis());
                request.setOrder_amount("10");
                request.setPay_method_category("BANKCARD");
                request.setConfig(requestConfig);

                // Execute purchase request
                PurchaseResponse response = client.execute(request);
                System.out.println("Purchase Response:" + response);

            } catch (ECRHubException e) {
                throw new RuntimeException(e);
            }

            return true;
        });


    }
}
