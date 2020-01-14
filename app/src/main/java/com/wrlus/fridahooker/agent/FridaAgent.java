package com.wrlus.fridahooker.agent;

import android.content.Context;
import android.util.Log;

import com.wrlus.fridahooker.util.LogUtil;
import com.wrlus.fridahooker.util.NativeRootShell;

import org.tukaani.xz.XZInputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;

public class FridaAgent {
    private static final String TAG = "FridaAgent";
    private static FridaAgent instance;
    private Context context;

    private FridaAgent(Context context) {
        this.context = context;
        Log.d("FridaAgent", "Create FridaAgent singleton");
    }

    public static FridaAgent getInstance(Context context) {
        if (instance == null) {
            synchronized (FridaAgent.class) {
                if (instance == null) {
                    instance = new FridaAgent(context);
                }
            }
        }
        return instance;
    }

    public boolean checkFridaInstallation(String version) {
        String targetPath = context.getFilesDir().getAbsolutePath() + "/frida/"+version+"/";
        int code = NativeRootShell.execute("ls " + targetPath + "frida-server*");
        LogUtil.d(TAG, "checkFridaInstallation exit with code "+code);
        return 0 == code;
    }

    public void checkFridaRunning(final StatusCallback callback) {
        Thread checkFridaThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    ServerSocket serverSocket = new ServerSocket(27042);
                    serverSocket.close();
                    LogUtil.d(TAG, "checkFridaRunning try to bind tcp:27042 success, frida is not running.");
                    callback.onFailure(null);
                } catch (IOException e) {
                    LogUtil.d(TAG, "checkFridaRunning try to bind tcp:27042 but failed, frida is still running.");
                    callback.onSuccess();
                }
            }
        });
        checkFridaThread.setDaemon(true);
        checkFridaThread.setName("Thread-checkFrida");
        checkFridaThread.start();
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

    public boolean installFrida(File installFile, String version) {
        String targetPath = context.getFilesDir().getAbsolutePath() + "/frida/"+version+"/";
        final String[] cmds = {
                "mkdir -p " + targetPath,
                "mv " + installFile.getAbsolutePath() + " " + targetPath
        };
        for (String cmd : cmds) {
            int code = NativeRootShell.execute(cmd);
            LogUtil.d(TAG, "installFrida `"+cmd+"` command exit with code "+code);
            if (0 != code) {
                return false;
            }
        }
        return true;
    }

    public boolean removeFrida(String version) {
        String targetPath = context.getFilesDir().getAbsolutePath() + "/frida/"+version+"/";
        int code = NativeRootShell.execute("rm -rf " + targetPath);
        LogUtil.d(TAG, "removeFrida exit with code "+code);
        return 0 == code;
    }

    public boolean startFrida(String version) {
        String targetPath = context.getFilesDir().getAbsolutePath() + "/frida/"+version+"/";
        String[] cmds = {
                "chmod +x " + targetPath + "frida-server*",
                "su -c " + targetPath + "frida-server*  &",
        };
        for (String cmd : cmds) {
            int code = NativeRootShell.execute(cmd);
            LogUtil.d(TAG, "startFrida `"+cmd+"` command exit with code "+code);
            if (0 != code) {
                return false;
            }
        }
        return true;
    }

    public boolean stopFrida(String version, String abi) {
        String[] cmds = {
                "su -c kill -9 $(su -c pidof frida-server-"+version+"-android-"+abi+") &",
        };
        for (String cmd : cmds) {
            int code = NativeRootShell.execute(cmd);
            LogUtil.d(TAG, "stopFrida `"+cmd+"` command exit with code "+code);
            if (0 != code) {
                return false;
            }
        }
        return true;
    }
}
