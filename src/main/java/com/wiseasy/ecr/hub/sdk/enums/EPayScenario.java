package com.wiseasy.ecr.hub.sdk.enums;

public enum EPayScenario {
    /**
     * Customer Scan QR Pay
     */
    SCANQR_PAY("SCANQR_PAY"),
    /**
     * Merchant Scan QR Pay
     */
    BSCANQR_PAY("BSCANQR_PAY"),
    /**
     * Mobile Wap Pay
     */
    WAP_PAY("WAP_PAY"),
    /**
     * Online Web Pay
     */
    WEB_PAY("WEB_PAY"),
    /**
     * In-App Pay
     */
    INAPP_PAY("INAPP_PAY"),
    /**
     * Mini program payment
     */
    MINI_PROGRAM_PAY("MINI_PROGRAM_PAY"),
    /**
     * Mobile web payment (offline)
     */
    ENTRYCODE_PAY("ENTRYCODE_PAY"),
    /**
     * Auto debit payment
     */
    AUTO_DEBIT_PAY("AUTO_DEBIT_PAY"),
    /**
     * OTPCode Pay
     */
    OTPCODE_PAY("OTPCODE_PAY"),
    /**
     * Bank card payment
     */
    SWIPE_CARD("SWIPE_CARD");

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