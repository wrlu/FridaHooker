package com.wrlus.fridahooker.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class RootShell {
    private static RootShell instance;
    private DataOutputStream os;
    private BufferedReader bs;
    private Thread readThread;
    private Process process;
    private boolean isRunning = false;

    private RootShell() {
        if (!isRunning) {
            init();
        }
        Log.d("RootShell", "Creating RootShell Singleton.");
    }

    public static RootShell getInstance() {
        if (instance == null) {
            synchronized (RootShell.class) {
                if (instance == null) {
                    instance = new RootShell();
                }
            }
        }
        return instance;
    }

    public synchronized void init() {
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
                            Log.i("RootShell", line);
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
