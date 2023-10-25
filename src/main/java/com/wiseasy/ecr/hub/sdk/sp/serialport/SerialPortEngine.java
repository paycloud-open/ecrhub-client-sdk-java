package com.wiseasy.ecr.hub.sdk.sp.serialport;

import cn.hutool.cache.impl.FIFOCache;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.wiseasy.ecr.hub.sdk.ECRHubConfig.SerialPortConfig;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubTimeoutException;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubProtobufHelper;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubResponseProto;
import com.wiseasy.ecr.hub.sdk.utils.HexUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @program: ECR-Hub
 * @description:
 * @author: jzj
 * @create: 2023-09-08 11:44
 **/
public class SerialPortEngine {

    private static final Logger log = LoggerFactory.getLogger(SerialPortEngine.class);

    private final FIFOCache<String, byte[]> MSG_CACHE = new FIFOCache<>(20, 10 * 60 * 1000);
    private final SerialPortConfig config;
    private final SerialPort serialPort;
    private final Map<Byte, Integer> ackMap;
    private volatile boolean handshakeConfirm;
    private final AtomicLong heartbeatCounter;
    private ScheduledExecutorService scheduled;


    public SerialPortEngine(String portName, SerialPortConfig config) throws ECRHubException {
        this.config = config;
        this.serialPort = getSerialPort(portName);
        this.ackMap = new ConcurrentHashMap<>();
        this.heartbeatCounter = new AtomicLong();
    }

    private SerialPort getSerialPort(String portName) throws ECRHubException {
        if (StrUtil.isNotBlank(portName)) {
            // Specify the serial port name
            try {
                return SerialPort.getCommPort(portName);
            } catch (Exception e) {
                log.error("Serial port[{}] is invalid.", portName);
                throw new ECRHubException(e);
            }
        } else {
            // No serial port name specified, automatic search for serial port
            SerialPort port = findSerialPort(config.getPortNameKeyword());
            if (port != null) {
                return port;
            } else {
                throw new ECRHubException("Serial port not found, please confirm the USB cable is connected.");
            }
        }
    }

    private SerialPort findSerialPort(String portNameKeyword) {
        for (SerialPort port : SerialPort.getCommPorts()) {
            String portName = port.getDescriptivePortName();
            if (StrUtil.contains(portName, portNameKeyword)) {
                return port;
            }
        }
        return null;
    }

    public void connect(long startTime) throws ECRHubException {
        // Open serial port
        open();

        // Add data listener
        serialPort.removeDataListener();
        serialPort.addDataListener(new ReadDataListener());

        // Handshake
        handshake(startTime);

        // Start HeartBeat
        if (scheduled == null) {
            scheduled = ThreadUtil.createScheduledExecutor(2);
            scheduled.scheduleAtFixedRate(new SendHeartbeatThread(), 0, 1, TimeUnit.SECONDS);
            scheduled.scheduleAtFixedRate(new RecvHeartbeatThread(), 30, 30, TimeUnit.SECONDS);
        }
    }

    private void open() throws ECRHubException {
        if (!serialPort.isOpen()) {
            String portName = serialPort.getSystemPortName();
            log.info("Serial port[{}] opening...", portName);

            serialPort.setComPortParameters(config.getBaudRate(), config.getDataBits(), config.getStopBits(), config.getParity());
            serialPort.setComPortTimeouts(config.getTimeoutMode(), config.getReadTimeout(), config.getWriteTimeout());
            boolean opened = serialPort.openPort(5);
            if (!opened) {
                log.error("Serial port[{}] opening failed.", portName);
                throw new ECRHubException("Serial port[" + portName +"] opening failed.");
            }

            log.info("Serial port[{}] successful opened.", portName);
        }
    }

    private void handshake(long startTime) throws ECRHubException {
        log.info("Serial port handshake connecting...");

        handshakeConfirm = false;

        byte[] buffer = new SerialPortMessage.HandshakeMessage().encode();
        while (!doHandshake(buffer)) {
            ThreadUtil.safeSleep(10);
            if (System.currentTimeMillis() - startTime > config.getConnTimeout()) {
                serialPort.closePort();
                log.error("Serial port handshake connection failed.");
                throw new ECRHubTimeoutException("Serial port handshake connection timeout.");
            }
        }

        log.info("Serial port handshake connection successful.");
    }

    private boolean doHandshake(byte[] buffer) {
        // send handshake message
        if (!write(buffer)) {
            return false;
        } else {
            // read handshake confirm message
            long startTime = System.currentTimeMillis();
            while (!handshakeConfirm) {
                ThreadUtil.safeSleep(5);
                if (System.currentTimeMillis() - startTime > 1000) {
                    return false;
                }
            }
            return true;
        }
    }

    public boolean isOpen() {
        if (serialPort.isOpen()) {
            SerialPort port = findSerialPort(config.getPortNameKeyword());
            return (port != null);
        } else {
            return false;
        }
    }

    public boolean close() {
        if (!serialPort.isOpen()) {
            return true;
        } else {
            if (scheduled != null && !scheduled.isShutdown()) {
                scheduled.shutdown();
                scheduled = null;
            }
            if (ackMap != null) {
                ackMap.clear();
            }
            return serialPort.closePort();
        }
    }

