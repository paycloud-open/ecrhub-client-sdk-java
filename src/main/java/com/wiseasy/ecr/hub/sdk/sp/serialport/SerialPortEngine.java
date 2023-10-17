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
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

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

    private final Queue<SerialPortPacket> writeQueue;
    private final Map<Byte, Integer> writeMap;
    private Thread writeThread;

    private final FIFOCache<String, String> MSG_CACHE = new FIFOCache<>(20, 10 * 60 * 1000);

    public SerialPortEngine(String portName, SerialPortConfig config) throws ECRHubException {
        this.config = config;
        this.serialPort = getSerialPort(portName);
        this.packDecoder = new SerialPortPacketDecoder();
        this.writeQueue = new ConcurrentLinkedQueue<>();
        this.writeMap = new ConcurrentHashMap<>();
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
            log.info("Start opening the serial port name:{}", serialPort.getSystemPortName());
            if (serialPort.openPort(5)) {
                log.info("Successful open the serial port name:{}", serialPort.getSystemPortName());
            } else {
                log.error("Failed to open the serial port name:{}", serialPort.getSystemPortName());
                throw new ECRHubException("Failed to open the serial port name:" + serialPort.getSystemPortName());
            }
        }
    }

    private void handshake(long startTime, int timeout) throws ECRHubException {
        log.info("Start handshake connection...");
        while (true) {
            if (doHandshake()) {
                log.info("Handshake connection successful");
                break;
            } else {
                ThreadUtil.safeSleep(10);
                if (System.currentTimeMillis() - startTime > timeout) {
                    log.error("Handshake connection failed");
                    throw new ECRHubTimeoutException("Handshake connection timeout");
                }
            }
        }
    }

    private boolean doHandshake() {
        // send handshake packet
        SerialPortPacket handshakePack = new SerialPortPacket.HandshakePacket();
        if (!write(handshakePack)) {
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

    public void addQueue(SerialPortPacket pack) throws ECRHubException {
        if (isOpen()) {
            writeQueue.add(pack);
        } else {
            throw new ECRHubException("The serial port is not opened.");
        }
    }

    public boolean write(SerialPortPacket pack) {
        if (!isOpen()) {
            return false;
        } else {
            byte[] msg = pack.encode();
            int numWritten = serialPort.writeBytes(msg, msg.length);
            return (numWritten > 0);
        }
    }

    public byte[] read(String msgId, long startTime, long timeout) throws ECRHubTimeoutException {
        byte[] bytes = null;
        while (isOpen()) {
            String msg = MSG_CACHE.get(msgId);
            if (StrUtil.isNotBlank(msg)) {
                MSG_CACHE.remove(msgId);
                bytes = HexUtil.hex2byte(msg);
                break;
            } else {
                ThreadUtil.safeSleep(10);
                if (System.currentTimeMillis() - startTime > timeout) {
                    throw new ECRHubTimeoutException();
                }
            }
        }
        return bytes;
    }

    private class WriteThread implements Runnable {
        @Override
        public void run() {
            while (isOpen()) {
                ThreadUtil.safeSleep(50);
                SerialPortPacket pack = writeQueue.peek();
                if (pack == null || pack.id == 0x00) {
                    continue;
                } else {
                    write(pack);
                }
                if (!checkRetryTimes(pack.id)) {
                    writeQueue.poll();
                }
            }
        }

        private boolean checkRetryTimes(byte id) {
            Integer times = writeMap.get(id);
            int retryTimes = times == null ? 1 : times + 1;
            if (retryTimes <= 100) {
                writeMap.put(id, retryTimes);
                return true;
            } else {
                writeMap.remove(id);
                return false;
            }
        }
    }

    private class ReadListener implements SerialPortDataListener {

        private volatile byte lastReceivedDataId = 0x00;

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
                log.debug("Received ack packet:\n{}", pack);
                // Remove this message from the message queue
                poll(ack);
            }
            // Common packet
            byte id = pack.getId();
            if (id != 0x00) {
                log.debug("Received data packet:\n{}", pack);
                // Send data ACK packet
                ack(id);
                // Check if the message Id is duplicated
                if (lastReceivedDataId != id) {
                    // Last received data id
                    lastReceivedDataId = id;
                    // Cache data
                    putcache(pack.getData());
                }
            }
        }

        private void poll(byte ack) {
            // Clear Sent Times
            writeMap.remove(ack);
            // Remove this message from the message queue
            if (!writeQueue.isEmpty() && writeQueue.peek().id == ack) {
                writeQueue.poll();
            }
        }

        private void ack(byte id) {
            SerialPortPacket pack = new SerialPortPacket.AckPacket(id);
            log.debug("Send ack packet:\n{}", pack);
            write(pack);
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