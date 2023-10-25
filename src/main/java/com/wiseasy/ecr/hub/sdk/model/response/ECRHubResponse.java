package com.wiseasy.ecr.hub.sdk.model.response;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.annotation.JSONField;

import java.util.Map;

public class ECRHubResponse {
    /**
     * Transaction request ID, used to receive the corresponding response. The caller needs to remain unique.
     */
    @JSONField(name = "requestId")
    private String request_id;
    /**
     * Execution status, true:success, false:failure
     */
    @JSONField(name = "success")
    private boolean success;
    /**
     * Error message, this msg contains an error message when an error occurs
     */
    @JSONField(name = "errorMsg")
    private String error_msg;
    /**
     * Extended parameters
     */
    @JSONField(name = "extendsParams")
    private Map<String, String> extends_params;
    /**
     * Device data object
     */
    @JSONField(name = "deviceData")
    private DeviceData device_data;

    public String getRequest_id() {
        return request_id;
    }

    public void setRequest_id(String request_id) {
        this.request_id = request_id;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getError_msg() {
        return error_msg;
    }

    public void setError_msg(String error_msg) {
        this.error_msg = error_msg;
    }

    public DeviceData getDevice_data() {
        return device_data;
    }

    public void setDevice_data(DeviceData device_data) {
        this.device_data = device_data;
    }

    public Map<String, String> getExtends_params() {
        return extends_params;
    }

    public void setExtends_params(Map<String, String> extends_params) {
        this.extends_params = extends_params;
    }

    /**
     * Device data object
     */
    public static class DeviceData {
        /**
         * Device SN
         */
        @JSONField(name = "deviceSn")
        private String device_sn;
        /**
         * App name
         */
        @JSONField(name = "appName")
        private String app_name;
        /**
         * App version
         */
        @JSONField(name = "appVersion")
        private String app_version;

        public String getDevice_sn() {
            return device_sn;
        }

        public void setDevice_sn(String device_sn) {
            this.device_sn = device_sn;
        }

        public String getApp_name() {
            return app_name;
        }

        public void setApp_name(String app_name) {
            this.app_name = app_name;
        }

        public String getApp_version() {
            return app_version;
        }

        public void setApp_version(String app_version) {
            this.app_version = app_version;
        }
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}