package com.wiseasy.ecr.hub.sdk.enums;

public enum EPayScenario {
    /**
     * Bank card payment
     */
    SWIPE_CARD("SWIPE_CARD"),
    /**
     * Customer Scan QR Pay
     */
    SCANQR_PAY("SCANQR_PAY"),
    /**
     * Merchant Scan QR Pay
     */
    BSCANQR_PAY("BSCANQR_PAY");

    EPayScenario(String val) {
        this.val = val;
    }

    private String val;

    public String getVal() {
        return val;
    }

    public static EPayScenario valOf(String val) {
        if (val != null) {
            for (EPayScenario item : values()) {
                if (item.getVal().equals(val)) {
                    return item;
                }
            }
        }
        return null;
    }
}