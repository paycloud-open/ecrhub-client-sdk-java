package com.wiseasy.ecr.hub.sdk.enums;

public enum EPayMethod {
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
    WeChatPay("WeChatPay"),
    /**
     * China UnionPay bank card and mobile phone payment
     */
    UnionPay("UnionPay"),
    /**
     * NIBSS QR
     */
    NibssQR("NibssQR"),
    /**
     * Myanmar’s leading e-wallet payment product
     */
    KBZPay("KBZPay"),
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
    DinnersClub("DinnersClub");

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