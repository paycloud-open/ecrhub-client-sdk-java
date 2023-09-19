package com.wiseasy.ecr.hub.sdk.spi.serialport;

import cn.hutool.cache.impl.FIFOCache;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;
import com.wiseasy.ecr.hub.sdk.ECRHubConfig;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubException;
import com.wiseasy.ecr.hub.sdk.exception.ECRHubTimeoutException;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubProtobufHelper;
import com.wiseasy.ecr.hub.sdk.protobuf.ECRHubResponseProto;
import com.wiseasy.ecr.hub.sdk.utils.HexUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final ECRHubConfig config;
    private final SerialPort serialPort;

    private final BlockingQueue<byte[]> outQueue;
    private Thread writeThread;

    private final FIFOCache<String, String> MSG_CACHE = new FIFOCache<>(20, 10 * 60 * 1000);

    public SerialPortEngine(String portName, ECRHubConfig config) throws ECRHubException {
        this.config = config;
        this.serialPort = getCommPort(portName);
        this.outQueue = new LinkedBlockingQueue<>();
    }

    /**
     * 获取串口
     *
     * @param portName 端口名称
     * @return 串口对象
     */
    public SerialPort getCommPort(String portName) throws ECRHubException {
        SerialPort serialPort = null;
        try {
            serialPort = SerialPort.getCommPort(portName);
        } catch (SerialPortInvalidPortException e) {
            log.error(e.getMessage(), e);
            throw new ECRHubException(e.getMessage());
        }
        if (serialPort.isOpen()) {
            throw new ECRHubException("This serial port is already occupied.");
        }
        return serialPort;
    }

    /**
     * 打开串口
     *
     * @return
     */
    public synchronized boolean open() throws ECRHubException {
        if (isOpen()) {
            return true;
        }

        // Open the serial port
        ECRHubConfig.SerialPortConfig cfg = config.getSerialPortConfig();
        serialPort.setComPortParameters(cfg.getBaudRate(), cfg.getDataBits(), cfg.getStopBits(), cfg.getParity());
        serialPort.setComPortTimeouts(cfg.getTimeoutMode(), cfg.getReadTimeout(), cfg.getWriteTimeout());
        if (!serialPort.openPort()) {
            throw new ECRHubException("Failed to open the serial port:" + serialPort.getSystemPortPath());
        }

        log.info("Successfully open the serial port:{}", serialPort.getSystemPortPath());

        // Start Write Thread
        writeThread = new Thread(new WriteThread());
        writeThread.start();

        // Add data listener
        serialPort.addDataListener(new ReceiveDataListener());

        // Send handshake
        sendHandshake();

        return true;
    }

    /**
     * 串口是否已打开
     *
     * @return
     */
    public boolean isOpen() {
        return serialPort.isOpen();
    }

    /**
     * 关闭串口
     *
     * @param
     */
    public synchronized boolean close() {
        if (!isOpen()) {
            return true;
        }

        // Stop Write Thread
        if (writeThread != null) {
            writeThread.interrupt();
            writeThread = null;
        }

        // Remove listener
        serialPort.removeDataListener();

        // Close Port
        return serialPort.closePort();
    }

    /**
     * 发送心跳包
     *
     * @throws ECRHubException
     */
    private void sendHandshake() throws ECRHubException {
        byte[] pack = new SerialPortPackage.HandshakePackage().encode();
        log.debug("Send handshake packet:{}", HexUtil.byte2hex(pack));
        write(pack);
    }

    /**
     * 发送数据ACK包
     *
     * @param ack
     */
    private void sendAck(byte ack) {
        byte[] pack = new SerialPortPackage.AckPackage(ack).encode();
        log.debug("Send ACK packet:{}", HexUtil.byte2hex(pack));
        safeWrite(pack);
    }

    /**
     * 往串口发送数据，不抛异常
     *
     * @param bytes 待发送数据
     */
    public void safeWrite(byte[] bytes) {
        if (isOpen()) {
            outQueue.add(bytes);
        }
    }

    /**
     * 往串口发送数据
     *
     * @param bytes 待发送数据
     */
    public void write(byte[] bytes) throws ECRHubException {
        if (isOpen()) {
            outQueue.add(bytes);
        } else {
            throw new ECRHubException("串口未打开");
        }
    }

    /**
     * 读取串口数据
     */
    public byte[] read(String msgId, long timeout) throws ECRHubTimeoutException {
        String msg = MSG_CACHE.get(msgId);

        long before = System.currentTimeMillis();
        while (StrUtil.isBlank(msg)) {
            if (System.currentTimeMillis() - before > timeout) {
                throw new ECRHubTimeoutException();
            }
            ThreadUtil.safeSleep(50);
            msg = MSG_CACHE.get(msgId);
        }

        MSG_CACHE.remove(msgId);

        return HexUtil.hex2byte(msg);
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

    private class ReceiveDataListener implements SerialPortDataListener {

        private final Lock lock = new ReentrantLock();
        private final StringBuilder lastRemainingBuffer = new StringBuilder();

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
            lock.lock();
            try {
                String hexPack = lastRemainingBuffer + HexUtil.byte2hex(bytes);
                lastRemainingBuffer.setLength(0);
                handlePack(hexPack, lastRemainingBuffer);
            } finally {
                lock.unlock();
            }
        }

        private void handlePack(String pack, StringBuilder lastRemainingBuffer) {
            while (StrUtil.isNotBlank(pack)) {
                if (pack.startsWith(SerialPortPackage.PACK_HEAD)) {
                    // Start with HEAD
                    int tailIndex = pack.indexOf(SerialPortPackage.PACK_TAIL);
                    if (tailIndex == -1) {
                        lastRemainingBuffer.append(pack);
                        break;
                    }
                    String validPack = StrUtil.subPre(pack, tailIndex + SerialPortPackage.PACK_TAIL.length());
                    pack = StrUtil.removePrefix(pack, validPack);
                    try {
                        handlePack(validPack);
                    } catch (Exception e) {
                        log.warn("handlePack [{}] error", validPack, e);
                    }
                } else {
                    // Not start with HEAD
                    int headIndex = pack.indexOf(SerialPortPackage.PACK_HEAD);
                    if (headIndex == -1) {
                        break;
                    }
                    // Remove invalid packet
                    String invalidPack = StrUtil.subPre(pack, headIndex);
                    pack = StrUtil.removePrefix(pack, invalidPack);
                }
            }
        }

        private void handlePack(String hexPack) {
            SerialPortPackage pack = new SerialPortPackage().decode(HexUtil.hex2byte(hexPack));
            switch (pack.getPackageType()) {
                case SerialPortPackage.PACKAGETYPE_HANDSHAKE_CONFIRM:
                    // Handshake confirm packet
                    log.debug("Received handshake confirm packet:{}", hexPack);
                case SerialPortPackage.PACKAGETYPE_COMMON:
                    // ACK packet
                    byte ack = pack.getAck();
                    if (ack != 0x00) {
                        log.debug("Received ACK packet:{}", hexPack);
                    }
                    // Common packet
                    byte id = pack.getId();
                    if (id != 0x00) {
                        log.debug("Received data packet:{}", hexPack);
                        // Send ACK packet
                        sendAck(id);
                        // Cache data
                        putcache(HexUtil.byte2hex(pack.getData()));
                    }
            }
        }

        private void putcache(String hexData) {
            if (StrUtil.isBlank(hexData)) {
                return;
            }
            ECRHubResponseProto.ECRHubResponse respProto = null;
            try {
                respProto = ECRHubProtobufHelper.unpack(HexUtil.hex2byte(hexData));
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }
            if (respProto != null) {
                MSG_CACHE.put(respProto.getMsgId(), hexData);
            }
        }
    }
}