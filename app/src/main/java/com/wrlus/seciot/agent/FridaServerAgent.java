package com.wrlus.seciot.agent;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class FridaServerAgent {
    private static final String AGENT_SERVER = "http://10.5.26.179:8080/SecIoT";
    private static final String FRIDA_DOWNLOAD_LINK = AGENT_SERVER + "/attach/downloads/frida/${version}/";
    private static final String FRIDA_SERVER_NAME = "frida-server-${version}-android-${abi}.tar.gz";

    public static void getFridaVersionOnServer(Callback callback) {
        String url = AGENT_SERVER + "/agent/frida-version";
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().get().url(url).build();
        okHttpClient.newCall(request).enqueue(callback);
    }

    public static void downloadFridaServer(String version, String abi, Callback callback) {
        String url = FRIDA_DOWNLOAD_LINK.replace("${version}", version) +
                FRIDA_SERVER_NAME.replace("${version}", version).replace("${abi}", abi);
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        okHttpClient.newCall(request).enqueue(callback);
    }

    public static void installFridaServer(final File downloadFile, final String version) {
        String targetPath = "/data/local/tmp/seciot/frida/" + version + "/";
        final String[] cmds = {
                "whoami",
                "mkdir /data/local/tmp/seciot/",
                "mkdir /data/local/tmp/seciot/frida/",
                "mkdir " + targetPath,
                "cp " + downloadFile.getAbsolutePath() + " " + targetPath,
                "chmod +x "
        };
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessBuilder processBuilder = new ProcessBuilder("su");
                    processBuilder.redirectErrorStream(true);
                    Process process = processBuilder.start();
                    BufferedReader bs = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    DataOutputStream os = new DataOutputStream(process.getOutputStream());
                    for (String cmd : cmds) {
                        os.writeBytes( cmd + "\n");
                    }
                    os.writeBytes("exit\n");
                    os.flush();
                    process.waitFor();
                    if (process.exitValue() != 0) {

                    }
                    String line;
                    while ((line = bs.readLine()) != null) {
                        Log.i("RootCheck", line);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public static void removeFridaServer(String version) {

    }

    public static void startFridaServer(String version) {

    }

    public static void stopFridaServer(String version) {

    }
}
