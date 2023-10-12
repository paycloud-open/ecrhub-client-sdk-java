package com.wiseasy.ecr.hub.sdk.model.request;

import com.alibaba.fastjson2.annotation.JSONField;
import com.wiseasy.ecr.hub.sdk.model.response.PairResponse;

public class PairRequest extends ECRHubRequest<PairResponse> {

    @Override
    public String getTopic() {
        return "ecrhub.pair";
    }

    /**
     * device name
     */
    @JSONField(name = "deviceName")
    private String device_name;
    /**
     * device alias name
     */
    @JSONField(name = "aliasName")
    private String alias_name;
    /**
     * device mac address
     */
    @JSONField(name = "macAddress")
    private String mac_address;
    /**
     * ip address
     */
    @JSONField(name = "ipAddress")
    private String ip_address;
    /**
     * port
     */
    @JSONField(name = "port")
    private String port;

    public String getDevice_name() {
        return device_name;
    }

    public void setDevice_name(String device_name) {
        this.device_name = device_name;
    }

    public String getAlias_name() {
        return alias_name;
    }

    public void setAlias_name(String alias_name) {
        this.alias_name = alias_name;
    }

    public String getMac_address() {
        return mac_address;
    }

    public void setMac_address(String mac_address) {
        this.mac_address = mac_address;
    }

    public String getIp_address() {
        return ip_address;
    }

    public void setIp_address(String ip_address) {
        this.ip_address = ip_address;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }
}