package com.wiseasy.ecr.hub.sdk.model.request;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.annotation.JSONField;
import com.wiseasy.ecr.hub.sdk.ECRHubConfig;
import com.wiseasy.ecr.hub.sdk.model.response.ECRHubResponse;

public abstract class ECRHubRequest<T extends ECRHubResponse> {

    /**
     * Message unique ID, return as is
     */
    @JSONField(name = "msgId")
    private String msg_id;
    /**
     * Version number, temporarily fixed: 1.0
     */
    @JSONField(name = "version")
    private String version = "1.0";
    /**
     * Voice data object
     */
    @JSONField(name = "voiceData")
    private VoiceData voice_data = new VoiceData();
    /**
     * Printer data object
     */
    @JSONField(name = "printerData")
    private PrinterData printer_data = new PrinterData();
    /**
     * Notify data object
     */
    @JSONField(name = "notifyData")
    private NotifyData notify_data = new NotifyData();

    private ECRHubConfig config;

    public Class<T> getResponseClass() {
        return (Class<T>) ClassUtil.getTypeArgument(getClass());
    }

    public abstract String getTopic();

    public ECRHubConfig getConfig() {
        return config;
    }

    public void setConfig(ECRHubConfig config) {
        this.config = config;
    }

    public String getMsg_id() {
        if (StrUtil.isBlank(msg_id)) {
            msg_id = IdUtil.fastSimpleUUID();
        }
        return msg_id;
    }

    public void setMsg_id(String msg_id) {
        this.msg_id = msg_id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public VoiceData getVoice_data() {
        return voice_data;
    }

    public void setVoice_data(VoiceData voice_data) {
        this.voice_data = voice_data;
    }

    public PrinterData getPrinter_data() {
        return printer_data;
    }

    public void setPrinter_data(PrinterData printer_data) {
        this.printer_data = printer_data;
    }

    public NotifyData getNotify_data() {
        return notify_data;
    }

    public void setNotify_data(NotifyData notify_data) {
        this.notify_data = notify_data;
    }

    /**
     * Voice data object
     */
    public static class VoiceData {
        /**
         * Voice content
         */
        @JSONField(name = "content")
        private String content;
        /**
         * Voice content local
         */
        @JSONField(name = "contentLocale")
        private String content_locale;
        /**
         * Voice content url
         */
        @JSONField(name = "contentUrl")
        private String content_url;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getContent_locale() {
            return content_locale;
        }

        public void setContent_locale(String content_locale) {
            this.content_locale = content_locale;
        }

        public String getContent_url() {
            return content_url;
        }

        public void setContent_url(String content_url) {
            this.content_url = content_url;
        }
    }

    /**
     * Printer data object
     */
    public static class PrinterData {
        /**
         * Printer content
         */
        @JSONField(name = "content")
        private String content;
        /**
         * Printer content url
         */
        @JSONField(name = "contentUrl")
        private String content_url;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getContent_url() {
            return content_url;
        }

        public void setContent_url(String content_url) {
            this.content_url = content_url;
        }
    }

    /**
     * Notify data object
     */
    public static class NotifyData {
        /**
         * Notify title
         */
        @JSONField(name = "title")
        private String title;
        /**
         * Notify body
         */
        @JSONField(name = "body")
        private String body;
        /**
         * Notify image url
         */
        @JSONField(name = "imageUrl")
        private String image_url;
        /**
         * Notify sound
         */
        @JSONField(name = "sound")
        private String sound;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public String getImage_url() {
            return image_url;
        }

        public void setImage_url(String image_url) {
            this.image_url = image_url;
        }

        public String getSound() {
            return sound;
        }

        public void setSound(String sound) {
            this.sound = sound;
        }

    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}