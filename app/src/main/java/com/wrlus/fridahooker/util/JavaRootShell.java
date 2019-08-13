package com.wrlus.fridahooker.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class JavaRootShell {
    private static final String TAG = "JavaRootShell";
    private static JavaRootShell instance;
    private DataOutputStream os;
    private BufferedReader bs;
    private Thread readThread;
    private Process process;
    private boolean isRunning = false;

    private JavaRootShell() {
        if (!isRunning) {
            init();
        }
        Log.d("JavaRootShell", "Creating JavaRootShell Singleton.");
    }

    public static JavaRootShell getInstance() {
        if (instance == null) {
            synchronized (JavaRootShell.class) {
                if (instance == null) {
                    instance = new JavaRootShell();
                }
            }
        }
        return instance;
    }

    private synchronized void init() {
        if (isRunning) {
            return;
        }
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("su");
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();
            bs = new BufferedReader(new InputStreamReader(process.getInputStream()));
            os = new DataOutputStream(process.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        readThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while(!Thread.currentThread().isInterrupted()) {
                        String line;
                        while ((line = bs.readLine()) != null) {
                            Log.i("JavaRootShell", line);
                        }
                        Thread.sleep(1000);
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        readThread.setDaemon(true);
        readThread.setName("Thread-ReadShell");
        readThread.start();
        isRunning = true;
    }

    public synchronized void execute(String cmd) throws IOException {
        if (!isRunning) {
            init();
        }
        os.writeBytes(cmd + "\n");
        os.flush();
    }

    public synchronized void execute(String[] cmds) throws IOException {
        if (!isRunning) {
            init();
        }
        for (String cmd : cmds) {
            os.writeBytes(cmd + "\n");
        }
        os.flush();
    }

    public synchronized void waitFor() throws InterruptedException {
        process.waitFor();
    }

    public synchronized void exit() throws IOException {
        if (!isRunning) {
            return;
        }
        os.writeBytes("exit\n");
        os.flush();
        if (readThread != null) {
            readThread.interrupt();
        }
        process.destroy();
        isRunning = false;
    }

}
