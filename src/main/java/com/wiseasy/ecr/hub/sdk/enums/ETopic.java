package com.wiseasy.ecr.hub.sdk.enums;

public enum ETopic {
    /**
     * pair
     */
    PAIR("ecrhub.pair"),
    /**
     * Unpair
     */
    UN_PAIR("ecrhub.unpair"),
    /**
     * pay.init
     */
    PAY_INIT("ecrhub.pay.init"),
    /**
     * pay.order
     */
    PAY_ORDER("ecrhub.pay.order"),
    /**
     * pay.query
     */
    QUERY_ORDER("ecrhub.pay.query"),
    /**
     * pay.close
     */
    CLOSE_ORDER("ecrhub.pay.close");

    ETopic(String val) {
        this.val = val;
    }

    private String val;

    public String getVal() {
        return val;
    }
}