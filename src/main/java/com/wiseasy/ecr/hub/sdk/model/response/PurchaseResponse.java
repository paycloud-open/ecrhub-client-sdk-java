package com.wiseasy.ecr.hub.sdk.model.response;

import com.alibaba.fastjson2.annotation.JSONField;

public class PurchaseResponse extends ECRHubResponse {
    /**
     * Merchant Order No.
     */
    @JSONField(name = "merchantOrderNo")
    private String merchant_order_no;
    /**
     * Price Currency
     */
    @JSONField(name = "priceCurrency")
    private String price_currency;
    /**
     * Order Amount
     */
    @JSONField(name = "orderAmount")
    private String order_amount;
    /**
     * Tip Amount
     */
    @JSONField(name = "tipAmount")
    private String tip_amount;
    /**
     * Transaction type
     */
    @JSONField(name = "transType")
    private String trans_type;
    /**
     * Payment scenario
     */
    @JSONField(name = "payScenario")
    private String pay_scenario;
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
     * Amount paid by customer
     */
    @JSONField(name = "paidAmount")
    private String paid_amount;
    /**
     * Payment Channel Transaction No.
     */
    @JSONField(name = "payChannelTransNo")
    private String pay_channel_trans_no;
    /**
     * Payment User Account
     */
    @JSONField(name = "payUserAccountId")
    private String pay_user_account_id;
    /**
     * Payment method id
     */
    @JSONField(name = "payMethodId")
    private String pay_method_id;
    /**
     * Transaction completion time
     */
    @JSONField(name = "transEndTime")
    private String trans_end_time;

    public String getMerchant_order_no() {
        return merchant_order_no;
    }

    public void setMerchant_order_no(String merchant_order_no) {
        this.merchant_order_no = merchant_order_no;
    }

    public String getPrice_currency() {
        return price_currency;
    }

    public void setPrice_currency(String price_currency) {
        this.price_currency = price_currency;
    }

    public String getOrder_amount() {
        return order_amount;
    }

    public void setOrder_amount(String order_amount) {
        this.order_amount = order_amount;
    }

    public String getTip_amount() {
        return tip_amount;
    }

    public void setTip_amount(String tip_amount) {
        this.tip_amount = tip_amount;
    }

    public String getTrans_type() {
        return trans_type;
    }

    public void setTrans_type(String trans_type) {
        this.trans_type = trans_type;
    }

    public String getPay_scenario() {
        return pay_scenario;
    }

    public void setPay_scenario(String pay_scenario) {
        this.pay_scenario = pay_scenario;
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

    public String getPaid_amount() {
        return paid_amount;
    }

    public void setPaid_amount(String paid_amount) {
        this.paid_amount = paid_amount;
    }

    public String getPay_channel_trans_no() {
        return pay_channel_trans_no;
    }

    public void setPay_channel_trans_no(String pay_channel_trans_no) {
        this.pay_channel_trans_no = pay_channel_trans_no;
    }

    public String getPay_user_account_id() {
        return pay_user_account_id;
    }

    public void setPay_user_account_id(String pay_user_account_id) {
        this.pay_user_account_id = pay_user_account_id;
    }

    public String getPay_method_id() {
        return pay_method_id;
    }

    public void setPay_method_id(String pay_method_id) {
        this.pay_method_id = pay_method_id;
    }

    public String getTrans_end_time() {
        return trans_end_time;
    }

    public void setTrans_end_time(String trans_end_time) {
        this.trans_end_time = trans_end_time;
    }
}