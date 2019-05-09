package com.wrlus.seciot.model;

public abstract class BaseResponse {
    private long status;
    private String reason;

    public long getStatus() {
        return status;
    }

    public void setStatus(long status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
