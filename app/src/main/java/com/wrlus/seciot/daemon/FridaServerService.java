package com.wrlus.seciot.daemon;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

public class FridaServerService extends Service {

    private FridaServerThread daemonThread;

    static {
        System.loadLibrary("seciot_agent");
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Cannot bind FridaServerService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String version = intent.getStringExtra("version");
        if (version == null) {
            Log.e("FridaServerService", "Missing parameter version: version is null.");
            return super.onStartCommand(intent, flags, startId);
        }
        daemonThread = new FridaServerThread(version);
        daemonThread.setName("Thread-fridaserver");
        daemonThread.setDaemon(true);
        daemonThread.start();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        daemonThread.interrupt();
    }

    class FridaServerThread extends Thread {
        private String version;

        FridaServerThread(String version) {
            this.version = version;
        }

        @Override
        public void run() {
            super.run();
            String targetPath = "/data/local/tmp/seciot/frida/" + version + "/";
            String[] cmds = {
                    "cd "+targetPath,
                    "chmod +x frida-server",
                    "./frida-server"
            };
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
                os.flush();
                process.waitFor();
                String line;
                while ((line = bs.readLine()) != null) {
                    Log.i("FridaServerThread", line);
                }
                Log.e("FridaServerThread", "Process frida server exited with code "+process.exitValue());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
