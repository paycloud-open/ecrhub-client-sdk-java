package com.wiseasy.ecr.hub.sdk.test;

import cn.hutool.core.thread.ThreadUtil;
import com.wiseasy.ecr.hub.sdk.ClientListener;
import com.wiseasy.ecr.hub.sdk.ECRHubConfig;
import com.wiseasy.ecr.hub.sdk.ECRHubServerFactory;
import com.wiseasy.ecr.hub.sdk.ECRHubServer;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.model.ECRHubTerminal;
import com.wiseasy.ecr.hub.sdk.model.request.PurchaseRequest;
import com.wiseasy.ecr.hub.sdk.model.response.PurchaseResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author wangyuxiang@wiseasy.com
 * @since 2023-10-13 11:09
 */
public class ECRHubSocketServerTest {

    public static final String APP_ID = "wz6012822ca2f1as78";

    private static ECRHubServer client;

    @BeforeAll
    public static void before() throws ECRHubException {
        ECRHubConfig config = new ECRHubConfig(APP_ID);
        config.getSocketServerConfig().setRegisterMDNS(true);
        client = ECRHubServerFactory.createSocketServer(config);

        client.setClientListener(new ClientListener() {
            @Override
            public void onConnect(ECRHubTerminal terminal) {
                System.out.println("onConnect: " + terminal);
            }

            @Override
            public void onDisconnect(ECRHubTerminal terminal, int code, String reason, boolean remote) {
                System.out.println("onDisconnect: " + terminal);
            }
        });

        client.start();
    }

    @AfterAll
    public static void after() throws ECRHubException {
        client.stop();
    }

    @Test
    @DisplayName("purchase")
    public void purchase() throws ECRHubException {
        // Setting read timeout,the timeout set here is valid for this request
        ECRHubConfig requestConfig = new ECRHubConfig();

        // Purchase
        PurchaseRequest request = new PurchaseRequest();
        request.setMerchant_order_no("O" + System.currentTimeMillis());
        request.setOrder_amount("10");
        request.setPay_method_category("BANKCARD");
        request.setConfig(requestConfig);

        List<ECRHubTerminal> terminalList = client.getTerminalList();
        while (terminalList.isEmpty()) {
            terminalList = client.getTerminalList();
            ThreadUtil.safeSleep(1000);
        }

        for (ECRHubTerminal ecrHubTerminal : terminalList) {
            // Execute purchase request
            PurchaseResponse response = client.execute(ecrHubTerminal, request);
            System.out.println("Purchase Response:" + response);
        }

        client.stop();

    }

}
