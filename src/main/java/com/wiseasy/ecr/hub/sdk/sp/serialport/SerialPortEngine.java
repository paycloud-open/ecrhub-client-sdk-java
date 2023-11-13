package com.wiseasy.ecr.hub.sdk.sp.serialport;

import cn.hutool.cache.impl.FIFOCache;
import cn.hutool.core.thread.ThreadUtil;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @program: ECR-Hub
 * @description:
 * @author: jzj
 * @create: 2023-09-08 11:44
 **/
public class SerialPortEngine {

    private static final Logger log = LoggerFactory.getLogger(SerialPortEngine.class);

    private static final long SEND_HEART_INTERVAL = 1000;
    private static final long RECV_HEART_TIMEOUT = 3000;
    private static final long CHECK_HEART_INTERVAL = 30 * 1000;

    private final Lock lock = new ReentrantLock();
    private final SerialPortConfig config;
    private final SerialPort serialPort;
    private final String serialPortName;
    private final Map<Byte, Integer> ackMap;
    private final FIFOCache<String, SerialPortMessage> msgCache;
    private final AtomicInteger reconnectTimes;
    private ScheduledExecutorService scheduledExecutor;
    private volatile long lastReceivedHeartTime;
    private volatile boolean isConnected;
    private volatile boolean isWorking;

    public SerialPortEngine(String portName, SerialPortConfig config) throws ECRHubException {
        this.config = config;
        this.serialPort = SerialPortHelper.getSerialPort(portName, config.getPortNameKeyword());
        this.serialPortName = serialPort.getSystemPortName();
        this.ackMap = new ConcurrentHashMap<>();
        this.msgCache = new FIFOCache<>(50, 10 * 60 * 1000);
        this.reconnectTimes = new AtomicInteger();
    }

    public void connect(long startTime) throws ECRHubException {
        lock.lock();
        try {
            log.info("Serial port[{}] connecting...", serialPortName);
            doConnect(startTime);
            startScheduledTask();

            isWorking = true;

            log.info("Serial port[{}] connection successful", serialPortName);
        } finally {
            lock.unlock();
        }
    }

    private void doConnect(long startTime) throws ECRHubException {
        if (!isOpen()) {
            // Open serial port
            open();
            // Handshake connect
            new HandshakeHandler().handshake(startTime);
            // Refresh last received heart time
            refreshLastReceivedHeartTime();
            // Reset the reconnection times is zero
            reconnectTimes.set(0);
        }
    }

    private boolean isOpen() {
        return serialPort.isOpen();
    }

    private void open() throws ECRHubException {
        log.info("Serial port[{}] opening...", serialPortName);
        boolean success = serialPort.openPort(10);
        if (success) {
            serialPort.setComPortParameters(config.getBaudRate(), config.getDataBits(), config.getStopBits(), config.getParity());
            serialPort.setComPortTimeouts(config.getTimeoutMode(), config.getReadTimeout(), config.getWriteTimeout());
            // Add data listener
            serialPort.addDataListener(new ReadDataListener());
            log.info("Serial port[{}] open successful", serialPortName);
        } else {
            log.error("Serial port[{}] open failed", serialPortName);
            throw new ECRHubException("Serial port[" + serialPortName + "] open failed");
        }
    }

    private void close() {
        if (isOpen()) {
            log.info("Close the serial port[{}]", serialPortName);
            serialPort.removeDataListener();
            serialPort.closePort();
            isConnected = false;
        }
    }

    private void refreshLastReceivedHeartTime() {
        this.lastReceivedHeartTime = System.currentTimeMillis();
    }

