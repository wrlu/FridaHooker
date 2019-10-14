package com.wrlus.fridahooker.agent;

import android.util.Log;

import com.wrlus.fridahooker.util.LogUtil;
import com.wrlus.fridahooker.util.NativeRootShell;

import org.tukaani.xz.XZInputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class FridaAgent {
    private static final String TAG = "FridaAgent";
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
        int code = NativeRootShell.execute("ls " + targetPath + "frida-server");
        LogUtil.d(TAG, "checkFridaInstallation exit with code "+code);
        return 0 == code;
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
        LogUtil.d(TAG, "Target: " + target);
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
        RootShell rootShell = RootShell.getInstance();
        try {
            rootShell.execute(cmds);
            callback.onSuccess();
        } catch (IOException e) {
            LogUtil.e(TAG, e);
            callback.onFailure(-1, e);
        }
    }

    public void removeFrida(final String version, final StatusCallback callback) {
        String targetPath = "/data/local/tmp/seciot/frida/" + version + "/";
        final String[] cmds = {
                "rm -rf " + targetPath
        };
        RootShell rootShell = RootShell.getInstance();
        try {
            rootShell.execute(cmds);
            callback.onSuccess();
        } catch (IOException e) {
            LogUtil.e(TAG, e);
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
        RootShell rootShell = RootShell.getInstance();
        try {
            rootShell.execute(cmds);
        } catch (IOException e) {
            LogUtil.e(TAG, e);
        }
    }

    public void stopFrida() {
        RootShell rootShell = RootShell.getInstance();
        try {
            rootShell.execute("kill -9 $(pidof frida-server)");
        } catch (IOException e) {
            LogUtil.e(TAG, e);
        }
    }
}
