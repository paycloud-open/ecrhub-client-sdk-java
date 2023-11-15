package com.wiseasy.ecr.hub.sdk.sp.serialport;

import cn.hutool.core.util.StrUtil;
import com.fazecast.jSerialComm.SerialPort;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubConnectionException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SerialPortHelper {

    public static SerialPort getSerialPort(String portName, String portNameKeyword) throws ECRHubConnectionException {
        if (StrUtil.isNotBlank(portName)) {
            // Specify the serial port name
            try {
                return SerialPort.getCommPort(portName);
            } catch (Exception e) {
                throw new ECRHubConnectionException(e.getMessage(), e);
            }
        } else {
            // No serial port name specified, automatic search for serial port
            SerialPort port = findSerialPort(portNameKeyword);
            if (port == null) {
                throw new ECRHubConnectionException("Serial port is not found, " +
                        "please check the USB cable is connected and POS terminal cashier App is launched");
            } else {
                return port;
            }
        }
    }

    public static SerialPort findSerialPort(String portNameKeyword) {
        for (SerialPort port : SerialPort.getCommPorts()) {
            String portName = port.getDescriptivePortName();
            if (StrUtil.contains(portName, portNameKeyword)) {
                return port;
            }
        }
        return null;
    }

    public static List<SerialPort> findSerialPorts() {
        List<SerialPort> list = new ArrayList<>();
        Collections.addAll(list, SerialPort.getCommPorts());
        return list;
    }

    public static List<String> findSerialPortNames() {
        List<String> list = new ArrayList<>();
        for (SerialPort port : SerialPort.getCommPorts()) {
            String name = port.getSystemPortName();
            if (!list.contains(name)) {
                list.add(name);
            }
        }
        return list;
    }
}