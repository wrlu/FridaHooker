package com.wrlus.fridahooker.agent;

import android.util.Log;

import com.wrlus.fridahooker.util.RootShellHelper;

import org.tukaani.xz.XZInputStream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class FridaAgent {
    private static FridaAgent instance;
    private String FRIDA_CENTER = "https://github.com/frida/frida/releases/download";

    private FridaAgent() {
        Log.d("FridaAgent", "Create FridaAgent singleton");
    }

    public static FridaAgent getInstance() {
        if (instance == null) {
            synchronized (FridaAgent.class) {
                if (instance == null) {
                    instance = new FridaAgent();
                }
            }
        }
        return instance;
    }

    public void getRemoteFridaVersion(Callback callback) {
        String url = FRIDA_CENTER + "/agent/frida-version";
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().get().url(url).build();
        okHttpClient.newCall(request).enqueue(callback);
    }

    public boolean checkFridaInstallation(String version) {
        String targetPath = "/data/local/tmp/seciot/frida/" + version + "/";
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("ls", targetPath + "frida-server");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            BufferedReader bs = new BufferedReader(new InputStreamReader(process.getInputStream()));
            process.waitFor();
            String line;
            while ((line = bs.readLine()) != null) {
                Log.i("FridaInstallationCheck", line);
            }
            if (process.exitValue() == 0) {
                return true;
            }
            Log.e("FridaInstallationCheck", String.valueOf(process.exitValue()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void downloadFrida(String version, String abi, Callback callback) {
        String url = FRIDA_CENTER + "/attach/downloads/frida/${version}/".replace("${version}", version) +
                "frida-server-${version}-android-${abi}.xz".replace("${version}", version).replace("${abi}", abi);
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        okHttpClient.newCall(request).enqueue(callback);
    }

    public File extractXZ(InputStream source, String target) throws IOException {
        XZInputStream xzis = new XZInputStream(source);
        FileOutputStream fos = new FileOutputStream(target);
        int len;
        byte[] buffer = new byte[4096];
        while (-1 != (len = xzis.read(buffer))) {
            fos.write(buffer, 0, len);
            fos.flush();
        }
        xzis.close();
        fos.close();
        return new File(target);
    }

    public void installFrida(final File installFile, final String version, final StatusCallback callback) {
        String targetPath = "/data/local/tmp/seciot/frida/" + version + "/";
        final String[] cmds = {
                "mkdir -p " + targetPath,
                "mv " + installFile.getAbsolutePath() + " " + targetPath,
                "cd " + targetPath,
                "mv " + installFile.getName() + " frida-server",
        };
        RootShellHelper rootShellHelper = RootShellHelper.getInstance();
        try {
            rootShellHelper.execute(cmds);
            callback.onSuccess();
        } catch (IOException e) {
            callback.onFailure(-1, e);
        }
    }

    public void removeFrida(final String version, final StatusCallback callback) {
        String targetPath = "/data/local/tmp/seciot/frida/" + version + "/";
        final String[] cmds = {
                "rm -rf " + targetPath
        };
        RootShellHelper rootShellHelper = RootShellHelper.getInstance();
        try {
            rootShellHelper.execute(cmds);
            callback.onSuccess();
        } catch (IOException e) {
            callback.onFailure(-1, e);
        }
    }

    public void startFrida(String version) {
        String targetPath = "/data/local/tmp/seciot/frida/" + version + "/";
        String[] cmds = {
                "cd "+targetPath,
                "chmod +x frida-server",
                "./frida-server &"
        };
        RootShellHelper rootShellHelper = RootShellHelper.getInstance();
        try {
            rootShellHelper.execute(cmds);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopFrida() {
        RootShellHelper rootShellHelper = RootShellHelper.getInstance();
        try {
            rootShellHelper.execute("kill -9 $(pidof frida-server)");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
