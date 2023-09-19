package com.wiseasy.ecr.hub.sdk.model.request;

import com.alibaba.fastjson2.annotation.JSONField;
import com.wiseasy.ecr.hub.sdk.model.response.CloseResponse;

public class CloseRequest extends ECRHubRequest<CloseResponse> {

    @Override
    public String getTopic() {
        return "ecrhub.pay.close";
    }

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