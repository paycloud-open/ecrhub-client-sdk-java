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

    private final FIFOCache<String, byte[]> MSG_CACHE = new FIFOCache<>(50, 10 * 60 * 1000);
    private final Lock lock = new ReentrantLock();
    private final SerialPortConfig config;
    private final SerialPort serialPort;
    private final String serialPortName;
    private final Map<Byte, Integer> ackMap;
    private ScheduledExecutorService scheduled;
    private volatile boolean handshakeConfirm;
    private volatile boolean recvHeartbeat;
    private volatile boolean isRunning;

    public SerialPortEngine(String portName, SerialPortConfig config) throws ECRHubException {
        this.config = config;
        this.serialPort = getSerialPort(portName);
        this.serialPortName = serialPort.getSystemPortName();
        this.ackMap = new ConcurrentHashMap<>();
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
                throw new ECRHubException("Serial port not found, please check the USB cable is connected or POS terminal cashier App is launched");
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
        lock.lock();
        try {
            // Open serial port
            open();

            // Add data listener
            serialPort.addDataListener(new ReadDataListener());

            // Handshake
            handshakeConfirm = false;
            handshake(startTime);
            isRunning = true;

            // Start HeartBeat
            if (scheduled == null) {
                scheduled = ThreadUtil.createScheduledExecutor(2);
                scheduled.scheduleAtFixedRate(new SendHeartbeatThread(), 0, 2, TimeUnit.SECONDS);
                scheduled.scheduleAtFixedRate(new CheckHeartbeatThread(), 30, 30, TimeUnit.SECONDS);
            }
        } finally {
            lock.unlock();
        }
    }

    private void open() throws ECRHubException {
        if (!serialPort.isOpen()) {
            log.info("Serial port[{}] opening...", serialPortName);
            boolean success = serialPort.openPort(10);
            if (success) {
                serialPort.setComPortParameters(config.getBaudRate(), config.getDataBits(), config.getStopBits(), config.getParity());
                serialPort.setComPortTimeouts(config.getTimeoutMode(), config.getReadTimeout(), config.getWriteTimeout());
                log.info("Serial port[{}] open successful", serialPortName);
            } else {
                log.error("Serial port[{}] open failed", serialPortName);
                throw new ECRHubException("Serial port["+ serialPortName +"] open failed");
            }
        }
    }

    private void handshake(long startTime) throws ECRHubException {
        log.info("Serial port[{}] handshake connecting...", serialPortName);
        byte[] byteMsg = new SerialPortMessage.HandshakeMessage().encode();
        while (true) {
            if (doHandshake(byteMsg)) {
                log.info("Serial port[{}] handshake connection successful", serialPortName);
                break;
            } else {
                ThreadUtil.safeSleep(10);
                if (System.currentTimeMillis() - startTime > config.getConnTimeout()) {
                    this.close();
                    log.error("Serial port[{}] handshake connection failed", serialPortName);
                    throw new ECRHubTimeoutException("Serial port["+ serialPortName +"] handshake connection timeout");
                }
            }
        }
    }

    private boolean doHandshake(byte[] byteMsg) throws ECRHubException {
        // send handshake message
        try {
            write(byteMsg, config.getWriteTimeout(), TimeUnit.MILLISECONDS);
        } catch (ECRHubException e) {
            log.error("Serial port[{}] handshake connection failed, {}", serialPortName, e.getMessage());
            this.close();
            throw e;
        }
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

    public boolean isOpen() {
        return serialPort.isOpen();
    }

    public boolean close() {
        if (!serialPort.isOpen()) {
            return true;
        } else {
            serialPort.removeDataListener();
            boolean isClosed = serialPort.closePort();
            log.info("Close the serial port[{}]", serialPortName);
            return isClosed;
        }
    }

    public boolean disconnect() {
        if (!isRunning) {
            return true;
        } else {
            if (scheduled != null && !scheduled.isShutdown()) {
                scheduled.shutdown();
                scheduled = null;
            }
            ackMap.clear();
            handshakeConfirm = false;
            recvHeartbeat = false;
            isRunning = false;
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

        while (isRunning && ackMap.containsKey(message.messageId)) {
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
        while (isRunning) {
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
        private final byte[] byteMsg;
        private final String hexMsg;

        private SendHeartbeatThread() {
            this.byteMsg = new SerialPortMessage.HeartbeatMessage().encode();
            this.hexMsg = HexUtil.byte2hex(byteMsg);
        }

        @Override
        public void run() {
            if (isOpen() && handshakeConfirm) {
                try {
                    write(byteMsg, 1, TimeUnit.SECONDS);
                    log.debug("Send heartbeat message:{}", hexMsg);
                } catch (Exception e) {
                    // do nothing
                }
            }
        }
    }

    public class CheckHeartbeatThread implements Runnable {
        @Override
        public void run() {
            if (recvHeartbeat) {
                recvHeartbeat = false;
            } else {
                reconnect();
            }
        }

        private void reconnect() {
            lock.lock();
            try {
                log.info("Not received heartbeat message from POS terminal cashier App, trying to reconnect");
                close();
                connect(System.currentTimeMillis());
            } catch (Exception e) {
                // do nothing
            } finally {
                lock.unlock();
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
                    SerialPortEngine.this.handshakeConfirm = true;
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
                // Heartbeat packet
                log.debug("Received heartbeat message:{}", hexMsg);
                SerialPortEngine.this.recvHeartbeat = true;
            } else {
                // ACK packet
                if (0x00 != messageAck) {
                    log.info("Received ack message:{}", hexMsg);
                    // Remove Sent Times
                    SerialPortEngine.this.ackMap.remove(messageAck);
                }
                // Common packet
                if (0x00 != messageId) {
                    // Data packet
                    log.info("Received data message:{}", hexMsg);
                    // Send data ACK packet
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
            ECRHubResponseProto.ECRHubResponse respProto = null;
            try {
                respProto = ECRHubProtobufHelper.unpack(data);
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }
            if (respProto == null) {
                return;
            }
            String requestId = respProto.getRequestId();
            if (StrUtil.isNotBlank(requestId) && !requestId.equals(lastReceivedRequestId)) {
                lastReceivedRequestId = requestId;
                MSG_CACHE.put(requestId, data);
            }
        }
    }
}