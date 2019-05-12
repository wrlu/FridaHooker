package com.wrlus.seciot.agent;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;

import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class FrpcAgent {
    private static final String AGENT_SERVER = "http://10.5.26.179:8080/SecIoT";
    private static final String FRP_DOWNLOAD_LINK = AGENT_SERVER + "/attach/downloads/frp/v${version}/";
    private static final String FRP_NAME = "frp_${version}_linux_${abi}.tar.gz";

    public static void getFrpsVersionOnServer(Callback callback) {
        String url = AGENT_SERVER + "/agent/frps-version";
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().get().url(url).build();
        okHttpClient.newCall(request).enqueue(callback);
    }

    public static void downloadFrp(String version, String abi, Callback callback) {
        String url = FRP_DOWNLOAD_LINK.replace("${version}", version) +
                FRP_NAME.replace("${version}", version).replace("${abi}", abi);
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().get().url(url).build();
        okHttpClient.newCall(request).enqueue(callback);
    }

    public static void installFrpc(final File downloadFile, final String version, final StatusCallback callback) {
        String targetPath = "/data/local/tmp/seciot/frp/" + version + "/";
        final String[] cmds = {
                "mkdir /data/local/tmp/seciot/",
                "mkdir /data/local/tmp/seciot/frp/",
                "mkdir " + targetPath,
                "mv " + downloadFile.getAbsolutePath() + " " + targetPath,
                "cd " + targetPath,
                "tar -zxvf " + downloadFile.getName(),
                "rm " + downloadFile.getName(),
                "mv " + downloadFile.getName().replace(".tar.gz", "") + "/frpc ./",
                "mv " + downloadFile.getName().replace(".tar.gz", "") + "/frpc.ini ./",
                "rm -rf " + downloadFile.getName().replace(".tar.gz", ""),
                "chmod +x frpc"
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
                    if (process.exitValue() != 0) {
                        callback.onFailure(process.exitValue(), null);
                        return;
                    }
                    String line;
                    while ((line = bs.readLine()) != null) {
                        Log.i("InstallFrpc", line);
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

    public static void bindRemotePort(String clientId, Callback callback) {
        String url = AGENT_SERVER + "/agent/bind";
        OkHttpClient okHttpClient = new OkHttpClient();
        RequestBody body = new FormBody.Builder().add("client_id", clientId).build();
        Request request = new Request.Builder().post(body).url(url).build();
        okHttpClient.newCall(request).enqueue(callback);
    }

    public static void unBindRemotePort(String clientId, Callback callback) {
        String url = AGENT_SERVER + "/agent/unbind";
        OkHttpClient okHttpClient = new OkHttpClient();
        RequestBody body = new FormBody.Builder().add("client_id", clientId).build();
        Request request = new Request.Builder().post(body).url(url).build();
        okHttpClient.newCall(request).enqueue(callback);
    }

    public static boolean checkFrpcInstallation(String version) {
        String targetPath = "/data/local/tmp/seciot/frp/" + version + "/";
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("ls", targetPath + "frpc", targetPath + "frpc.ini");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            BufferedReader bs = new BufferedReader(new InputStreamReader(process.getInputStream()));
            process.waitFor();
            String line;
            while ((line = bs.readLine()) != null) {
                Log.i("FrpCheck", line);
            }
            if (process.exitValue() == 0) {
                return true;
            }
            Log.e("FrpCheck", String.valueOf(process.exitValue()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void removeFrpc(final String version, final StatusCallback callback) {
        String targetPath = "/data/local/tmp/seciot/frp/" + version + "/";
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
                    if (process.exitValue() != 0) {
                        callback.onFailure(process.exitValue(), null);
                        return;
                    }
                    String line;
                    while ((line = bs.readLine()) != null) {
                        Log.i("RemoveFrpc", line);
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

    public static void startFrpc(String version) {

    }

    public static void stopFrpc(String version) {

    }
}
