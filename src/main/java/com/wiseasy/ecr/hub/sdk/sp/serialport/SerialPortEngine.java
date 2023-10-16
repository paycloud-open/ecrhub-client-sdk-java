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

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @program: ECR-Hub
 * @description:
 * @author: jzj
 * @create: 2023-09-08 11:44
 **/
public class SerialPortEngine {

    private static final Logger log = LoggerFactory.getLogger(SerialPortEngine.class);

    private final SerialPortConfig config;
    private final SerialPort serialPort;
    private final SerialPortPacketDecoder packDecoder;

    private final BlockingQueue<byte[]> writeQueue;
    private Thread writeThread;

    private final FIFOCache<String, String> MSG_CACHE = new FIFOCache<>(20, 10 * 60 * 1000);

    public SerialPortEngine(String portName, SerialPortConfig config) throws ECRHubException {
        this.config = config;
        this.serialPort = getSerialPort(portName);
        this.packDecoder = new SerialPortPacketDecoder();
        this.writeQueue = new LinkedBlockingQueue<>();
    }

    private SerialPort getSerialPort(String portName) throws ECRHubException {
        if (StrUtil.isNotBlank(portName)) {
            // Specify the serial port name
            try {
                return SerialPort.getCommPort(portName);
            } catch (Exception e) {
                log.error("The serial port name[{}] is invalid.", portName);
                throw new ECRHubException(e);
            }
        } else {
            // No serial port name specified, automatic search for serial port
            SerialPort port = findSerialPort(config.getPortNameKeyword());
            if (port != null) {
                return port;
            } else {
                throw new ECRHubException("The serial port name cannot be empty.");
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

    public void connect(long startTime, int timeout) throws ECRHubException {
        // Open serial port
        open();

        // Handshake
        handshake(startTime, timeout);

        // Add data listener
        serialPort.removeDataListener();
        serialPort.addDataListener(new ReadListener());

        // Start Write Thread
        if (writeThread == null) {
            writeThread = new Thread(new WriteThread());
            writeThread.start();
        }
    }

    private void open() throws ECRHubException {
        if (!serialPort.isOpen()) {
            serialPort.setComPortParameters(config.getBaudRate(), config.getDataBits(), config.getStopBits(), config.getParity());
            serialPort.setComPortTimeouts(config.getTimeoutMode(), config.getReadTimeout(), config.getWriteTimeout());
            if (serialPort.openPort(5)) {
                log.info("Successful open the serial port name:{}", serialPort.getSystemPortName());
            } else {
                log.error("Failed to open the serial port name:{}", serialPort.getSystemPortName());
                throw new ECRHubException("Failed to open the serial port name:" + serialPort.getSystemPortName());
            }
        }
    }

    private void handshake(long startTime, int timeout) throws ECRHubException {
        while (true) {
            if (doHandshake()) {
                log.info("Handshake successful");
                break;
            } else {
                ThreadUtil.safeSleep(10);
                if (System.currentTimeMillis() - startTime > timeout) {
                    log.error("Handshake failed");
                    throw new ECRHubTimeoutException("Handshake connection timeout");
                }
            }
        }
    }

    private boolean doHandshake() {
        // send handshake packet
        byte[] msg = new SerialPortPacket.HandshakePacket().encode();
        int numWritten = serialPort.writeBytes(msg, msg.length);
        if (numWritten <= 0) {
            return false;
        }

        // read handshake confirm packet
        byte[] buffer = new byte[0];
        for (int i = 0; i < 100; i++) {
            int bytesAvailable = serialPort.bytesAvailable();
            if (bytesAvailable <= 0) {
                ThreadUtil.safeSleep(10);
            } else {
                buffer = new byte[bytesAvailable];
                serialPort.readBytes(buffer, buffer.length);
                break;
            }
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
        // Stop Write Thread
        if (writeThread != null) {
            writeThread.interrupt();
            writeThread = null;
        }
        // Remove Data Listener
        serialPort.removeDataListener();
        // Close Port
        if (serialPort.isOpen()) {
            return serialPort.closePort();
        } else {
            return true;
        }
    }

    public boolean isOpen() {
        return serialPort.isOpen();
    }

    public void safeWrite(byte[] bytes) {
        if (isOpen()) {
            writeQueue.add(bytes);
        }
    }

    public void write(byte[] bytes) throws ECRHubException {
        if (isOpen()) {
            writeQueue.add(bytes);
        } else {
            throw new ECRHubException("The serial port is not opened.");
        }
    }

    public byte[] read(String msgId, long startTime, long timeout) throws ECRHubTimeoutException {
        while (true) {
            String msg = MSG_CACHE.get(msgId);
            if (StrUtil.isNotBlank(msg)) {
                MSG_CACHE.remove(msgId);
                return HexUtil.hex2byte(msg);
            } else {
                ThreadUtil.safeSleep(10);
                if (System.currentTimeMillis() - startTime > timeout) {
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
                    byte[] bytes = writeQueue.take();
                    serialPort.writeBytes(bytes, bytes.length);
                }
            } catch (InterruptedException e) {
                for (byte[] bytes : writeQueue) {
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
                    decodePack(pack);
                } catch (Exception e) {
                    log.warn("Decode packet[{}] error:", pack, e);
                }
            });
        }

        private void decodePack(String hexPack) {
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

        private void putcache(byte[] bytes) {
            if (bytes.length == 0) {
                return;
            }
            ECRHubResponseProto.ECRHubResponse respProto = null;
            try {
                respProto = ECRHubProtobufHelper.unpack(bytes);
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }
            if (respProto != null) {
                MSG_CACHE.put(respProto.getMsgId(), HexUtil.byte2hex(bytes));
            }
        }
    }
}