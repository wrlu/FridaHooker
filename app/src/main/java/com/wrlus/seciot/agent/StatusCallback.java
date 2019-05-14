package com.wrlus.seciot.agent;

public interface StatusCallback {
    void onSuccess();
    void onFailure(int exitCode, Exception e);
}
