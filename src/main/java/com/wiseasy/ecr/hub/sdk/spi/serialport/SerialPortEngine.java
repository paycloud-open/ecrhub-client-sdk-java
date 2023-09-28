package com.wiseasy.ecr.hub.sdk.spi.serialport;

import cn.hutool.cache.impl.FIFOCache;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.wiseasy.ecr.hub.sdk.ECRHubConfig;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubTimeoutException;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubProtobufHelper;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubResponseProto;
import com.wiseasy.ecr.hub.sdk.utils.HexUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
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

    private volatile boolean isConnected = false;
    private final Lock lock = new ReentrantLock();
    private final String PORT_NAME_TAG = "GPS";
    private final ECRHubConfig config;
    private final SerialPort serialPort;
    private final SerialPortPacketDecoder packDecoder;

    private final BlockingQueue<byte[]> outQueue;
    private Thread writeThread;

    private final FIFOCache<String, String> MSG_CACHE = new FIFOCache<>(30, 15 * 60 * 1000);

    public SerialPortEngine(String portName, ECRHubConfig config) throws ECRHubException {
        this.config = config;
        this.serialPort = getCommPort(portName);
        this.packDecoder = new SerialPortPacketDecoder();
        this.outQueue = new LinkedBlockingQueue<>();
    }

    public SerialPort getCommPort(String portName) throws ECRHubException {
        SerialPort serialPort = null;
        if (StrUtil.isBlank(portName)) {
            serialPort = findSerialPort(PORT_NAME_TAG);
            if (serialPort == null) {
                throw new ECRHubException("The serial port name cannot be empty.");
            }
        } else {
            try {
                serialPort = SerialPort.getCommPort(portName);
            } catch (Exception e) {
                log.error("Invalid serial port name:{}", portName);
                throw new ECRHubException(e);
            }
        }
        if (serialPort.isOpen()) {
            throw new ECRHubException("This serial port is already occupied.");
        }
        return serialPort;
    }

    private SerialPort findSerialPort(String portNameTag) {
        for (SerialPort port : SerialPort.getCommPorts()) {
            String portName = port.getDescriptivePortName();
            if (StrUtil.contains(portName, portNameTag)) {
                return port;
            }
        }
        return null;
    }

    public boolean connect() throws ECRHubException {
        if (isConnected) {
            return true;
        }
        lock.lock();
        try {
            // Open serial port
            if (!serialPort.isOpen()) {
                open();
            }

            // Handshake
            handshake();

            // Add data listener
            serialPort.addDataListener(new ReadListener());

            // Start Write Thread
            writeThread = new Thread(new WriteThread());
            writeThread.start();

            isConnected = true;

            return true;
        } finally {
            lock.unlock();
        }
    }

    private void open() throws ECRHubException {
        ECRHubConfig.SerialPortConfig cfg = config.getSerialPortConfig();
        serialPort.setComPortParameters(cfg.getBaudRate(), cfg.getDataBits(), cfg.getStopBits(), cfg.getParity());
        serialPort.setComPortTimeouts(cfg.getTimeoutMode(), cfg.getReadTimeout(), cfg.getWriteTimeout());
        if (serialPort.openPort(100)) {
            log.info("Successfully open the serial port:{}", serialPort.getSystemPortName());
        } else {
            throw new ECRHubException("Failed to open the serial port:" + serialPort.getSystemPortName());
        }
    }

    private void handshake() throws ECRHubException {
        int timeout = config.getSerialPortConfig().getConnTimeout();
        long before = System.currentTimeMillis();
        while (true) {
            log.info("connecting...");
            if (doHandshake()) {
                log.info("Connection successful");
                return;
            } else {
                ThreadUtil.safeSleep(10);
                if (System.currentTimeMillis() - before > timeout) {
                    throw new ECRHubException("Connection timeout");
                }
            }
        }
    }

    private boolean doHandshake() {
        // send handshake packet
        byte[] msg = new SerialPortPacket.HandshakePacket().encode();
        int numWritten = serialPort.writeBytes(msg, msg.length);
        if (numWritten <= 0) {
            log.error("Send handshake packet fail");
            return false;
        }

        // read handshake confirm packet
        ThreadUtil.safeSleep(1000);
        byte[] buffer = new byte[0];
        int bytesAvailable = serialPort.bytesAvailable();
        if (bytesAvailable > 0) {
            buffer = new byte[bytesAvailable];
            serialPort.readBytes(buffer, buffer.length);
        }

        // decode handshake confirm packet
        if (buffer.length > 0) {
            for (String hexPack : packDecoder.decode(buffer)) {
                SerialPortPacket pack = new SerialPortPacket().decode(hexPack);
                if (pack != null && pack.getPackType() == SerialPortPacket.PACK_TYPE_HANDSHAKE_CONFIRM) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean close() {
        if (!isConnected) {
            return true;
        }
        lock.lock();
        try {
            // Stop Write Thread
            if (writeThread != null) {
                writeThread.interrupt();
                writeThread = null;
            }
            // Close Port
            boolean isClosed = serialPort.closePort();
            if (isClosed) {
                isConnected = false;
            }
            return isClosed;
        } finally {
            lock.unlock();
        }
    }

    public boolean isConnected() {
        return serialPort.isOpen() && isConnected;
    }

    public void safeWrite(byte[] bytes) {
        if (isConnected()) {
            outQueue.add(bytes);
        }
    }

    public void write(byte[] bytes) throws ECRHubException {
        if (isConnected()) {
            outQueue.add(bytes);
        } else {
            throw new ECRHubException("The serial port is not connected.");
        }
    }

    public byte[] read(String msgId, long timeout) throws ECRHubTimeoutException {
        long before = System.currentTimeMillis();
        while (true) {
            String msg = MSG_CACHE.get(msgId);
            if (StrUtil.isNotBlank(msg)) {
                MSG_CACHE.remove(msgId);
                return HexUtil.hex2byte(msg);
            } else {
                ThreadUtil.safeSleep(200);
                if (System.currentTimeMillis() - before > timeout) {
                    throw new ECRHubTimeoutException();
                }
            }
        }
    }

    private class WriteThread implements Runnable {
        @Override
        public void run() {
            Thread.currentThread().setName("SerialPortWriteThread-" + Thread.currentThread().getId());
            try {
                while (!Thread.interrupted()) {
                    byte[] bytes = outQueue.take();
                    serialPort.writeBytes(bytes, bytes.length);
                }
            } catch (InterruptedException e) {
                for (byte[] bytes : outQueue) {
                    serialPort.writeBytes(bytes, bytes.length);
                }
                Thread.currentThread().interrupt();
            }
        }
    }

    private class ReadListener implements SerialPortDataListener {

        @Override
        public int getListeningEvents() {
            return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
        }

        @Override
        public void serialEvent(SerialPortEvent event) {
            byte[] bytes = event.getReceivedData();
            if (bytes.length == 0) {
                return;
            }
            Set<String> packList = packDecoder.decode(bytes);
            packList.forEach(pack -> {
                try {
                    handlePack(pack);
                } catch (Exception e) {
                    log.warn("Handle packet[{}] error:", pack, e);
                }
            });
        }

        private void handlePack(String hexPack) {
            SerialPortPacket pack = new SerialPortPacket().decode(hexPack);
            if (pack == null || pack.getPackType() != SerialPortPacket.PACK_TYPE_COMMON) {
                return;
            }
            // ACK packet
            byte ack = pack.getAck();
            if (ack != 0x00) {
                log.debug("Received ACK packet:{}", hexPack);
            }
            // Common packet
            byte id = pack.getId();
            if (id == 0x00) {
                // HeartBeat packet
            } else {
                // Data packet
                log.debug("Received data packet:{}", hexPack);
                // Send data ACK packet
                sendAck(id);
                // Cache data
                putcache(pack.getData());
            }
        }

        private void sendAck(byte ack) {
            byte[] pack = new SerialPortPacket.AckPacket(ack).encode();
            log.debug("Send ACK packet:{}", HexUtil.byte2hex(pack));
            safeWrite(pack);
        }

        private void putcache(byte[] byteData) {
            if (byteData.length == 0) {
                return;
            }
            ECRHubResponseProto.ECRHubResponse respProto = null;
            try {
                respProto = ECRHubProtobufHelper.unpack(byteData);
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }
            if (respProto != null) {
                MSG_CACHE.put(respProto.getMsgId(), HexUtil.byte2hex(byteData));
            }
        }
    }
}