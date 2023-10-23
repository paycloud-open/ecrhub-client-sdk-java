package com.wiseasy.ecr.hub.sdk.model.response;

import com.alibaba.fastjson2.annotation.JSONField;

public class PurchaseResponse extends ECRHubResponse {
    /**
     * Merchant order No.
     * The order number for the refund request when refunded, different from the order number of the original consumer transaction. No more than 32 alphanumeric characters.
     *
     * For example: 1217752501201407033233368018
     */
    @JSONField(name = "merchantOrderNo")
    private String merchant_order_no;
    /**
     * Price Currency, ISO-4217 compliant, described in a three-character code
     *
     * For example: USD
     */
    @JSONField(name = "priceCurrency")
    private String price_currency;
    /**
     * Order Amount
     * Expressed in the quoted currency, for example, One USD stands for one dollar, not one cent
     *
     * For example: 34.50
     */
    @JSONField(name = "orderAmount")
    private String order_amount;
    /**
     * Tip Amount
     * The amount of the tip is expressed in the currency in which it is denominated, for example, 1 USD stands for one dollar, not one cent.
     *
     * For example: 1.50
     */
    @JSONField(name = "tipAmount")
    private String tip_amount;
    /**
     * Transaction type
     *
     * @see com.wiseasy.ecr.hub.sdk.enums.ETransType
     *
     * For example: 1
     */
    @JSONField(name = "transType")
    private String trans_type;
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
     * Amount paid by customer
     * The actual amount paid by the customer
     *
     * For example: 36.00
     */
    @JSONField(name = "paidAmount")
    private String paid_amount;
    /**
     * Merchant discount amount
     *
     * For example: 10.00
     */
    @JSONField(name = "discountBmopc")
    private String discount_bmopc;
    /**
     * Payment Channel discount amount
     *
     * For example: 6.00
     */
    @JSONField(name = "discountBpc")
    private String discount_bpc;
    /**
     * Payment User Account
     *
     * For example: 6227***6666
     */
    @JSONField(name = "payUserAccountId")
    private String pay_user_account_id;
    /**
     * Payment method id
     *
     * @see com.wiseasy.ecr.hub.sdk.enums.EPayMethod
     *
     * For example: Visa
     */
    @JSONField(name = "payMethodId")
    private String pay_method_id;
    /**
     * Payment scenario
     *
     * @see com.wiseasy.ecr.hub.sdk.enums.EPayScenario
     */
    @JSONField(name = "payScenario")
    private String pay_scenario;
    /**
     * Store no
     */
    @JSONField(name = "storeNo")
    private String store_no;
    /**
     * Time of successful trade, time zone: UTC/GMT+0, format: YYYY-MM-DD HH:mm:ss
     *
     * For Example: 2021-06-03 12:48:51
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

    public String getPaid_amount() {
        return paid_amount;
    }

    public void setPaid_amount(String paid_amount) {
        this.paid_amount = paid_amount;
    }

    public String getDiscount_bmopc() {
        return discount_bmopc;
    }

    public void setDiscount_bmopc(String discount_bmopc) {
        this.discount_bmopc = discount_bmopc;
    }

    public String getDiscount_bpc() {
        return discount_bpc;
    }

    public void setDiscount_bpc(String discount_bpc) {
        this.discount_bpc = discount_bpc;
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

    public String getPay_scenario() {
        return pay_scenario;
    }

    public void setPay_scenario(String pay_scenario) {
        this.pay_scenario = pay_scenario;
    }

    public String getStore_no() {
        return store_no;
    }

    public void setStore_no(String store_no) {
        this.store_no = store_no;
    }

    public String getTrans_end_time() {
        return trans_end_time;
    }

    public void setTrans_end_time(String trans_end_time) {
        this.trans_end_time = trans_end_time;
    }
}