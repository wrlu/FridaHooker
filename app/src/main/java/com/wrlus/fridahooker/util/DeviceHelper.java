package com.wrlus.fridahooker.util;

import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

public class DeviceHelper {

    public static String[] getSupportedAbis() {
        return Build.SUPPORTED_ABIS;
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

    public static boolean checkAPILevel() {
//        frida issues #971
//        https://github.com/frida/frida/issues/971
        return !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);
    }
}
