package com.wrlus.seciot.hook;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

public class FridaServerAgent {
    public static native boolean requestRootPermission();
    public static boolean requestRootPermissionJava() throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder("su");
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        BufferedReader bs = new BufferedReader(new InputStreamReader(process.getInputStream()));
        DataOutputStream os = new DataOutputStream(process.getOutputStream());
        os.writeBytes("ls /data/local/tmp" + "\n");
        os.writeBytes("exit\n");
        os.flush();
        process.waitFor();
        if (process.exitValue() != 0) {
            return false;
        }
        String line = null;
        while ((line = bs.readLine()) != null) {
            Log.i("RootCheck", line);
        }
        return true;
    }
}
