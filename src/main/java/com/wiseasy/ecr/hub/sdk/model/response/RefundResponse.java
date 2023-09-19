package com.wiseasy.ecr.hub.sdk.model.response;

import com.alibaba.fastjson2.annotation.JSONField;

public class RefundResponse extends ECRHubResponse {
    /**
     * Merchant Order No.
     */
    @JSONField(name = "merchantOrderNo")
    private String merchant_order_no;
    /**
     * Order Amount
     */
    @JSONField(name = "orderAmount")
    private String order_amount;
    /**
     * attach
     */
    @JSONField(name = "attach")
    private String attach;
    /**
     * Transaction status
     */
    @JSONField(name = "transStatus")
    private String trans_status;
    /**
     * PayCloud transaction No.
     */
    @JSONField(name = "transNo")
    private String trans_no;
    /**
     * Payment Channel Transaction No.
     */
    @JSONField(name = "payChannelTransNo")
    private String pay_channel_trans_no;
    /**
     * Transaction completion time
     */
    @JSONField(name = "transEndTime")
    private String trans_end_time;

    public String getMerchantOrderNo() {
        return merchant_order_no;
    }

    public void setMerchantOrderNo(String merchantOrderNo) {
        this.merchant_order_no = merchantOrderNo;
    }

    public String getOrder_amount() {
        return order_amount;
    }

    public void setOrder_amount(String order_amount) {
        this.order_amount = order_amount;
    }

    public String getAttach() {
        return attach;
    }

    public void setAttach(String attach) {
        this.attach = attach;
    }

    public String getTrans_status() {
        return trans_status;
    }

    public void setTrans_status(String trans_status) {
        this.trans_status = trans_status;
    }

    public String getTrans_no() {
        return trans_no;
    }

    public void setTrans_no(String trans_no) {
        this.trans_no = trans_no;
    }

    public String getPay_channel_trans_no() {
        return pay_channel_trans_no;
    }

    public void setPay_channel_trans_no(String pay_channel_trans_no) {
        this.pay_channel_trans_no = pay_channel_trans_no;
    }

    public String getTrans_end_time() {
        return trans_end_time;
    }

    public void setTrans_end_time(String trans_end_time) {
        this.trans_end_time = trans_end_time;
    }
}