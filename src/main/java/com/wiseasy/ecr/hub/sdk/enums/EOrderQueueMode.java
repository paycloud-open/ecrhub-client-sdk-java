package com.wiseasy.ecr.hub.sdk.enums;

public enum EOrderQueueMode {
    /**
     * first-in first-out
     */
    FIFO("FIFO"),
    /**
     * first-in last-out
     */
    FILO("FILO");

    EOrderQueueMode(String val) {
        this.val = val;
    }

    private String val;

    public String getVal() {
        return val;
    }
}