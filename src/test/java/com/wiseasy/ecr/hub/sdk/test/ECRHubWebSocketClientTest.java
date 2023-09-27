package com.wiseasy.ecr.hub.sdk.test;

import cn.hutool.core.util.RandomUtil;
import com.wiseasy.ecr.hub.sdk.ECRHubClient;
import com.wiseasy.ecr.hub.sdk.ECRHubClientFactory;
import com.wiseasy.ecr.hub.sdk.ECRHubConfig;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.model.request.PurchaseRequest;
import com.wiseasy.ecr.hub.sdk.model.request.QueryRequest;
import com.wiseasy.ecr.hub.sdk.model.response.QueryResponse;

/**
 * @author wangyuxiang@wiseasy.com
 * @since 2023-09-14 14:59
 */
public class ECRHubWebSocketClientTest {

    public static final String APP_ID = "wz6012822ca2f1as78";

    public static void main(String[] args) throws ECRHubException {
        ECRHubConfig config = new ECRHubConfig(APP_ID);
        ECRHubClient client = ECRHubClientFactory.create("ws://192.168.100.30:35779", config);
        client.connect(); // Must

        PurchaseRequest request = new PurchaseRequest();
        request.setMerchant_order_no(RandomUtil.randomNumbers(20));
        request.setOrder_amount("400");
        request.setTip_amount("200");
        request.setPay_method_category("BANKCARD");

        client.execute(request);

        for (int i = 0; i < 5; i++) {
            QueryRequest queryRequest = new QueryRequest();
            queryRequest.setMerchant_order_no(request.getMerchant_order_no());
            QueryResponse execute1 = client.execute(queryRequest);
        }
    }
}