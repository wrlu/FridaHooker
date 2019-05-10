package com.wrlus.seciot.net;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPForwardService extends Service {

    private String remoteIp = "10.5.26.179";
    private int remotePort = 8042;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Cannot bind TCPForwardService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TCPForwardService.class.getSimpleName(), "Service started");
        TCPForwardListenThread listenThread = new TCPForwardListenThread();
        listenThread.setName("Thread-TCPForwardListen");
        listenThread.setDaemon(true);
        listenThread.start();
        return super.onStartCommand(intent, flags, startId);
    }

    class TCPForwardListenThread extends Thread {

        @Override
        public void run() {
            super.run();
            try {
                ServerSocket serverSocket = new ServerSocket(8042);
                while(true) {
                    try {
                        Socket socket = serverSocket.accept();
                        Thread thread = new Thread(new TCPForwardHandler(socket));
                        thread.setDaemon(true);
                        thread.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class TCPForwardHandler implements Runnable {

        private Socket socket;

        public TCPForwardHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                Socket forwardSocket = new Socket(remoteIp, remotePort);
                InputStream localis = socket.getInputStream();
                OutputStream localos = socket.getOutputStream();
                InputStream forwardis = forwardSocket.getInputStream();
                OutputStream forwardos = forwardSocket.getOutputStream();
                int localbyte;
                byte[] localbuffer = new byte[4096];
                while ( (localbyte = localis.read(localbuffer)) != -1) {
                    forwardos.write(localbuffer, 0, localbyte);
                    forwardos.flush();
                    int forwardbyte;
                    byte[] forwardbuffer = new byte[4096];
                    while ( (forwardbyte = forwardis.read(forwardbuffer)) != -1) {
                        localos.write(forwardbuffer, 0, forwardbyte);
                        localos.flush();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
