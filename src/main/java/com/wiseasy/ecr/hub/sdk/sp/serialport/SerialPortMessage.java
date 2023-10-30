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
public class SerialPortMessage {

    private static final Logger log = LoggerFactory.getLogger(SerialPortMessage.class);

    public static final byte MESSAGE_TYPE_COMMON = 0x00;
    public static final byte MESSAGE_TYPE_HANDSHAKE = 0x01;
    public static final byte MESSAGE_TYPE_HANDSHAKE_CONFIRM = 0x02;

    public static final String MESSAGE_STX = "55AA";
    public static final String MESSAGE_ETX = "CC33";
    public static final byte MESSAGE_STX1 = HexUtil.hex2byte("55")[0];
    public static final byte MESSAGE_STX2 = HexUtil.hex2byte("AA")[0];

    public static final int MESSAGE_STX_LENGTH = 2;//Message start text length
    public static final int MESSAGE_TYPE_LENGTH = 1;//Message type length
    public static final int MESSAGE_ACK_LENGTH = 1;//Message ack length
    public static final int MESSAGE_ID_LENGTH = 1;//Message id length
    public static final int MESSAGE_DATA_LENGTH = 2;//Message data length
    public static final int MESSAGE_CRC_LENGTH = 1;//Message CRC length
    public static final int MESSAGE_ETX_LENGTH = 2;//Message end text length
    public static final int MESSAGE_HEADER_LENGTH = MESSAGE_STX_LENGTH + MESSAGE_TYPE_LENGTH + MESSAGE_ACK_LENGTH + MESSAGE_ID_LENGTH + MESSAGE_DATA_LENGTH;//Protocol header length, length before valid data

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    protected byte[] messageStx = HexUtil.hex2byte(MESSAGE_STX);
    protected byte messageType;
    protected byte messageAck;
    protected byte messageId;
    protected byte[] messageDataLen;
    protected byte[] messageData;
    protected byte messageCrc;
    protected byte[] messageEtx = HexUtil.hex2byte(MESSAGE_ETX);
    protected String hexMessage;

    public SerialPortMessage() {}

    public SerialPortMessage(byte messageType, byte messageAck, byte messageId, byte[] messageDataLen, byte[] messageData) {
        this.messageType = messageType;
        this.messageAck = messageAck;
        this.messageId = messageId;
        this.messageDataLen = messageDataLen;
        this.messageData = messageData;
    }

    public byte getMessageType() {
        return messageType;
    }

    public byte getMessageAck() {
        return messageAck;
    }

    public byte getMessageId() {
        return messageId;
    }

    public byte[] getMessageData() {
        return messageData;
    }

    public String getHexMessage() {
        return hexMessage;
    }

    /**
     * messageId in 1..255
     */
    synchronized private static byte getDataId() {
        int id = COUNTER.incrementAndGet();
        if (id > 255) {
            COUNTER.set(0);
            id = COUNTER.incrementAndGet();
        }
        return (byte) id;
    }

    public static byte[] getDataLen(int length) {
        byte[] len = new byte[2];
        len[0] = (byte) ((length >> 8) & 0xFF);
        len[1] = (byte) ((length >> 0) & 0xFF);
        return len;
    }

    public static byte[] getDataLen(byte[] pack) {
        byte[] dataLen = new byte[MESSAGE_DATA_LENGTH];
        System.arraycopy(pack, MESSAGE_STX_LENGTH + MESSAGE_TYPE_LENGTH + MESSAGE_ACK_LENGTH + MESSAGE_ID_LENGTH, dataLen, 0, MESSAGE_DATA_LENGTH);
        return dataLen;
    }

    public static int parseDataLen(byte a, byte b) {
        int rlt = (a & 0xFF) << 8;
        rlt += (b & 0xFF) << 0;
        return rlt;
    }

    public static byte getCRC(byte[] bytes) {
        byte temp = bytes[0];
        for (int i = 1; i < bytes.length; i++) {
            temp = (byte) (temp ^ bytes[i]);
        }
        return temp;
    }

    public byte[] encode() {
        int dataRealLength = messageData != null ? messageData.length : 0;
        int checkLength = MESSAGE_HEADER_LENGTH - MESSAGE_STX_LENGTH + dataRealLength;
        ByteBuffer checkBuffer = ByteBuffer.allocate(checkLength);
        checkBuffer.put(messageType);
        checkBuffer.put(messageAck);
        checkBuffer.put(messageId);
        checkBuffer.put(messageDataLen);
        if (dataRealLength > 0) {
            checkBuffer.put(messageData);
        }

        this.messageCrc = getCRC(checkBuffer.array());

        ByteBuffer buffer = ByteBuffer.allocate(MESSAGE_STX_LENGTH + checkLength + MESSAGE_CRC_LENGTH + MESSAGE_ETX_LENGTH);
        buffer.put(messageStx);
        buffer.put(checkBuffer.array());
        buffer.put(messageCrc);
        buffer.put(messageEtx);
        byte[] message = buffer.array();

        this.hexMessage = HexUtil.byte2hex(message);
        return message;
    }

