package com.wiseasy.ecr.hub.sdk.test;

import com.wiseasy.ecr.hub.sdk.ECRHubClient;
import com.wiseasy.ecr.hub.sdk.ECRHubClientFactory;
import com.wiseasy.ecr.hub.sdk.ECRHubConfig;
import com.wiseasy.ecr.hub.sdk.ECRHubResponseCallBack;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubTimeoutException;
import com.wiseasy.ecr.hub.sdk.model.request.CloseRequest;
import com.wiseasy.ecr.hub.sdk.model.request.PurchaseRequest;
import com.wiseasy.ecr.hub.sdk.model.request.QueryRequest;
import com.wiseasy.ecr.hub.sdk.model.request.RefundRequest;
import com.wiseasy.ecr.hub.sdk.model.response.CloseResponse;
import com.wiseasy.ecr.hub.sdk.model.response.PurchaseResponse;
import com.wiseasy.ecr.hub.sdk.model.response.QueryResponse;
import com.wiseasy.ecr.hub.sdk.model.response.RefundResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author wangyuxiang@wiseasy.com
 * @since 2023-09-14 14:59
 */
public class ECRHubWebSocketClientTest {

    public static final String APP_ID = "wz6012822ca2f1as78";

    private static ECRHubClient client;

    @BeforeAll
    public static void before() throws ECRHubException {
        ECRHubConfig config = new ECRHubConfig(APP_ID);
        client = ECRHubClientFactory.create("ws://192.168.100.30:35779", config);
        client.connect(); // Must
    }

    @Test
    @DisplayName("purchase")
    public void purchase() throws ECRHubException {
        // Setting read timeout,the timeout set here is valid for this request
        ECRHubConfig requestConfig = new ECRHubConfig();
        requestConfig.getSerialPortConfig().setReadTimeout(5 * 60 * 1000);

        // Purchase
        PurchaseRequest request = new PurchaseRequest();
        request.setMerchant_order_no("O" + System.currentTimeMillis());
        request.setOrder_amount("10");
        request.setPay_method_category("BANKCARD");
        request.setConfig(requestConfig);

        // Execute purchase request
        PurchaseResponse response = client.execute(request);
        System.out.println("Purchase Response:" + response);
    }

    @Test
    @DisplayName("purchase async")
    public void purchase_async() throws ECRHubException {
        // Purchase
        PurchaseRequest request = new PurchaseRequest();
        request.setMerchant_order_no("O" + System.currentTimeMillis());
        request.setOrder_amount("10");
        request.setTip_amount("2");
        request.setPay_method_category("BANKCARD");

        // Execute purchase request
        // Asynchronous return result
        client.asyncExecute(request, new ECRHubResponseCallBack<PurchaseResponse>() {
            @Override
            public void onResponse(PurchaseResponse response) {
                System.out.println("Purchase onCompleted:" + response);
            }

            @Override
            public void onTimeout(ECRHubTimeoutException e) {
                System.out.println("Purchase onTimeout.");
            }

            @Override
            public void onError(ECRHubException e) {
                System.out.println("Purchase onError:" + e.getMessage());
            }
        });
    }

    @Test
    @DisplayName("refund")
    public void refund() throws ECRHubException {
        RefundRequest request = new RefundRequest();
        request.setOrig_merchant_order_no("O1695032342508");
        request.setMerchant_order_no("O" + System.currentTimeMillis());
        request.setOrder_amount("1");
        request.setPay_method_category("BANKCARD");

        RefundResponse response = client.execute(request);
        System.out.println("Refund Response:" + response);
    }

    @Test
    @DisplayName("closeOrder")
    public void closeOrder() throws ECRHubException {
        CloseRequest request = new CloseRequest();
        request.setMerchant_order_no("O1695032342508");

        CloseResponse response = client.execute(request);
        System.out.println("Close Response:" + response);
    }

    @Test
    @DisplayName("queryOrder")
    public void queryOrder() throws ECRHubException {
        QueryRequest request = new QueryRequest();
        request.setMerchant_order_no("O1695032342508");

        QueryResponse response = client.execute(request);
        System.out.println("Query Response:" + response);
    }
}