package com.wrlus.seciot.agent;

public interface StatusCallback {
    public void onSuccess();
    public void onFailure(int exitCode, Exception e);
}
