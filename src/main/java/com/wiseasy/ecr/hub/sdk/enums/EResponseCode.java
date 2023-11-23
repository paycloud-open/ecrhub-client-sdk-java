package com.wiseasy.ecr.hub.sdk.enums;

public enum EResponseCode {
    /**
     * Success
     */
    SUCCESS("000"),
    /**
     * Timeout
     */
    TIMEOUT("112");

    EResponseCode(String code) {
        this.code = code;
    }

    private String code;

    public String getCode() {
        return code;
    }

    public static EResponseCode codeOf(String code) {
        if (code != null) {
            for (EResponseCode item : values()) {
                if (item.getCode().equals(code)) {
                    return item;
                }
            }
        }
        return null;
    }
}