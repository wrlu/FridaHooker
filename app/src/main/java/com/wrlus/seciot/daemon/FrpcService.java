package com.wrlus.seciot.daemon;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class FrpcService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Cannot bind TCPForwardService");
    }
}
