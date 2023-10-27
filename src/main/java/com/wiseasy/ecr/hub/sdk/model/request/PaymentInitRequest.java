package com.wiseasy.ecr.hub.sdk.model.request;

import com.alibaba.fastjson2.annotation.JSONField;
import com.wiseasy.ecr.hub.sdk.enums.ETopic;
import com.wiseasy.ecr.hub.sdk.model.response.PaymentInitResponse;

public class PaymentInitRequest extends ECRHubRequest<PaymentInitResponse> {

    @Override
    public String getTopic() {
        return ETopic.PAY_INIT.getVal();
    }

    /**
     * Whether the transaction requires the cashier to click confirmation at the POS terminal.
     *
     * default value: false
     *
     * Value range:
     * true: terminal confirmation is required;
     * false: terminal confirmation is not required;
     *
     * Example: false
     */
    @JSONField(name = "confirmOnTerminal")
    private Boolean confirm_on_terminal;
    /**
     * Order queue mode, which sets the sorting rules for orders. When pushing multiple orders, priority is given to new or old orders.
     *
     * default value: FIFO
     *
     * Value range:
     * FIFO: first-in first-out
     * FILO: first-in last-out
     *
     * Example: FIFO
     */
    @JSONField(name = "orderQueueMode")
    private String order_queue_mode;
    /**
     * Whether transactions are automatically batch settlement.
     *
     * default value: true
     *
     * Value range:
     * true: auto settlement;
     * false: manual settlement;
     *
     * Example: true
     */
    @JSONField(name = "isAutoSettlement")
    private Boolean is_auto_settlement;

    public Boolean getConfirm_on_terminal() {
        return confirm_on_terminal;
    }

    public void setConfirm_on_terminal(Boolean confirm_on_terminal) {
        this.confirm_on_terminal = confirm_on_terminal;
    }

    public String getOrder_queue_mode() {
        return order_queue_mode;
    }

    public void setOrder_queue_mode(String order_queue_mode) {
        this.order_queue_mode = order_queue_mode;
    }

    public Boolean getIs_auto_settlement() {
        return is_auto_settlement;
    }

    public void setIs_auto_settlement(Boolean is_auto_settlement) {
        this.is_auto_settlement = is_auto_settlement;
    }
}