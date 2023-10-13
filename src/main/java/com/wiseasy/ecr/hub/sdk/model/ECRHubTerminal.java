package com.wiseasy.ecr.hub.sdk.model;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.annotation.JSONField;
import org.java_websocket.WebSocket;

/**
 * @author wangyuxiang@wiseasy.com
 * @since 2023-10-13 09:30
 */
public class ECRHubTerminal {

    private String terminal_type;

    private String cashier_app_version;

    private String ip_address;

    private String mac_address;

    private Integer connect_port;
    @JSONField(serialize = false)
    private WebSocket socket;

    public ECRHubTerminal() {

    }

    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }

    public ECRHubTerminal(String terminal_type, String cashier_app_version, String ip_address, String mac_address, Integer connect_port, WebSocket socket) {
        this.terminal_type = terminal_type;
        this.cashier_app_version = cashier_app_version;
        this.ip_address = ip_address;
        this.mac_address = mac_address;
        this.connect_port = connect_port;
        this.socket = socket;
    }

    public String getTerminal_type() {
        return terminal_type;
    }

    public String getCashier_app_version() {
        return cashier_app_version;
    }

    public String getIp_address() {
        return ip_address;
    }

    public String getMac_address() {
        return mac_address;
    }

    public Integer getConnect_port() {
        return connect_port;
    }

    public WebSocket getSocket() {
        return socket;
    }
}
