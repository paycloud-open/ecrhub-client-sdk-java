package com.wiseasy.ecr.hub.sdk.sp.serialport;

public interface SerialPortMessageHandler {

    void handle(byte[] message);

}