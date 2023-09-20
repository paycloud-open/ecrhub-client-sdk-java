package com.wiseasy.ecr.hub.sdk.spi.serialport;

import cn.hutool.core.util.StrUtil;
import com.wiseasy.ecr.hub.sdk.utils.HexUtil;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SerialPortPacketDecoder {

    private final StringBuilder lastRemainingBuffer = new StringBuilder();
    private final Lock lock = new ReentrantLock();

    public Set<String> decode(byte[] bytes) {
        lock.lock();
        try {
            String hexPack = lastRemainingBuffer + HexUtil.byte2hex(bytes);
            lastRemainingBuffer.setLength(0);
            return decode(hexPack, lastRemainingBuffer);
        } finally {
            lock.unlock();
        }
    }

    private Set<String> decode(String pack, StringBuilder lastRemainingBuffer) {
        Set<String> packList = new LinkedHashSet<>();
        while (StrUtil.isNotBlank(pack)) {
            if (pack.startsWith(SerialPortPacket.PACK_HEAD)) {
                // Start with HEAD
                int tailIndex = pack.indexOf(SerialPortPacket.PACK_TAIL);
                if (tailIndex == -1) {
                    lastRemainingBuffer.append(pack);
                    break;
                }
                String validPack = StrUtil.subPre(pack, tailIndex + SerialPortPacket.PACK_TAIL.length());
                packList.add(validPack);
                pack = StrUtil.removePrefix(pack, validPack);
            } else {
                // Not start with HEAD
                int headIndex = pack.indexOf(SerialPortPacket.PACK_HEAD);
                if (headIndex == -1) {
                    break;
                }
                // Remove invalid packet
                String invalidPack = StrUtil.subPre(pack, headIndex);
                pack = StrUtil.removePrefix(pack, invalidPack);
            }
        }
        return packList;
    }
}