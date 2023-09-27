package com.wiseasy.ecr.hub.sdk.model.response;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.annotation.JSONField;

public abstract class ECRHubResponse {
    /**
     * Message ID, used to receive the corresponding response. The caller needs to remain unique.
     */
    @JSONField(name = "msgId")
    private String msg_id;
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

    public String getMsg_id() {
        return msg_id;
    }

    public void setMsg_id(String msg_id) {
        this.msg_id = msg_id;
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

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}