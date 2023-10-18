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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
    private final Map<Byte, Integer> writeMap;
    private Thread heartBeatThread;

    private final FIFOCache<String, String> MSG_CACHE = new FIFOCache<>(20, 10 * 60 * 1000);

    public SerialPortEngine(String portName, SerialPortConfig config) throws ECRHubException {
        this.config = config;
        this.serialPort = getSerialPort(portName);
        this.packDecoder = new SerialPortPacketDecoder();
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

    public void connect(long startTime) throws ECRHubException {
        // Open serial port
        open();

        // Handshake
        handshake(startTime);

        // Add data listener
        serialPort.removeDataListener();
        serialPort.addDataListener(new ReadListener());

        // Start HeartBeat
        if (heartBeatThread == null) {
            heartBeatThread = new Thread(new HeartBeatThread());
            heartBeatThread.start();
        }
    }

    private void open() throws ECRHubException {
        if (!isOpen()) {
            String portName = serialPort.getSystemPortName();
            log.info("Start opening the serial port name:{}", portName);

            serialPort.setComPortParameters(config.getBaudRate(), config.getDataBits(), config.getStopBits(), config.getParity());
            serialPort.setComPortTimeouts(config.getTimeoutMode(), config.getReadTimeout(), config.getWriteTimeout());
            boolean opened = serialPort.openPort(5);
            if (!opened) {
                log.error("Failed to open the serial port name:{}", portName);
                throw new ECRHubException("Failed to open the serial port name:" + portName);
            }

            log.info("Successful open the serial port name:{}", portName);
        }
    }

    private void handshake(long startTime) throws ECRHubException {
        log.info("Start handshake connection...");
        while (true) {
            if (doHandshake()) {
                log.info("Handshake connection successful");
                break;
            } else {
                ThreadUtil.safeSleep(10);
                if (System.currentTimeMillis() - startTime > config.getConnTimeout()) {
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
        byte[] bytes = null;
        for (int i = 0; i < 100; i++) {
            bytes = read();
            if (ArrayUtil.isNotEmpty(bytes)) {
                break;
            } else {
                ThreadUtil.safeSleep(10);
            }
        }
        // decode handshake confirm packet
        if (ArrayUtil.isNotEmpty(bytes)) {
            for (String hexPack : packDecoder.decode(bytes)) {
                SerialPortPacket pack = new SerialPortPacket().decode(hexPack);
                if (pack != null && pack.getPackType() == SerialPortPacket.PACK_TYPE_HANDSHAKE_CONFIRM) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean close() {
        if (!isOpen()) {
            return true;
        } else {
            if (heartBeatThread != null) {
                heartBeatThread.interrupt();
                heartBeatThread = null;
            }
            if (writeMap != null) {
                writeMap.clear();
            }
            return serialPort.closePort();
        }
    }

    public boolean isOpen() {
        return serialPort.isOpen();
    }

    public boolean write(SerialPortPacket pack) {
        if (isOpen()) {
            byte[] bytes = pack.encode();
            int numWritten = serialPort.writeBytes(bytes, bytes.length);
            return numWritten > 0;
        } else {
            return false;
        }
    }

    public void write(SerialPortPacket pack, long startTime, long timeout) throws ECRHubException {
        writeMap.put(pack.id, 1);
        while (isOpen()) {
            if (!writeMap.containsKey(pack.id)) {
                break;
            }
            write(pack);
            ThreadUtil.safeSleep(100);
            if (System.currentTimeMillis() - startTime > timeout) {
                writeMap.remove(pack.id);
                throw new ECRHubTimeoutException("Write timeout");
            }
        }
    }

    public byte[] read() {
        byte[] bytes = null;
        if (isOpen()) {
            int bytesAvailable = serialPort.bytesAvailable();
            if (bytesAvailable > 0) {
                bytes = new byte[bytesAvailable];
                serialPort.readBytes(bytes, bytes.length);
            }
        }
        return bytes;
    }

    public byte[] read(String msgId, long startTime, long timeout) throws ECRHubTimeoutException {
        while (true) {
            String msg = MSG_CACHE.get(msgId);
            if (StrUtil.isNotBlank(msg)) {
                MSG_CACHE.remove(msgId);
                return HexUtil.hex2byte(msg);
            } else {
                ThreadUtil.safeSleep(20);
                if (System.currentTimeMillis() - startTime > timeout) {
                    throw new ECRHubTimeoutException("Read timeout");
                }
            }
        }
    }

    private class HeartBeatThread implements Runnable {
        @Override
        public void run() {
            SerialPortPacket pack = new SerialPortPacket.HeartBeatPacket();
            while (isOpen()) {
                write(pack);
                ThreadUtil.safeSleep(1000);
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
                // Remove Sent Times
                writeMap.remove(ack);
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