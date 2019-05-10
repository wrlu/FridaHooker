package com.wrlus.seciot.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

public class DeviceHelper {
    public static boolean requestRootPermission(String testCmd) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder("su");
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        BufferedReader bs = new BufferedReader(new InputStreamReader(process.getInputStream()));
        DataOutputStream os = new DataOutputStream(process.getOutputStream());
        os.writeBytes(testCmd + "\n");
        os.writeBytes("exit\n");
        os.flush();
        process.waitFor();
        if (process.exitValue() != 0) {
            return false;
        }
        String line;
        while ((line = bs.readLine()) != null) {
            Log.i("RootCheck", line);
        }
        return true;
    }

    public static String getProductCpuAbi() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("getprop", "ro.product.cpu.abi");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            BufferedReader bs = new BufferedReader(new InputStreamReader(process.getInputStream()));
            process.waitFor();
            if (process.exitValue() == 0) {
                return bs.readLine().replace("\n", "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
