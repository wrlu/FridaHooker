package com.wrlus.seciot.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class RootShellHelper {
    private static RootShellHelper instance;
    private DataOutputStream os;
    private BufferedReader bs;
    private Thread readThread;

    private RootShellHelper() {
//        Thread shellThread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//
//            }
//        });
//        shellThread.setDaemon(true);
//        shellThread.setName("Thread-RootShell");
//        shellThread.start();
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("su");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            bs = new BufferedReader(new InputStreamReader(process.getInputStream()));
            os = new DataOutputStream(process.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        readThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while(Thread.currentThread().isInterrupted()) {
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
        Log.d("RootShellHelper", "Creating RootShellHelper Singleton.");
    }
    public static RootShellHelper getInstance() {
        if (instance == null) {
            synchronized (RootShellHelper.class) {
                if (instance == null) {
                    instance = new RootShellHelper();
                }
            }
        }
        return instance;
    }

    public synchronized void execute(String cmd) throws IOException {
        os.writeBytes(cmd + "\n");
        os.flush();
    }

    public synchronized void execute(String[] cmds) throws IOException {
        for (String cmd : cmds) {
            os.writeBytes(cmd + "\n");
        }
        os.flush();
    }

    public synchronized void exit() throws IOException {
        os.writeBytes("exit\n");
        os.flush();
        if (readThread != null) {
            readThread.interrupt();
        }
    }
}
