package com.wrlus.seciot.daemon;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

@Deprecated
public class FrpcService extends Service {

    private FrpcThread daemonThread;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Cannot bind FrpcService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return super.onStartCommand(null, flags, startId);
        }
        String version = intent.getStringExtra("version");
        if (version == null) {
            Log.e("FrpcService", "Missing parameter version: version is null.");
            return super.onStartCommand(intent, flags, startId);
        }
        daemonThread = new FrpcThread(version);
        daemonThread.setName("Thread-frpc");
        daemonThread.setDaemon(true);
        daemonThread.start();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        String cmd = "kill -9 $(pidof frpc)";
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("su");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            BufferedReader bs = new BufferedReader(new InputStreamReader(process.getInputStream()));
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            Log.d("ExecCmd", cmd);
            os.writeBytes( cmd + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
            String line;
            while ((line = bs.readLine()) != null) {
                Log.i("FrpcThread", line);
            }
            Log.e("FrpcThread", "Kill process frida server exited with code "+process.exitValue());
        } catch (Exception e) {
            e.printStackTrace();
        }
        daemonThread.interrupt();
    }

    class FrpcThread extends Thread {
        private String version;

        FrpcThread(String version) {
            this.version = version;
        }

        @Override
        public void run() {
            super.run();
            String targetPath = "/data/local/tmp/seciot/frp/" + version + "/";
            String[] cmds = {
                    "cd "+targetPath,
                    "chmod +x frpc",
                    "./frpc -c frpc.ini"
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
                os.writeBytes("exit\n");
                os.flush();
                process.waitFor();
                String line;
                while ((line = bs.readLine()) != null) {
                    Log.i("FrpcThread", line);
                }
                Log.e("FrpcThread", "Process frida server exited with code "+process.exitValue());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
