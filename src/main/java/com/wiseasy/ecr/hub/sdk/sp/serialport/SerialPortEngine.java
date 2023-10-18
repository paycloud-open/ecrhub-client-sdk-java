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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    private final Map<Byte, Integer> writeMap;
    private volatile boolean handshakeConfirm;
    private Thread heartBeatThread;

    public SerialPortEngine(String portName, SerialPortConfig config) throws ECRHubException {
        this.config = config;
        this.serialPort = getSerialPort(portName);
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
                throw new ECRHubException("Not found available serial port.");
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
        serialPort.addDataListener(new ReadListener());

        // Handshake
        handshake(startTime);

        // Start HeartBeat
        if (heartBeatThread == null) {
            heartBeatThread = new Thread(new HeartBeatThread());
            heartBeatThread.start();
        }
    }

    private void open() throws ECRHubException {
        if (!serialPort.isOpen()) {
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
        byte[] buffer = new SerialPortPacket.HandshakePacket().encode();
        handshakeConfirm = false;
        while (true) {
            if (doHandshake(buffer)) {
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

    private boolean doHandshake(byte[] buffer) {
        // send handshake packet
        if (!write(buffer)) {
            return false;
        }
        // read handshake confirm packet
        long startTime = System.currentTimeMillis();
        while (true) {
            if (handshakeConfirm) {
                return true;
            } else {
                ThreadUtil.safeSleep(10);
                if (System.currentTimeMillis() - startTime > 1000) {
                    return false;
                }
            }
        }
    }

    public boolean close() {
        if (!serialPort.isOpen()) {
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

    public boolean write(byte[] buffer) {
        if (isOpen()) {
            int numWritten = serialPort.writeBytes(buffer, buffer.length);
            return numWritten > 0;
        } else {
            return false;
        }
    }

    public void write(byte[] buffer, long startTime, long timeout) throws ECRHubException {
        SerialPortPacket pack = new SerialPortPacket.MsgPacket(buffer);
        byte[] bytes = pack.encode();
        log.debug("Send data packet:\n{}", pack);

        writeMap.put(pack.id, 0);

        while (isOpen() && writeMap.containsKey(pack.id)) {
            write(bytes);
            ThreadUtil.safeSleep(100);
            if (System.currentTimeMillis() - startTime > timeout) {
                writeMap.remove(pack.id);
                throw new ECRHubTimeoutException("Write timeout");
            }
        }
    }

    public byte[] read(String msgId, long startTime, long timeout) throws ECRHubTimeoutException {
        byte[] buffer = new byte[0];
        while (isOpen()) {
            byte[] msg = MSG_CACHE.get(msgId);
            if (ArrayUtil.isNotEmpty(msg)) {
                MSG_CACHE.remove(msgId);
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

    private class HeartBeatThread implements Runnable {
        @Override
        public void run() {
            byte[] buffer = new SerialPortPacket.HeartBeatPacket().encode();
            while (isOpen()) {
                write(buffer);
                ThreadUtil.safeSleep(1000);
            }
        }
    }

    private class ReadListener implements SerialPortDataListener {

        private volatile byte lastReceivedDataId = 0x00;
        private final SerialPortPacketDecoder packDecoder = new SerialPortPacketDecoder();

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
            packDecoder.decode(buffer).forEach(hexPack -> {
                SerialPortPacket pack = new SerialPortPacket().decode(hexPack);
                if (pack != null) {
                    handlePack(pack);
                }
            });
        }

        private void handlePack(SerialPortPacket pack) {
            switch (pack.packType) {
                case SerialPortPacket.PACK_TYPE_HANDSHAKE_CONFIRM:
                    // Handshake confirm packet
                    handshakeConfirm = true;
                    break;
                case SerialPortPacket.PACK_TYPE_COMMON:
                    // Common packet
                    handleCommonPack(pack);
                    break;
                default:
                    // Other packet, do nothing
                    break;
            }
        }

        private void handleCommonPack(SerialPortPacket pack) {
            // ACK packet
            byte ack = pack.getAck();
            if (0x00 != ack) {
                log.debug("Received ack packet:\n{}", pack);
                // Remove Sent Times
                writeMap.remove(ack);
            }
            // Common packet
            byte id = pack.getId();
            if (0x00 != id) {
                log.debug("Received data packet:\n{}", pack);
                // Send data ACK packet
                sendAck(id);
                // Cache data
                if (lastReceivedDataId != id) {
                    lastReceivedDataId = id;
                    putCache(pack.data);
                }
            }
        }

        private void sendAck(byte id) {
            SerialPortPacket pack = new SerialPortPacket.AckPacket(id);
            log.debug("Send ack packet:\n{}", pack);
            write(pack.encode());
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
            if (respProto != null) {
                MSG_CACHE.put(respProto.getMsgId(), data);
            }
        }
    }
}