package com.wiseasy.ecr.hub.sdk.spi.serialport;


import com.wiseasy.ecr.hub.sdk.utils.HexUtil;

import java.nio.ByteBuffer;

/**
 * @program: ECR-Hub
 * @description:
 * @author: jzj
 * @create: 2023-09-08 12:05
 **/
public class SerialPortPackage {

    public static final byte PACKAGETYPE_COMMON = 0x00;
    public static final byte PACKAGETYPE_HANDSHAKE = 0x01;
    public static final byte PACKAGETYPE_HANDSHAKE_CONFIRM = 0x02;

    public static final String PACK_HEAD = "55AA";
    public static final String PACK_TAIL = "CC33";

    private int maxLength = 1024;//定义一个包的最大长度，2个字节表示的最大长度
    private int starCodeLength = 2;//起始符
    private int packageTypeLength = 1;//包类型
    private int ackLength = 1;//ack
    private int packageIdLength = 1;//包id
    private int dataLength = 2;//包长度
    private int headerLength = starCodeLength + packageTypeLength + ackLength + packageIdLength + dataLength;//协议头长度，有效数据之前的长度
    private int checkCodeLength = 1;//校验和长度1
    private int endCodeLength = 2;//终止符

    private static volatile int msgId; //消息id in 1..255

    protected byte[] head = HexUtil.hex2byte(PACK_HEAD);
    private byte packageType;
    protected byte ack;
    protected byte id;
    protected byte[] dataLen;
    protected byte[] data;
    protected byte checkCode;
    protected byte[] end = HexUtil.hex2byte(PACK_TAIL);

    public SerialPortPackage() {
    }

    public SerialPortPackage(byte packageType, byte ack, byte id, byte[] dataLen, byte[] data) {
        this.packageType = packageType;
        this.ack = ack;
        this.id = id;
        this.dataLen = dataLen;
        this.data = data;
    }

    public byte getPackageType() {
        return packageType;
    }

    public byte getAck() {
        return ack;
    }

    public byte getId() {
        return id;
    }

    public byte[] getData() {
        return data;
    }

    /**
     * msgId in 1..255
     */
    private static byte getMsgId() {
        if (++msgId > 255) {
            msgId = 1;
        }
        return (byte)msgId;
    }

    /**
     * Int 类型转2字节长度
     */
    private static byte[] getLen(int length) {
         byte[] len = new byte[2];
         len[0] = (byte) ((length >> 8) & 0xFF);
         len[1] = (byte) ((length >> 0) & 0xFF);
         return len;
     }

    /**
     * 校验和
     */
     private byte getCheckCode(byte[] datas) {
         byte temp = datas[0];
         for (int i = 1; i < datas.length; i++) {
            temp = (byte) (temp ^ datas[i]);
         }
         return temp;
     }

    /**
     * 获取数据长度
     */
    private byte[] getDataLen(byte[] pack) {
        byte[] data = new byte[dataLength];
        System.arraycopy(pack, starCodeLength + packageTypeLength + ackLength + packageIdLength, data, 0, dataLength);
        return data;
    }

    /**
     * 获取数据长度
     */
    private int parseLen(byte a, byte b) {
        int rlt = (a & 0xFF) << 8;
        rlt += (b & 0xFF) << 0;
        return rlt;
    }

    /**
     * 组包
     * @return
     */
    public byte[] encode() {
        int dataRealLength = data != null ? data.length : 0;
        int checkLength = headerLength - starCodeLength + dataRealLength;
        ByteBuffer bufferCheck = ByteBuffer.allocate(checkLength);
        bufferCheck.put(packageType);
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
     * 拆包
     * @return
     */
    public SerialPortPackage decode(byte[] pack) {
        int checkDataLen = pack.length - starCodeLength - checkCodeLength - endCodeLength;
        byte[] checkDataBuffer = new byte[checkDataLen];
        System.arraycopy(pack, starCodeLength, checkDataBuffer, 0, checkDataLen);

        this.checkCode = pack[pack.length - 1 - endCodeLength];
        if (getCheckCode(checkDataBuffer) != checkCode) {
            // 校验包有问题不做处理
        } else {
            // 校验包没有问题
            this.packageType = pack[starCodeLength];
            this.ack = pack[starCodeLength + packageTypeLength];
            this.id = pack[starCodeLength + packageTypeLength + ackLength];
            this.dataLen = getDataLen(pack);
            int dataLen2 = parseLen(dataLen[0], dataLen[1]);
            this.data = new byte[dataLen2];
            if (dataLen2 > 0) {
                System.arraycopy(pack, headerLength, data, 0, dataLen2);
            }
        }

        return this;
    }

    /**
     * 握手包
     */
    public static class HandshakePackage extends SerialPortPackage {
        public HandshakePackage() {
            super((byte)0x01, (byte)0x00, (byte)0x00, getLen(0), null);
        }
    }

    /**
     * 握手确认包
     */
    public static class HandshakeConfirmPackage extends SerialPortPackage {
        public HandshakeConfirmPackage() {
            super((byte)0x02, (byte)0x00, (byte)0x00, getLen(0), null);
        }
    }

    /**
     * 心跳包
     */
    public static class HeartBeatPackage extends SerialPortPackage {
        public HeartBeatPackage() {
            super((byte)0x00, (byte)0x00, (byte)0x00, getLen(0), null);
        }
    }

    /**
     * 普通x消息包
     */
    public static class MsgPackage extends SerialPortPackage {
        public MsgPackage(byte[] data) {
            super((byte)0x00, (byte)0x00, getMsgId(), getLen((data != null) ? data.length : 0), data);
        }
    }

    /**
     * acK包：ack字段非0，数据包id为0，有效数据长度为0
     */
    public static class AckPackage extends SerialPortPackage {
        public AckPackage(byte ack) {
            super((byte)0x00, ack, (byte)0x00, getLen(0), null);
        }
    }
}