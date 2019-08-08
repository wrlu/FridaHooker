package com.wrlus.fridahooker.agent;

public interface StatusCallback {
    void onSuccess();
    void onFailure(int exitCode, Exception e);
}
