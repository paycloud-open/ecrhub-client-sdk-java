package com.wiseasy.ecr.hub.sdk.enums;

public enum EPayMethodCategory {
    /**
     * Bank card payment
     */
    BANKCARD("BANKCARD"),
    /**
     * Customers scan the merchant’s QR code for payment
     */
    QR_C_SCAN_B("QR_C_SCAN_B"),
    /**
     * Scan the code to pay, merchants scan the customer’s QR code
     */
    QR_B_SCAN_C("QR_B_SCAN_C");

    EPayMethodCategory(String val) {
        this.val = val;
    }

    private String val;

    public String getVal() {
        return val;
    }

    public static EPayMethodCategory valOf(String val) {
        if (val != null) {
            for (EPayMethodCategory item : values()) {
                if (item.getVal().equals(val)) {
                    return item;
                }
            }
        }
        return null;
    }
}