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
        int currentLen = buffer.length;
        int cursor = 0;

        // If the currently received packet is larger than the header length, parse the current packet
        while (currentLen >= SerialPortMessage.MESSAGE_HEADER_LENGTH) {
            // Read the header start character, any data before the start character will be discarded
            if (!(buffer[cursor] == SerialPortMessage.MESSAGE_STX1 && buffer[cursor + 1] == SerialPortMessage.MESSAGE_STX2)) {
                --currentLen;
                ++cursor;
                continue;
            }

            // Parse the length of valid data
            int dataLenIndex = SerialPortMessage.MESSAGE_DATA_LENGTH_INDEX;
            int dataLen = SerialPortMessage.parseDataLen(buffer[cursor + dataLenIndex], buffer[cursor + dataLenIndex + 1]);

            // Calculate the total packet length
            int packLen = SerialPortMessage.MESSAGE_HEADER_LENGTH + dataLen +
                          SerialPortMessage.MESSAGE_CRC_LENGTH + SerialPortMessage.MESSAGE_ETX_LENGTH;

            // If the current length of the packet is less than the length of the whole packet,
            // jump out of the loop and continue to receive data.
            if (currentLen < packLen) {
                break;
            }

            // Handle packet
            byte[] pack = new byte[packLen];
            System.arraycopy(buffer, cursor, pack, 0, packLen);
            messageHandler.handle(pack);

            // The cursor moves to the end of the packet with the length of the remaining data
            currentLen -= packLen;
            cursor += packLen;
        }

        // The remaining bytes are put into the buffer
        if (currentLen > 0 && cursor > 0) {
            lastRemainingBuffer = new byte[currentLen];
            System.arraycopy(buffer, cursor, lastRemainingBuffer, 0, currentLen);
        }
    }
}