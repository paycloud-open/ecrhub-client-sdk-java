package com.wiseasy.ecr.hub.sdk.model;

import com.alibaba.fastjson2.JSONObject;

/**
 * @author wangyuxiang@wiseasy.com
 * @since 2023-10-13 09:30
 */
public class ECRHubDevice {

    private String terminal_sn;

    private String terminal_type;

    private String cashier_app_version;

    private String ip_address;

    private String mac_address;

    private Integer connect_port;

    private String ws_address;


    public String getTerminal_sn() {
        return terminal_sn;
    }

    public void setTerminal_sn(String terminal_sn) {
        this.terminal_sn = terminal_sn;
    }

    public String getTerminal_type() {
        return terminal_type;
    }

    public void setTerminal_type(String terminal_type) {
        this.terminal_type = terminal_type;
    }

    public String getCashier_app_version() {
        return cashier_app_version;
    }

    public void setCashier_app_version(String cashier_app_version) {
        this.cashier_app_version = cashier_app_version;
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

    public Integer getConnect_port() {
        return connect_port;
    }

    public void setConnect_port(Integer connect_port) {
        this.connect_port = connect_port;
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
