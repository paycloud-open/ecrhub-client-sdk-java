package com.wiseasy.ecr.hub.sdk.sp.serialport;

import com.wiseasy.ecr.hub.sdk.utils.HexUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @program: ECR-Hub
 * @description:
 * @author: jzj
 * @create: 2023-09-08 12:05
 **/
public class SerialPortPacket {

    private static final Logger log = LoggerFactory.getLogger(SerialPortPacket.class);

    public static final byte PACK_TYPE_COMMON = 0x00;
    public static final byte PACK_TYPE_HANDSHAKE = 0x01;
    public static final byte PACK_TYPE_HANDSHAKE_CONFIRM = 0x02;

    public static final String PACK_HEAD = "55AA";
    public static final String PACK_TAIL = "CC33";

    private static final AtomicInteger counter = new AtomicInteger(0);

    private int maxLength = 1024;//Defines the maximum length of a packet, the maximum length expressed in 2 bytes
    private int starCodeLength = 2;//start symbol
    private int packetTypeLength = 1;//Package type
    private int ackLength = 1;//ack
    private int packetIdLength = 1;//Package id
    private int dataLength = 2;//Package length
    private int headerLength = starCodeLength + packetTypeLength + ackLength + packetIdLength + dataLength;//Protocol header length, length before valid data
    private int checkCodeLength = 1;//check code length
    private int endCodeLength = 2;//end symbol

    protected byte[] head = HexUtil.hex2byte(PACK_HEAD);
    protected byte packType;
    protected byte ack;
    protected byte id;
    protected byte[] dataLen;
    protected byte[] data;
    protected byte checkCode;
    protected byte[] end = HexUtil.hex2byte(PACK_TAIL);

    public SerialPortPacket() {
    }

    public SerialPortPacket(byte packType, byte ack, byte id, byte[] dataLen, byte[] data) {
        this.packType = packType;
        this.ack = ack;
        this.id = id;
        this.dataLen = dataLen;
        this.data = data;
    }

    public byte getAck() {
        return ack;
    }

    public byte getId() {
        return id;
    }

    /**
     * messageId in 1..127
     */
    private static synchronized byte getDataId() {
        int id = counter.incrementAndGet();
        if (id > 255) {
            counter.set(0);
            id = counter.incrementAndGet();
        }
        return (byte) id;
    }

    /**
     * Int type to 2-byte length
     */
    private static byte[] getDataLen(int length) {
        byte[] len = new byte[2];
        len[0] = (byte) ((length >> 8) & 0xFF);
        len[1] = (byte) ((length >> 0) & 0xFF);
        return len;
    }

    /**
     * Get data length
     */
    private byte[] getDataLen(byte[] pack) {
        byte[] dataLen = new byte[dataLength];
        System.arraycopy(pack, starCodeLength + packetTypeLength + ackLength + packetIdLength, dataLen, 0, dataLength);
        return dataLen;
    }

    /**
     * Parse data length
     */
    private int parseLen(byte a, byte b) {
        int rlt = (a & 0xFF) << 8;
        rlt += (b & 0xFF) << 0;
        return rlt;
    }

    /**
     * Get check code
     */
    private byte getCheckCode(byte[] datas) {
        byte temp = datas[0];
        for (int i = 1; i < datas.length; i++) {
            temp = (byte) (temp ^ datas[i]);
        }
        return temp;
    }

    /**
     * Encoding
     *
     * @return
     */
    public byte[] encode() {
        int dataRealLength = data != null ? data.length : 0;
        int checkLength = headerLength - starCodeLength + dataRealLength;
        ByteBuffer bufferCheck = ByteBuffer.allocate(checkLength);
        bufferCheck.put(packType);
        bufferCheck.put(ack);
        bufferCheck.put(id);
        bufferCheck.put(dataLen);
        if (dataRealLength > 0) {
            bufferCheck.put(data);
        }
        this.checkCode = getCheckCode(bufferCheck.array());
        ByteBuffer buffer = ByteBuffer.allocate(starCodeLength + checkLength + checkCodeLength + endCodeLength);
        buffer.put(head);
        buffer.put(bufferCheck.array());
        buffer.put(checkCode);
        buffer.put(end);
        return buffer.array();
    }

