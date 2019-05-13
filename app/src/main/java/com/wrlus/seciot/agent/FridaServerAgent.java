package com.wrlus.seciot.agent;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.wrlus.seciot.daemon.FridaServerService;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class FridaServerAgent {
    private static final String AGENT_SERVER_HOST = "10.5.26.179";
    private static final String AGENT_SERVER = "http://"+AGENT_SERVER_HOST+":8080/SecIoT";
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

    public static void installFridaServer(final File downloadFile, final String version, final StatusCallback callback) {
        String targetPath = "/data/local/tmp/seciot/frida/" + version + "/";
        final String[] cmds = {
                "mkdir /data/local/tmp/seciot/",
                "mkdir /data/local/tmp/seciot/frida/",
                "mkdir " + targetPath,
                "mv " + downloadFile.getAbsolutePath() + " " + targetPath,
                "cd " + targetPath,
                "tar -zxvf " + downloadFile.getName(),
                "rm " + downloadFile.getName(),
                "mv " + downloadFile.getName().replace(".tar.gz", "") + " frida-server",
                "chmod +x frida-server"
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
                        Log.d("ExecCmd", cmd);
                        os.writeBytes( cmd + "\n");
                    }
                    os.writeBytes("exit\n");
                    os.flush();
                    process.waitFor();
                    String line;
                    while ((line = bs.readLine()) != null) {
                        Log.i("InstallFridaServer", line);
                    }
                    if (process.exitValue() != 0) {
                        callback.onFailure(process.exitValue(), null);
                        return;
                    }
                    callback.onSuccess();
                } catch (Exception e) {
                    callback.onFailure(-1, e);
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public static boolean checkFridaServerInstallation(String version) {
        String targetPath = "/data/local/tmp/seciot/frida/" + version + "/";
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("ls", targetPath + "frida-server");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            BufferedReader bs = new BufferedReader(new InputStreamReader(process.getInputStream()));
            process.waitFor();
            String line;
            while ((line = bs.readLine()) != null) {
                Log.i("FridaCheck", line);
            }
            if (process.exitValue() == 0) {
                return true;
            }
            Log.e("FridaCheck", String.valueOf(process.exitValue()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void removeFridaServer(final String version, final StatusCallback callback) {
        String targetPath = "/data/local/tmp/seciot/frida/" + version + "/";
        final String[] cmds = {
                "rm -rf " + targetPath
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
                        Log.d("ExecCmd", cmd);
                        os.writeBytes( cmd + "\n");
                    }
                    os.writeBytes("exit\n");
                    os.flush();
                    process.waitFor();
                    String line;
                    while ((line = bs.readLine()) != null) {
                        Log.i("RemoveFridaServer", line);
                    }
                    if (process.exitValue() != 0) {
                        callback.onFailure(process.exitValue(), null);
                        return;
                    }
                    callback.onSuccess();
                } catch (Exception e) {
                    e.printStackTrace();
                    callback.onFailure(-1, e);
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public static void startFridaServer(Context context, String version) {
        Intent intent = new Intent(context, FridaServerService.class);
        intent.putExtra("version", version);
        context.startService(intent);
    }

    public static void stopFridaServer(Context context) {
        Intent intent = new Intent(context, FridaServerService.class);
        context.stopService(intent);
    }
}