    public SerialPortMessage decode(byte[] pack) {
        this.hexMessage = HexUtil.byte2hex(pack);

        int checkDataLen = pack.length - MESSAGE_STX_LENGTH - MESSAGE_CRC_LENGTH - MESSAGE_ETX_LENGTH;
        byte[] checkDataBuffer = new byte[checkDataLen];
        System.arraycopy(pack, MESSAGE_STX_LENGTH, checkDataBuffer, 0, checkDataLen);

        this.messageCrc = pack[pack.length - 1 - MESSAGE_ETX_LENGTH];
        if (getCRC(checkDataBuffer) != messageCrc) {
            // Faulty calibration packets are not handled
            return null;
        } else {
            this.messageType = pack[MESSAGE_STX_LENGTH];
            this.messageAck = pack[MESSAGE_STX_LENGTH + MESSAGE_TYPE_LENGTH];
            this.messageId = pack[MESSAGE_STX_LENGTH + MESSAGE_TYPE_LENGTH + MESSAGE_ACK_LENGTH];
            this.messageDataLen = getDataLen(pack);
            int dataLenInt = parseDataLen(messageDataLen[0], messageDataLen[1]);
            this.messageData = new byte[dataLenInt];
            if (dataLenInt > 0) {
                System.arraycopy(pack, MESSAGE_HEADER_LENGTH, messageData, 0, dataLenInt);
            }
            return this;
        }
    }

    @Override
    public String toString() {
        int dataRealLength = messageData != null ? messageData.length : 0;
        StringBuilder builder = new StringBuilder();
        builder.append("Message STX: ");
        builder.append(HexUtil.byte2hex(messageStx));
        builder.append("\n");
        builder.append("Message Type: ");
        builder.append(HexUtil.byte2hex(messageType));
        builder.append("\n");
        builder.append("Message ACK: ");
        builder.append(HexUtil.byte2hex(messageAck));
        builder.append("\n");
        builder.append("Message Id: ");
        builder.append(HexUtil.byte2hex(messageId));
        builder.append("\n");
        builder.append("Message Data Length: ");
        builder.append(dataRealLength);
        builder.append("\n");
        if (dataRealLength > 0) {
            builder.append("Message Data: ");
            builder.append(HexUtil.byte2hex(messageData));
            builder.append("\n");
        }
        builder.append("Message CRC: ");
        builder.append(HexUtil.byte2hex(messageCrc));
        builder.append("\n");
        builder.append("Message ETX: ");
        builder.append(HexUtil.byte2hex(messageEtx));
        return builder.toString();
    }

    /**
     * Handshake message
     */
    public static class HandshakeMessage extends SerialPortMessage {
        public HandshakeMessage() {
            super(MESSAGE_TYPE_HANDSHAKE, (byte)0x00, (byte)0x00, getDataLen(0), null);
        }
    }

    /**
     * Handshake confirm message
     */
    public static class HandshakeConfirmMessage extends SerialPortMessage {
        public HandshakeConfirmMessage() {
            super(MESSAGE_TYPE_HANDSHAKE_CONFIRM, (byte)0x00, (byte)0x00, getDataLen(0), null);
        }
    }

    /**
     * Heartbeat message
     */
    public static class HeartbeatMessage extends SerialPortMessage {
        public HeartbeatMessage() {
            super(MESSAGE_TYPE_COMMON, (byte)0x00, (byte)0x00, getDataLen(0), null);
        }
    }

    /**
     * Data message
     */
    public static class DataMessage extends SerialPortMessage {
        public DataMessage(byte[] data) {
            super(MESSAGE_TYPE_COMMON, (byte)0x00, getDataId(), getDataLen(data != null ? data.length : 0), data);
        }
    }

    /**
     * ACK message：The ack field is non-zero, the packet id is 0, and the effective data length is 0
     */
    public static class AckMessage extends SerialPortMessage {
        public AckMessage(byte ack) {
            super(MESSAGE_TYPE_COMMON, ack, (byte)0x00, getDataLen(0), null);
        }
    }
}