    /**
     * Decoding
     *
     * @return
     */
    public SerialPortPacket decode(byte[] pack) {
        int checkDataLen = pack.length - starCodeLength - checkCodeLength - endCodeLength;
        byte[] checkDataBuffer = new byte[checkDataLen];
        System.arraycopy(pack, starCodeLength, checkDataBuffer, 0, checkDataLen);

        this.checkCode = pack[pack.length - 1 - endCodeLength];
        if (getCheckCode(checkDataBuffer) != checkCode) {
            // Faulty calibration packets are not handled
            return null;
        } else {
            this.packType = pack[starCodeLength];
            this.ack = pack[starCodeLength + packetTypeLength];
            this.id = pack[starCodeLength + packetTypeLength + ackLength];
            this.dataLen = getDataLen(pack);
            int dataLen2 = parseLen(dataLen[0], dataLen[1]);
            this.data = new byte[dataLen2];
            if (dataLen2 > 0) {
                System.arraycopy(pack, headerLength, data, 0, dataLen2);
            }
            return this;
        }
    }

    public SerialPortPacket decode(String hexPack) {
        SerialPortPacket pack = null;
        try {
            pack = decode(HexUtil.hex2byte(hexPack));
        } catch (Exception e) {
            log.warn("Decode Hex packet[{}] error:{}", hexPack, e);
        }
        return pack;
    }

    @Override
    public String toString() {
        int dataLength = data != null ? data.length : 0;
        StringBuilder sb = new StringBuilder();
        sb.append("PacketHead: ");
        sb.append(HexUtil.byte2hex(head));
        sb.append("\n");
        sb.append("PacketType: ");
        sb.append(HexUtil.byte2hex(packType));
        sb.append("\n");
        sb.append("       ACK: ");
        sb.append(HexUtil.byte2hex(ack));
        sb.append("\n");
        sb.append("    DataId: ");
        sb.append(HexUtil.byte2hex(id));
        sb.append("\n");
        sb.append("DataLength: ");
        sb.append(dataLength);
        sb.append("\n");
        if (dataLength > 0) {
            sb.append("      Data: ");
            sb.append(HexUtil.byte2hex(data));
            sb.append("\n");
        }
        sb.append(" CheckCode: ");
        sb.append(HexUtil.byte2hex(checkCode));
        sb.append("\n");
        sb.append("PacketTail: ");
        sb.append(HexUtil.byte2hex(end));
        sb.append("\n");
        return sb.toString();
    }

    /**
     * Handshake packet
     */
    public static class HandshakePacket extends SerialPortPacket {
        public HandshakePacket() {
            super(PACK_TYPE_HANDSHAKE, (byte)0x00, (byte)0x00, getDataLen(0), null);
        }
    }

    /**
     * Handshake confirm packet
     */
    public static class HandshakeConfirmPacket extends SerialPortPacket {
        public HandshakeConfirmPacket() {
            super(PACK_TYPE_HANDSHAKE_CONFIRM, (byte)0x00, (byte)0x00, getDataLen(0), null);
        }
    }

    /**
     * HeartBeat packet
     */
    public static class HeartBeatPacket extends SerialPortPacket {
        public HeartBeatPacket() {
            super(PACK_TYPE_COMMON, (byte)0x00, (byte)0x00, getDataLen(0), null);
        }
    }

    /**
     * Message packet
     */
    public static class MsgPacket extends SerialPortPacket {
        public MsgPacket(byte[] data) {
            super(PACK_TYPE_COMMON, (byte)0x00, getDataId(), getDataLen((data != null) ? data.length : 0), data);
        }
    }

    /**
     * ACK packet：The ack field is non-zero, the packet id is 0, and the effective data length is 0
     */
    public static class AckPacket extends SerialPortPacket {
        public AckPacket(byte ack) {
            super(PACK_TYPE_COMMON, ack, (byte)0x00, getDataLen(0), null);
        }
    }
}