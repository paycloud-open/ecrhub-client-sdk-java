package com.wiseasy.ecr.hub.sdk.model.response;

import com.alibaba.fastjson2.annotation.JSONField;

public class CloseResponse extends ECRHubResponse {
    /**
     * Merchant Order No.
     */
    @JSONField(name = "merchantOrderNo")
    private String merchant_order_no;

    public String getMerchant_order_no() {
        return merchant_order_no;
    }

    public void setMerchant_order_no(String merchant_order_no) {
        this.merchant_order_no = merchant_order_no;
    }
}