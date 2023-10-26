package com.wiseasy.ecr.hub.sdk.enums;

public enum EPayMethod {
    /**
     * Visa
     */
    Visa("Visa"),
    /**
     * MasterCard
     */
    MasterCard("MasterCard"),
    /**
     * AmericanExpress
     */
    Amex("Amex"),
    /**
     * Discover Card
     */
    Discover("Discover"),
    /**
     * Japan Credit Bureau
     */
    JCB("JCB"),
    /**
     * DinnersClub
     */
    DinnersClub("DinnersClub"),
    /**
     * MyDebit
     */
    MyDebit("MyDebit"),
    /**
     * China UnionPay bank card and mobile phone payment
     */
    UnionPay("UnionPay"),
    /**
     * Alipay+, which is not the same product as Alipay, is an aggregated payment product that supports AlipayCN, but also AlipayHK, GCash, TNG and other Alipay wallets.
     */
    AlipayPlus("Alipay+"),
    /**
     * Alipay(By default, please use Alipay+. To use Alipay, you need to contact the merchant to support a separate subscription)
     */
    Alipay("Alipay"),
    /**
     * Tencent’s mobile payment wallet in Shenzhen, China
     */
    WeChatPay("WeChatPay");

    EPayMethod(String val) {
        this.val = val;
    }

    private String val;

    public String getVal() {
        return val;
    }

    public static EPayMethod valOf(String val) {
        if (val != null) {
            for (EPayMethod item : values()) {
                if (item.getVal().equals(val)) {
                    return item;
                }
            }
        }
        return null;
    }
}