package com.wrlus.fridahooker.util;

import android.os.Build;

public class DeviceHelper {

    public static String[] getSupportedAbis() {
        return Build.SUPPORTED_ABIS;
    }

    public static String conventToFridaAbi(String systemAbi) {
        if (systemAbi.contains("arm64")) {
            return "arm64";
        } else if (systemAbi.contains("arm")) {
            return "arm";
        } else if (systemAbi.contains("x86_64")) {
            return "x86_64";
        } else if (systemAbi.contains("x86")) {
            return "x86";
        }
        return "";
    }

    public static String getProductName() {
        return Build.MANUFACTURER + " " + Build.MODEL + " ( "+ Build.DEVICE +" )";
    }

    public static String getAndroidVersion() {
        return Build.VERSION.RELEASE;
    }

    public static int getAPILevel() {
        return Build.VERSION.SDK_INT;
    }
}
