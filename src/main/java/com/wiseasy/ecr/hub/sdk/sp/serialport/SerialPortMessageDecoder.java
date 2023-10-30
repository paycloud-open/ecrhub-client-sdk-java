package com.wiseasy.ecr.hub.sdk.sp.serialport;

import cn.hutool.core.util.ArrayUtil;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SerialPortMessageDecoder {

    private final Lock lock = new ReentrantLock();

    private final SerialPortMessageHandler messageHandler;
    private byte[] lastRemainingBuffer;

    public SerialPortMessageDecoder(SerialPortMessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    public void decode(byte[] bytes) {
        lock.lock();
        try {
            byte[] buffer = ArrayUtil.addAll(lastRemainingBuffer, bytes);
            lastRemainingBuffer = null;
            doDecode(buffer);
        } finally {
            lock.unlock();
        }
    }

    private void doDecode(byte[] buffer) {
        int currentLength = buffer.length;
        int cursor = 0;
        while (currentLength >= SerialPortMessage.MESSAGE_HEADER_LENGTH) {
            // Retrieve the header start character, and the data before the start character will be discarded
            if (!(buffer[cursor] == SerialPortMessage.MESSAGE_STX1 && buffer[cursor + 1] == SerialPortMessage.MESSAGE_STX2)) {
                --currentLength;
                ++cursor;
                continue;
            }

            // If the current length is less than the length of the entire packet, the loop continues to receive data
            int len = SerialPortMessage.MESSAGE_STX_LENGTH + SerialPortMessage.MESSAGE_TYPE_LENGTH + SerialPortMessage.MESSAGE_ACK_LENGTH + SerialPortMessage.MESSAGE_ID_LENGTH;
            int dataLength = SerialPortMessage.parseDataLen(buffer[cursor + len], buffer[cursor + len + 1]);
            int packLength = SerialPortMessage.MESSAGE_HEADER_LENGTH + dataLength + SerialPortMessage.MESSAGE_CRC_LENGTH + SerialPortMessage.MESSAGE_ETX_LENGTH;
            if (currentLength < packLength) {
                break;
            }

            // Handle valid packet
            byte[] pack = new byte[packLength];
            System.arraycopy(buffer, cursor, pack, 0, packLength);
            messageHandler.handle(pack);

            // The cursor moves to the end of the packet and the length is the remaining data length
            currentLength -= packLength;
            cursor += packLength;
        }

        // The remaining bytes are put into the buffer
        if (currentLength > 0 && cursor > 0) {
            lastRemainingBuffer = new byte[currentLength];
            System.arraycopy(buffer, cursor, lastRemainingBuffer, 0, currentLength);
        }
    }
}