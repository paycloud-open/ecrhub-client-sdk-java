package com.wiseasy.ecr.hub.sdk.sp.serialport;

import cn.hutool.core.util.StrUtil;
import com.wiseasy.ecr.hub.sdk.utils.HexUtil;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SerialPortMessageDecoder {

    private final StringBuilder lastRemainingBuffer = new StringBuilder();
    private final Lock lock = new ReentrantLock();

    public Set<String> decode(byte[] bytes) {
        lock.lock();
        try {
            String hexMessage = lastRemainingBuffer + HexUtil.byte2hex(bytes);
            lastRemainingBuffer.setLength(0);
            return decode(hexMessage, lastRemainingBuffer);
        } finally {
            lock.unlock();
        }
    }

    private Set<String> decode(String messagee, StringBuilder lastRemainingBuffer) {
        Set<String> messageList = new LinkedHashSet<>();
        while (StrUtil.isNotBlank(messagee)) {
            if (messagee.startsWith(SerialPortMessage.MESSAGE_STX)) {
                // Start with HEAD
                int tailIndex = messagee.indexOf(SerialPortMessage.MESSAGE_ETX);
                if (tailIndex == -1) {
                    lastRemainingBuffer.append(messagee);
                    break;
                }
                String validMessage = StrUtil.subPre(messagee, tailIndex + SerialPortMessage.MESSAGE_ETX.length());
                messageList.add(validMessage);
                messagee = StrUtil.removePrefix(messagee, validMessage);
            } else {
                // Not start with HEAD
                int headIndex = messagee.indexOf(SerialPortMessage.MESSAGE_STX);
                if (headIndex == -1) {
                    break;
                }
                // Remove invalid message
                String invalidMessage = StrUtil.subPre(messagee, headIndex);
                messagee = StrUtil.removePrefix(messagee, invalidMessage);
            }
        }
        return messageList;
    }
}