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
    private static final long CHECK_HEART_INTERVAL = 30 * 1000;
    private static final long HEART_TIMEOUT = 2000;
    private static final long CONN_KEEPALIVE_TIMEOUT = 30 * 1000;
    private static final long MAX_RECONN_TIMES = 5;

    private final Lock lock = new ReentrantLock();
    private final SerialPortConfig config;
    private final SerialPort serialPort;
    private final String serialPortName;
    private final Map<Byte, Integer> ackMap;
    private final FIFOCache<String, byte[]> msgCache;
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
            doConnect(startTime);
            startScheduledTask();
            isWorking = true;
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

    private void startScheduledTask() {
        if (scheduledExecutor == null) {
            scheduledExecutor = ThreadUtil.createScheduledExecutor(2);
            scheduledExecutor.scheduleAtFixedRate(new SendHeartbeatThread(), 0, SEND_HEART_INTERVAL, TimeUnit.MILLISECONDS);
            scheduledExecutor.scheduleAtFixedRate(new CheckHeartbeatThread(), CHECK_HEART_INTERVAL, CHECK_HEART_INTERVAL, TimeUnit.MILLISECONDS);
        }
    }

    private void shutdownScheduledTask() {
        if (scheduledExecutor != null && !scheduledExecutor.isShutdown()) {
            scheduledExecutor.shutdown();
            scheduledExecutor = null;
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

    private boolean close() {
        if (!isOpen()) {
            return true;
        } else {
            isConnected = false;
            serialPort.removeDataListener();
            boolean isClosed = serialPort.closePort();
            log.info("Close the serial port[{}]", serialPortName);
            return isClosed;
        }
    }

    public void refreshLastReceivedHeartTime() {
        this.lastReceivedHeartTime = System.currentTimeMillis();
    }

    public boolean isReceivedHeart() {
        long nowTime = System.currentTimeMillis();
        long intervalTime = nowTime - lastReceivedHeartTime;
        return intervalTime > HEART_TIMEOUT;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public boolean disconnect() {
        if (!isWorking) {
            return true;
        } else {
            shutdownScheduledTask();
            ackMap.clear();
            isWorking = false;
            return close();
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
        log.info("Send data message:{}", HexUtil.byte2hex(byteMsg));

        ackMap.put(message.messageId, 0);

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
        byte[] buffer = new byte[0];
        while (isWorking) {
            byte[] msg = msgCache.get(requestId);
            if (ArrayUtil.isNotEmpty(msg)) {
                msgCache.remove(requestId);
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
                        SerialPortEngine.this.close();
                        log.error("Serial port[{}] handshake connection failed", serialPortName);
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
                log.error("Serial port[{}] handshake connection failed, {}", serialPortName, e.getMessage());
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

        private final byte[] byteMsg = new SerialPortMessage.HeartbeatMessage().encode();
        private final String hexMsg = HexUtil.byte2hex(byteMsg);

        @Override
        public void run() {
            if (isConnected()) {
                sendHeartbeat();
            }
        }

        private void sendHeartbeat() {
            try {
                write(byteMsg, 1, TimeUnit.SECONDS);
                log.debug("Send heartbeat message:{}", hexMsg);
            } catch (Exception e) {
                // do nothing
            }
        }
    }

    private class CheckHeartbeatThread implements Runnable {

        @Override
        public void run() {
            long nowTime = System.currentTimeMillis();
            long intervalTime = nowTime - lastReceivedHeartTime;
            if (intervalTime < CONN_KEEPALIVE_TIMEOUT) {
                return;
            }
            if (reconnectTimes.get() < MAX_RECONN_TIMES) {
                log.info("Not received heartbeat message from POS terminal cashier App, try to reconnect");
                reconnect();
                reconnectTimes.incrementAndGet();
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
            messageDecoder.decode(buffer).forEach(hexMsg -> {
                SerialPortMessage message = new SerialPortMessage().decodeHex(hexMsg);
                if (message != null) {
                    handlePack(message, hexMsg);
                }
            });
        }

        private void handlePack(SerialPortMessage message, String hexMsg) {
            switch (message.messageType) {
                case SerialPortMessage.MESSAGE_TYPE_HANDSHAKE_CONFIRM:
                    // Handshake confirm message
                    SerialPortEngine.this.isConnected = true;
                    break;
                case SerialPortMessage.MESSAGE_TYPE_COMMON:
                    // Common message
                    handleCommonPack(message, hexMsg);
                    break;
                default:
                    // Other message, ignore
                    break;
            }
        }

        private void handleCommonPack(SerialPortMessage message, String hexMsg) {
            byte messageAck = message.getMessageAck();
            byte messageId = message.getMessageId();
            if (0x00 == messageAck && 0x00 == messageId) {
                // Heartbeat message
                log.debug("Received heartbeat message:{}", hexMsg);
                refreshLastReceivedHeartTime();
            } else {
                // ACK message
                if (0x00 != messageAck) {
                    log.info("Received ack message:{}", hexMsg);
                    // Remove Sent Times
                    ackMap.remove(messageAck);
                }
                // Common packet
                if (0x00 != messageId) {
                    // Data message
                    log.info("Received data message:{}", hexMsg);
                    // Send data ACK message
                    sendAck(messageId);
                    // Cache data
                    putCache(message.getMessageData());
                }
            }
        }

        private void sendAck(byte id) {
            byte[] byteMsg = new SerialPortMessage.AckMessage(id).encode();
            try {
                write(byteMsg, 1, TimeUnit.SECONDS);
                log.info("Send ack message:{}", HexUtil.byte2hex(byteMsg));
            } catch (Exception e) {
                // do nothing
            }
        }

        private void putCache(byte[] data) {
            if (data == null || data.length == 0) {
                return;
            }
            ECRHubResponseProto.ECRHubResponse response = null;
            try {
                response = ECRHubProtobufHelper.unpack(data);
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }
            if (response == null) {
                return;
            }
            String requestId = response.getRequestId();
            if (StrUtil.isNotBlank(requestId) && !requestId.equals(lastReceivedRequestId)) {
                lastReceivedRequestId = requestId;
                msgCache.put(requestId, data);
            }
        }
    }
}