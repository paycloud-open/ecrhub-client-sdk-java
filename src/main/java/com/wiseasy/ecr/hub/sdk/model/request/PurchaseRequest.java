package com.wiseasy.ecr.hub.sdk.model.request;

import com.alibaba.fastjson2.annotation.JSONField;
import com.wiseasy.ecr.hub.sdk.enums.ETopic;
import com.wiseasy.ecr.hub.sdk.enums.ETransType;
import com.wiseasy.ecr.hub.sdk.model.response.PurchaseResponse;

public class PurchaseRequest extends ECRHubRequest<PurchaseResponse> {

    @Override
    public String getTopic() {
        return ETopic.PAY_ORDER.getValue();
    }

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
    private String trans_type = ETransType.PURCHASE.getCode();
    /**
     * Payment Methods Category
     *
     * @see com.wiseasy.ecr.hub.sdk.enums.EPayMethodCategory
     *
     * For example: BANKCARD
     */
    @JSONField(name = "payMethodCategory")
    private String pay_method_category;
    /**
     * Attach
     * Allows merchants to submit an additional data to the gateway, which will be returned as-is for payment notifications and inquiries
     *
     * For example: abc
     */
    @JSONField(name = "attach")
    private String attach;
    /**
     * Order description
     * A brief description of the goods or services purchased by the customer
     *
     * For example: IPhone White X2
     */
    @JSONField(name = "description")
    private String description;
    /**
     * PayCloud backend server callback address after successful payment
     * Receive payment notifications from the gateway to call back the server address, and only when the transaction goes through the payment gateway will there be a callback.
     *
     * For example: http://www.xxx.com/notify
     */
    @JSONField(name = "notifyUrl")
    private String notify_url;
    /**
     * Order need terminal confirmation. Default: true
     * - true: Terminal confirmation is required;
     * - false: No terminal confirmation is required.
     *
     * For example: true
     */
    @JSONField(name = "confirmOnTerminal")
    private boolean confirm_on_terminal = true;
    /**
     * Order expires time, in seconds. Default to 300 seconds.
     *
     * For example: 300
     */
    @JSONField(name = "expires")
    private String expires;

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

    public boolean isConfirm_on_terminal() {
        return confirm_on_terminal;
    }

    public void setConfirm_on_terminal(boolean confirm_on_terminal) {
        this.confirm_on_terminal = confirm_on_terminal;
    }

    public String getExpires() {
        return expires;
    }

    public void setExpires(String expires) {
        this.expires = expires;
    }
}