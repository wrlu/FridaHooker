package com.wrlus.fridahooker.util;

import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

public class DeviceHelper {

    public static boolean requestRootPermission(String testCmd) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("su");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            BufferedReader bs = new BufferedReader(new InputStreamReader(process.getInputStream()));
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(testCmd + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
            String line;
            while ((line = bs.readLine()) != null) {
                Log.i("RootCheck", line);
            }
            if (process.exitValue() == 0) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

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
}
