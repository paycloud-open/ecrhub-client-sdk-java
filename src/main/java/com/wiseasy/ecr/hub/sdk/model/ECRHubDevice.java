package com.wiseasy.ecr.hub.sdk.model;

import com.alibaba.fastjson2.JSONObject;

/**
 * @author wangyuxiang
 * @since 2023-10-13 09:30
 */
public class ECRHubDevice {

    private String terminal_sn;

    private String ip_address;

    private String mac_address;

    private String ws_address;


    public String getTerminal_sn() {
        return terminal_sn;
    }

    public void setTerminal_sn(String terminal_sn) {
        this.terminal_sn = terminal_sn;
    }

    public String getIp_address() {
        return ip_address;
    }

    public void setIp_address(String ip_address) {
        this.ip_address = ip_address;
    }

    public String getMac_address() {
        return mac_address;
    }

    public void setMac_address(String mac_address) {
        this.mac_address = mac_address;
    }


    public String getWs_address() {
        return ws_address;
    }

    public void setWs_address(String ws_address) {
        this.ws_address = ws_address;
    }

    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }


}
