package com.wiseasy.ecr.hub.sdk.model.request;

import com.alibaba.fastjson2.annotation.JSONField;
import com.wiseasy.ecr.hub.sdk.enums.ETransType;
import com.wiseasy.ecr.hub.sdk.model.response.RefundResponse;

public class RefundRequest extends ECRHubRequest<RefundResponse> {

    @Override
    public String getTopic() {
        return "ecrhub.pay.order";
    }

    /**
     * Merchant Order No.
     */
    @JSONField(name = "merchantOrderNo")
    private String merchant_order_no;
    /**
     * Original Merchant Order No.
     */
    @JSONField(name = "origMerchantOrderNo")
    private String orig_merchant_order_no;
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
    private String trans_type = ETransType.REFUND.getCode();
    /**
     * Payment Methods Category
     *
     * QR_C_SCAN_B: Customer Scan merchant payment code
     * QR_B_SCAN_C: Merchant Scan customer payment code
     * BANKCARD: Bank card payment
     */
    @JSONField(name = "payMethodCategory")
    private String pay_method_category;
    /**
     * attach
     */
    @JSONField(name = "attach")
    private String attach;
    /**
     * Order description
     */
    @JSONField(name = "description")
    private String description;
    /**
     * PayCloud backend server callback address after successful payment
     */
    @JSONField(name = "notifyUrl")
    private String notify_url;

    public String getMerchant_order_no() {
        return merchant_order_no;
    }

    public void setMerchant_order_no(String merchant_order_no) {
        this.merchant_order_no = merchant_order_no;
    }

    public String getOrig_merchant_order_no() {
        return orig_merchant_order_no;
    }

    public void setOrig_merchant_order_no(String orig_merchant_order_no) {
        this.orig_merchant_order_no = orig_merchant_order_no;
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

    public String getPay_method_category() {
        return pay_method_category;
    }

    public void setPay_method_category(String pay_method_category) {
        this.pay_method_category = pay_method_category;
    }

    public String getAttach() {
        return attach;
    }

    public void setAttach(String attach) {
        this.attach = attach;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNotify_url() {
        return notify_url;
    }

    public void setNotify_url(String notify_url) {
        this.notify_url = notify_url;
    }
}