    public boolean isReceivedHeart() {
        long nowTime = System.currentTimeMillis();
        long intervalTime = nowTime - lastReceivedHeartTime;
        return intervalTime < RECV_HEART_TIMEOUT;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void disconnect() {
        lock.lock();
        try {
            log.info("Serial port[{}] disconnecting...", serialPortName);
            shutdownScheduledTask();
            close();

            ackMap.clear();
            isWorking = false;

            log.info("Serial port[{}] disconnect successful", serialPortName);
        } finally {
            lock.unlock();
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

    public boolean write(byte[] buffer, long timeout, TimeUnit timeUnit) throws ECRHubException {
        try {
            Future<Boolean> f = CompletableFuture.supplyAsync(() -> write(buffer));
            return f.get(timeout, timeUnit);
        } catch (TimeoutException e) {
            throw new ECRHubTimeoutException("Write timeout");
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            throw new ECRHubException(e);
        }
    }

    public void write(byte[] buffer, long startTime, long timeout) throws ECRHubException {
        SerialPortMessage message = new SerialPortMessage.DataMessage(buffer);
        byte[] byteMsg = message.encode();
        log.info("Send data message:{}", message.getHexMessage());

        ackMap.put(message.messageId, 1);

        while (isWorking && ackMap.containsKey(message.messageId)) {
            write(byteMsg, timeout, TimeUnit.MILLISECONDS);
            ThreadUtil.safeSleep(100);
            if (System.currentTimeMillis() - startTime > timeout) {
                ackMap.remove(message.messageId);
                throw new ECRHubTimeoutException("Write timeout");
            }
        }
    }

    public byte[] read(String requestId, long startTime, long timeout) throws ECRHubException {
        SerialPortMessage message = readMessage(requestId, startTime, timeout);
        if (message != null) {
            return message.getMessageData();
        } else {
            return new byte[0];
        }
    }

    public SerialPortMessage readMessage(String requestId, long startTime, long timeout) throws ECRHubException {
        SerialPortMessage message = null;
        while (isWorking) {
            message = msgCache.get(requestId);
            if (message != null) {
                msgCache.remove(requestId);
                break;
            }
            ThreadUtil.safeSleep(20);
            if (System.currentTimeMillis() - startTime > timeout) {
                throw new ECRHubTimeoutException("Read timeout");
            }
        }
        return message;
    }

    public byte[] send(String requestId, byte[] bytes, long writeTimeout, long readTimeout) throws ECRHubException {
        // Write message
        log.info("Send data message:{}", HexUtil.byte2hex(bytes));
        write(bytes, writeTimeout, TimeUnit.MILLISECONDS);

        // Read message
        SerialPortMessage message = readMessage(requestId, System.currentTimeMillis(), readTimeout);
        if (message != null) {
            return HexUtil.hex2byte(message.getHexMessage());
        } else {
            return new byte[0];
        }
    }

    private void startScheduledTask() {
        if (scheduledExecutor == null) {
            scheduledExecutor = ThreadUtil.createScheduledExecutor(2);
            scheduledExecutor.scheduleAtFixedRate(new SendHeartbeatThread(), 5, SEND_HEART_INTERVAL, TimeUnit.MILLISECONDS);
            scheduledExecutor.scheduleAtFixedRate(new CheckHeartbeatThread(), CHECK_HEART_INTERVAL, CHECK_HEART_INTERVAL, TimeUnit.MILLISECONDS);
        }
    }

    private void shutdownScheduledTask() {
        if (scheduledExecutor != null && !scheduledExecutor.isShutdown()) {
            scheduledExecutor.shutdown();
            scheduledExecutor = null;
        }
    }

    private class HandshakeHandler {

        private final byte[] byteMsg = new SerialPortMessage.HandshakeMessage().encode();

        private void handshake(long startTime) throws ECRHubException {
            log.info("Serial port[{}] handshake connecting...", serialPortName);
            while (true) {
                if (doHandshake()) {
                    log.info("Serial port[{}] handshake connection successful", serialPortName);
                    break;
                } else {
                    ThreadUtil.safeSleep(10);
                    if (System.currentTimeMillis() - startTime > config.getConnTimeout()) {
                        log.error("Serial port[{}] handshake connection timeout", serialPortName);
                        SerialPortEngine.this.close();
                        throw new ECRHubTimeoutException("Serial port["+ serialPortName +"] handshake connection timeout");
                    }
                }
            }
        }

        private boolean doHandshake() throws ECRHubException {
            // Send handshake message
            try {
                write(byteMsg, config.getWriteTimeout(), TimeUnit.MILLISECONDS);
            } catch (ECRHubException e) {
                log.error("Serial port[{}] handshake connection failed, reason: {}", serialPortName, e.getMessage());
                SerialPortEngine.this.close();
                throw e;
            }
            // Read handshake confirm message
            long startTime = System.currentTimeMillis();
            while (!isConnected) {
                ThreadUtil.safeSleep(5);
                if (System.currentTimeMillis() - startTime > 1000) {
                    return false;
                }
            }
            return true;
        }
    }

    private class SendHeartbeatThread implements Runnable {

        private final SerialPortMessage message = new SerialPortMessage.HeartbeatMessage();
        private final byte[] byteMsg = message.encode();

        @Override
        public void run() {
            if (isConnected()) {
                sendHeartbeat();
            }
        }

        private void sendHeartbeat() {
            try {
                log.debug("Send heartbeat message:{}", message.getHexMessage());
                write(byteMsg, 800, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                // do nothing
            }
        }
    }

    private class CheckHeartbeatThread implements Runnable {

        private static final long MAX_RECONNECT_TIMES = 5;

        @Override
        public void run() {
            long nowTime = System.currentTimeMillis();
            long intervalTime = nowTime - lastReceivedHeartTime;
            if (intervalTime < CHECK_HEART_INTERVAL) {
                return;
            }
            if (reconnectTimes.get() < MAX_RECONNECT_TIMES) {
                log.info("Not received heartbeat message from POS terminal cashier App, try to reconnect");
                reconnectTimes.incrementAndGet();
                reconnect();
            }
        }

        private boolean reconnect() {
            try {
                close();
                connect(System.currentTimeMillis());
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    private class ReadDataListener implements SerialPortDataListener {

        private final SerialPortMessageDecoder decoder;

        private ReadDataListener() {
            this.decoder = new SerialPortMessageDecoder(new MessageHandler());
        }

        @Override
        public int getListeningEvents() {
            return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
        }

        @Override
        public void serialEvent(SerialPortEvent event) {
            byte[] data = event.getReceivedData();
            if (data != null && data.length > 0) {
                decoder.decode(data);
            }
        }
    }

    private class MessageHandler implements SerialPortMessageHandler {

        private volatile String lastReceivedRequestId = null;

        @Override
        public void handle(byte[] buffer) {
            if (buffer == null || buffer.length == 0) {
                return;
            }
            SerialPortMessage message = null;
            try {
                message = new SerialPortMessage().decode(buffer);
                if (message != null) {
                    handle(message);
                }
            } catch (Exception e) {
                String hexMsg = HexUtil.byte2hex(buffer);
                log.warn("Handle message["+ hexMsg +"] error: ", e);
            }
        }

        private void handle(SerialPortMessage message) {
            switch (message.messageType) {
                case SerialPortMessage.MESSAGE_TYPE_HANDSHAKE_CONFIRM:
                    // Handshake confirm message
                    SerialPortEngine.this.isConnected = true;
                    break;
                case SerialPortMessage.MESSAGE_TYPE_COMMON:
                    // Common message
                    handleCommonMessage(message);
                    break;
                default:
                    // Other message, ignore
                    break;
            }
        }

        private void handleCommonMessage(SerialPortMessage message) {
            String hexMessage = message.getHexMessage();
            byte messageAck = message.getMessageAck();
            byte messageId = message.getMessageId();
            if (0x00 == messageAck && 0x00 == messageId) {
                // Heartbeat message
                log.debug("Received heartbeat message:{}", hexMessage);
                refreshLastReceivedHeartTime();
            } else {
                // ACK message
                if (0x00 != messageAck) {
                    log.info("Received ack message:{}", hexMessage);
                    // Remove Sent Times
                    ackMap.remove(messageAck);
                }
                // Common packet
                if (0x00 != messageId) {
                    // Data message
                    log.info("Received data message:{}", hexMessage);
                    // Send data ACK message
                    sendAck(messageId);
                    // Cache message
                    putCache(message);
                }
            }
        }

        private void sendAck(byte id) {
            SerialPortMessage message = new SerialPortMessage.AckMessage(id);
            try {
                write(message.encode(), 800, TimeUnit.MILLISECONDS);
                log.info("Send ack message:{}", message.getHexMessage());
            } catch (Exception e) {
                // do nothing
            }
        }

        private void putCache(SerialPortMessage message) {
            byte[] data = message.getMessageData();
            if (data == null || data.length == 0) {
                return;
            }
            ECRHubResponseProto.ECRHubResponse response = null;
            try {
                response = ECRHubProtobufHelper.parseRespFrom(data);
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }
            if (response == null) {
                return;
            }
            String requestId = response.getRequestId();
            if (StrUtil.isNotBlank(requestId) && !requestId.equals(lastReceivedRequestId)) {
                lastReceivedRequestId = requestId;
                msgCache.put(requestId, message);
            }
        }
    }
}