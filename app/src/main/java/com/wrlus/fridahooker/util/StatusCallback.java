package com.wrlus.fridahooker.util;

public interface StatusCallback {
    void onSuccess();
    void onFailure(int exitCode, Exception e);
}
