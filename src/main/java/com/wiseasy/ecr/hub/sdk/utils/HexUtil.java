package com.wiseasy.ecr.hub.sdk.utils;

public class HexUtil {

    private static final char[] CS = "0123456789ABCDEF".toCharArray();

    public static String byte2hex(byte b) {
        char[] cs = new char[2];
        int io = 0;
        cs[io++] = CS[(b >> 4) & 0xF];
        cs[io++] = CS[(b >> 0) & 0xF];
        return new String(cs);
    }

    public static String byte2hex(byte[] bs) {
        char[] cs = new char[bs.length * 2];
        int io = 0;
        for (int n : bs) {
            cs[io++] = CS[(n >> 4) & 0xF];
            cs[io++] = CS[(n >> 0) & 0xF];
        }
        return new String(cs);
    }

    public static byte[] hex2byte(String s) {
        s = s.toUpperCase();
        int len = s.length() / 2;
        int ii = 0;
        byte[] bs = new byte[len];
        char c;
        int h;
        for (int i = 0; i < len; i++) {
            c = s.charAt(ii++);
            if (c <= '9') {
                h = c - '0';
            } else {
                h = c - 'A' + 10;
            }
            h <<= 4;
            c = s.charAt(ii++);
            if (c <= '9') {
                h |= c - '0';
            } else {
                h |= c - 'A' + 10;
            }
            bs[i] = (byte) h;
        }
        return bs;
    }
}