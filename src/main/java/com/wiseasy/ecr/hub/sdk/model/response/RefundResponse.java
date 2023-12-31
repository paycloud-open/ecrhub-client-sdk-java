package com.wiseasy.ecr.hub.sdk.model.response;

import com.alibaba.fastjson2.annotation.JSONField;

public class RefundResponse extends ECRHubResponse {
    /**
     * Merchant order No.
     * The order number for the refund request when refunded, different from the order number of the original consumer transaction. No more than 32 alphanumeric characters.
     *
     * For example: 1217752501201407033233368018
     */
    @JSONField(name = "merchantOrderNo")
    private String merchant_order_no;
    /**
     * Order Amount
     * Expressed in the quoted currency, for example, One USD stands for one dollar, not one cent
     *
     * For example: 34.50
     */
    @JSONField(name = "orderAmount")
    private String order_amount;
    /**
     * Attach
     * Allows merchants to submit an additional data to the gateway, which will be returned as-is for payment notifications and inquiries
     *
     * For example: abc
     */
    @JSONField(name = "attach")
    private String attach;
    /**
     * Transaction status
     *
     * @see com.wiseasy.ecr.hub.sdk.enums.ETransStatus
     *
     * For example: 2
     */
    @JSONField(name = "transStatus")
    private String trans_status;
    /**
     * PayCloud transaction No.
     *
     * For example: 51230016492309010000001
     */
    @JSONField(name = "transNo")
    private String trans_no;
    /**
     * Payment Channel Transaction No. such as WeChat, Alipay, Visa, Mastercard and other payment platforms
     *
     * For example: 4210001022202106045676702818
     */
    @JSONField(name = "payChannelTransNo")
    private String pay_channel_trans_no;
    /**
     * Time of successful trade, time zone: UTC/GMT+0, format: YYYY-MM-DD HH:mm:ss
     *
     * For Example: 2021-06-03 12:48:51
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