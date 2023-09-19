package com.wiseasy.ecr.hub.sdk.enums;

public enum ETransType {
    /**
     * Purchase/Sale
     */
    PURCHASE("1"),
    /**
     * Cancel/Void
     */
    CANCEL("2"),
    /**
     * Refund/Return
     */
    REFUND("3"),
    /**
     * Pre-auth
     */
    PRE_AUTH("4"),
    /**
     * Pre-auth cancel
     */
    PRE_AUTH_CANCEL("5"),
    /**
     * Pre-auth completion
     */
    PRE_AUTH_COMPLETION("6"),
    /**
     * Pre-auth completion cancel
     */
    PRE_AUTH_COMPLETION_CANCEL("7"),
    /**
     * Pre-auth completion refund
     */
    PRE_AUTH_COMPLETION_REFUND("8"),
    /**
     * Cashback
     */
    CASH_BACK("11"),
    /**
     * Cash Withdrawal
     */
    CASH_WITHDRAWAL("12");

    ETransType(String code) {
        this.code = code;
    }

    private String code;

    public String getCode() {
        return code;
    }
}