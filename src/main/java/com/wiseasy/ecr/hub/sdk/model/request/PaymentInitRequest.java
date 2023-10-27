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
     * Order need terminal confirmation. Default: false
     *
     * Value range:
     * - true: Terminal confirmation is required;
     * - false: No terminal confirmation is required.
     *
     * For example: true
     */
    @JSONField(name = "confirmOnTerminal")
    private Boolean confirm_on_terminal;
    /**
     * Order queue mode, which sets the sorting rules for orders. When pushing multiple orders, priority is given to new or old orders.
     *
     * Value range:
     * FIFO: first-in, first-out, default value
     * FILO: first-in last-out
     *
     * Example: FIFO
     */
    @JSONField(name = "orderQueueMode")
    private String order_queue_mode;
    /**
     * Order auto settlement. Default: true
     *
     * Value range:
     * - true: auto settlement;
     * - false: not auto settlement.
     *
     * For example: true
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