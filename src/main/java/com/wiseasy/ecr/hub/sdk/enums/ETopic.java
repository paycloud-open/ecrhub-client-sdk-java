package com.wiseasy.ecr.hub.sdk.enums;

public enum ETopic {
    /**
     * pair
     */
    PAIR("ecrhub.pair"),
    /**
     * init
     */
    INIT("ecrhub.init"),
    /**
     * heartbeat
     */
    HEARTBEAT("ecrhub.heartbeat"),
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

    ETopic(String value) {
        this.value = value;
    }

    private String value;

    public String getValue() {
        return value;
    }
}