    public boolean write(byte[] buffer) {
        if (isOpen()) {
            int numWritten = serialPort.writeBytes(buffer, buffer.length);
            return numWritten > 0;
        } else {
            return false;
        }
    }

    public void write(byte[] buffer, long startTime, long timeout) throws ECRHubException {
        SerialPortMessage message = new SerialPortMessage.DataMessage(buffer);
        byte[] byteMessage = message.encode();
        log.info("Send data message:{}", HexUtil.byte2hex(byteMessage));

        ackMap.put(message.messageId, 0);

        while (isOpen() && ackMap.containsKey(message.messageId)) {
            write(byteMessage);
            ThreadUtil.safeSleep(100);
            if (System.currentTimeMillis() - startTime > timeout) {
                ackMap.remove(message.messageId);
                throw new ECRHubTimeoutException("Write timeout");
            }
        }
    }

    public byte[] read(String requestId, long startTime, long timeout) throws ECRHubTimeoutException {
        byte[] buffer = new byte[0];
        while (isOpen()) {
            byte[] msg = MSG_CACHE.get(requestId);
            if (ArrayUtil.isNotEmpty(msg)) {
                MSG_CACHE.remove(requestId);
                buffer = msg;
                break;
            } else {
                ThreadUtil.safeSleep(20);
                if (System.currentTimeMillis() - startTime > timeout) {
                    throw new ECRHubTimeoutException("Read timeout");
                }
            }
        }
        return buffer;
    }

    private class SendHeartbeatThread implements Runnable {
        private final byte[] buffer;

        private SendHeartbeatThread() {
            this.buffer = new SerialPortMessage.HeartbeatMessage().encode();
        }

        @Override
        public void run() {
            SerialPortEngine.this.write(buffer);
            if (log.isDebugEnabled()) {
                log.debug("Send heartbeat message:{}", HexUtil.byte2hex(buffer));
            }
        }
    }

    private class RecvHeartbeatThread implements Runnable {
        @Override
        public void run() {
            long counter = heartbeatCounter.get();
            if (counter > 0) {
                heartbeatCounter.set(0);
            } else {
                SerialPortEngine.this.close();
                log.warn("No heartbeat message received from POS terminal, disconnected.");
            }
        }
    }

    private class ReadDataListener implements SerialPortDataListener {

        private final SerialPortMessageDecoder messageDecoder = new SerialPortMessageDecoder();
        private volatile String lastReceivedRequestId = null;

        @Override
        public int getListeningEvents() {
            return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
        }

        @Override
        public void serialEvent(SerialPortEvent event) {
            byte[] buffer = event.getReceivedData();
            if (buffer == null || buffer.length == 0) {
                return;
            }
            messageDecoder.decode(buffer).forEach(hexMessage -> {
                SerialPortMessage message = new SerialPortMessage().decodeHex(hexMessage);
                if (message != null) {
                    handlePack(message, hexMessage);
                }
            });
        }

        private void handlePack(SerialPortMessage message, String hexMessage) {
            switch (message.messageType) {
                case SerialPortMessage.MESSAGE_TYPE_HANDSHAKE_CONFIRM:
                    // Handshake confirm message
                    SerialPortEngine.this.handshakeConfirm = true;
                    break;
                case SerialPortMessage.MESSAGE_TYPE_COMMON:
                    // Common message
                    handleCommonPack(message, hexMessage);
                    break;
                default:
                    // Other message, ignore
                    break;
            }
        }

        private void handleCommonPack(SerialPortMessage message, String hexMessage) {
            byte messageAck = message.getMessageAck();
            byte messageId = message.getMessageId();
            if (0x00 == messageAck && 0x00 == messageId) {
                // Heartbeat packet
                log.debug("Received heartbeat message:{}", hexMessage);
                SerialPortEngine.this.heartbeatCounter.incrementAndGet();
            } else {
                // ACK packet
                if (0x00 != messageAck) {
                    log.info("Received ack message:{}", hexMessage);
                    // Remove Sent Times
                    SerialPortEngine.this.ackMap.remove(messageAck);
                }
                // Common packet
                if (0x00 != messageId) {
                    // Data packet
                    log.info("Received data message:{}", hexMessage);
                    // Send data ACK packet
                    sendAck(messageId);
                    // Cache data
                    putCache(message.getMessageData());
                }
            }
        }

        private void sendAck(byte id) {
            byte[] buffer = new SerialPortMessage.AckMessage(id).encode();
            log.info("Send ack message:{}", HexUtil.byte2hex(buffer));
            write(buffer);
        }

        private void putCache(byte[] data) {
            if (data == null || data.length == 0) {
                return;
            }
            ECRHubResponseProto.ECRHubResponse respProto = null;
            try {
                respProto = ECRHubProtobufHelper.unpack(data);
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }
            if (respProto == null) {
                return;
            }
            String requestId = respProto.getMsgId();
            if (StrUtil.isNotBlank(requestId) && !requestId.equals(lastReceivedRequestId)) {
                lastReceivedRequestId = requestId;
                MSG_CACHE.put(requestId, data);
            }
        }
    }
}