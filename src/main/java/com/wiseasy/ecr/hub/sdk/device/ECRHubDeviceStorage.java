package com.wiseasy.ecr.hub.sdk.device;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author wangyuxiang
 * @since 2023-10-20 09:26
 */
public class ECRHubDeviceStorage {

    private static final Logger log = LoggerFactory.getLogger(ECRHubDeviceStorage.class);

    private static class ECRHubDeviceStorageHolder {
        private static final ECRHubDeviceStorage INSTANCE = new ECRHubDeviceStorage();
    }

    public static ECRHubDeviceStorage getInstance() {
        return ECRHubDeviceStorageHolder.INSTANCE;
    }

    private final File storageFile;

    private ECRHubDeviceConfig config;


    private void readConfig() {
        String json = FileUtil.readString(storageFile, StandardCharsets.UTF_8);
        if (!JSON.isValid(json)) {
            config = new ECRHubDeviceConfig();
            FileUtil.writeUtf8String(config.toString(), storageFile);
            return;
        }
        log.debug("content: {}", json);
        try {
            config = JSONObject.parseObject(json).toJavaObject(ECRHubDeviceConfig.class);
        } catch (Exception e) {
            log.error("config parse error", e);
            FileUtil.writeUtf8String("{}", storageFile);
            config = new ECRHubDeviceConfig();
        }
    }


    public void addPairedDevice(String device) {
        config.getPairedDevice().add(device);
        update();
    }

    public List<String> queryPairedDevice() {
        return new ArrayList<>(this.config.getPairedDevice());
    }

    public void removePairedDevice(String device) {
        config.getPairedDevice().remove(device);
        update();
    }

    public void update() {
        FileUtil.writeUtf8String(config.toString(), storageFile);
    }

    private ECRHubDeviceStorage() {
        String path = FileUtil.getUserHomePath() + File.separator + ".ecr-hub-client.json";
        storageFile = new File(path);
        if (!storageFile.exists()) {
            try {
                boolean success = storageFile.createNewFile();
                if (success) {
                    log.debug("File created successfully, path: {}", path);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        readConfig();
    }

    public static class ECRHubDeviceConfig {

        Set<String> pairedDevice = new HashSet<>();

        public Set<String> getPairedDevice() {
            return pairedDevice;
        }


        @Override
        public String toString() {
            return JSONObject.toJSONString(this);
        }

    }

}
