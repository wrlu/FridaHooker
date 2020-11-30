package com.wrlus.fridahooker.agent;

import android.os.Build;
import android.util.Log;

import com.wrlus.fridahooker.config.Config;
import com.wrlus.fridahooker.util.DeviceHelper;
import com.wrlus.fridahooker.util.LogUtil;
import com.wrlus.fridahooker.shell.NativeShell;

import org.tukaani.xz.XZInputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;

public class FridaAgent {
    private static final String TAG = "FridaAgent";
    private static final int FRIDA_CHECKER_DELAY = Config.FRIDA_CHECKER_DELAY;
    private String installPath;
    private boolean isSupported;
    private boolean isInstalled;
    private volatile boolean isStarted;

    public FridaAgent(String installPath) {
        this.installPath = installPath;
        Log.d("FridaAgent", "Create FridaAgent, isSupported="+isSupported);
    }

    public File extractLocalFrida(InputStream is, String to) throws IOException {
        File cacheFile = new File(to + File.separator + "frida-server");
        XZInputStream xzis = new XZInputStream(is);
        FileOutputStream fos = new FileOutputStream(cacheFile);
        int len;
        byte[] buffer = new byte[4096];
        while (-1 != (len = xzis.read(buffer))) {
            fos.write(buffer, 0, len);
            fos.flush();
        }
        xzis.close();
        fos.close();
        LogUtil.d(TAG, "Local frida cache file path: "+cacheFile.getAbsolutePath());
        return cacheFile;
    }

    public boolean installFrida(File cacheFile) {
        if (!isSupported) {
            return false;
        }
        final String[] cmds = {
                "mkdir -p " + installPath,
                "mv " + cacheFile.getAbsolutePath() + " " + installPath + File.separator
        };
        LogUtil.i(TAG, installPath);
        for (String cmd : cmds) {
            int code = NativeShell.execute(cmd);
            LogUtil.d(TAG, "installFrida `"+cmd+"` command exit with code "+code);
            if (0 != code) {
                return false;
            }
        }
        return true;
    }

    public boolean removeFrida() {
        if (!isSupported || isInstalled) {
            return false;
        }
        int code = NativeShell.execute("rm -rf " + installPath);
        LogUtil.d(TAG, "removeFrida exit with code "+code);
        return 0 == code;
    }

    public boolean startFrida() {
        if (!isSupported || !isInstalled) {
            return false;
        }
        String[] cmds = {
                "chmod +x " + installPath + File.separator+ "frida-server",
                "su -c " + installPath + File.separator + "frida-server  &",
        };
        for (String cmd : cmds) {
            int code = NativeShell.execute(cmd);
            LogUtil.d(TAG, "startFrida `"+cmd+"` command exit with code "+code);
            if (0 != code) {
                return false;
            }
        }
        return true;
    }

    public boolean stopFrida() {
        if (!isSupported || !isInstalled) {
            return false;
        }
        String[] cmds = {
                "su -c kill -9 $(su -c pidof frida-server) &",
        };
        for (String cmd : cmds) {
            int code = NativeShell.execute(cmd);
            LogUtil.d(TAG, "stopFrida `"+cmd+"` command exit with code "+code);
            if (0 != code) {
                return false;
            }
        }
        return true;
    }

    public String getSystemFridaAbi() {
        return DeviceHelper.conventToFridaAbi(DeviceHelper.getSupportedAbis()[0]);
    }

    public boolean validFridaExecutable(File cacheFile) {
        return false;
    }

    public boolean isInstalled() {
        return isInstalled;
    }

    public boolean isStarted() {
        return isStarted;
    }

    public boolean isSupported() {
        return isSupported;
    }

    public void checkAll(final StatusCallback callback) {
        isSupported = checkSupported();
        isInstalled = checkInstallation();
        checkRunning(new StatusCallback() {
            @Override
            public void onSuccess() {
                isStarted = true;
                if (callback != null) callback.onSuccess();
            }

            @Override
            public void onFailure(Throwable e) {
                isStarted = false;
                if (callback != null) callback.onSuccess();
            }
        });
    }

    private boolean checkSupported() {
        if (DeviceHelper.getAPILevel() < Build.VERSION_CODES.LOLLIPOP) {
            return false;
        }
        String abi = DeviceHelper.getSupportedAbis()[0];
        return DeviceHelper.conventToFridaAbi(abi) != null;
    }

    private boolean checkInstallation() {
        if (!isSupported) {
            return false;
        }
        int code = NativeShell.execute("ls " + installPath + File.separator + "frida-server");
        LogUtil.d(TAG, "checkInstallation exit with code "+code);
        return 0 == code;
    }

    private void checkRunning(final StatusCallback callback) {
        if (!isSupported) {
            if (callback != null) {
                callback.onFailure(null);
            }
            return;
        }
        Thread checkFridaThread = new Thread(() -> {
            try {
                Thread.sleep(FRIDA_CHECKER_DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                ServerSocket serverSocket = new ServerSocket(27042);
                serverSocket.close();
                LogUtil.d(TAG, "checkRunning try to bind tcp:27042 success, frida is not running.");
                if (callback != null) callback.onFailure(null);

            } catch (IOException e) {
                LogUtil.d(TAG, "checkRunning try to bind tcp:27042 but failed, frida is still running.");
                if (callback != null) callback.onSuccess();
            }
        });
        checkFridaThread.setDaemon(true);
        checkFridaThread.setName("Thread-checkFrida");
        checkFridaThread.start();
    }
}
