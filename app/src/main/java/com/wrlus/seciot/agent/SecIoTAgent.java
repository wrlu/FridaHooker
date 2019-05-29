package com.wrlus.seciot.agent;

import com.wrlus.seciot.util.DeviceHelper;

import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class SecIoTAgent {
    private static final String AGENT_SERVER_HOST = "192.168.43.7";
    private static final String AGENT_SERVER = "http://"+AGENT_SERVER_HOST+":8080/SecIoT";

    public static void addDevice(String clientId, int port, Callback callback) {
        String url = AGENT_SERVER + "/device/add";
        OkHttpClient okHttpClient = new OkHttpClient();
        RequestBody body = new FormBody.Builder()
                .add("clientid", clientId)
                .add("devicename", DeviceHelper.getProductName())
                .add("version", DeviceHelper.getAndroidVersion())
                .add("apilevel", String.valueOf(DeviceHelper.getAPILevel()))
                .add("agentver", "1.0-aosp")
                .add("port", String.valueOf(port))
                .add("online", "1")
                .build();
        Request request = new Request.Builder().post(body).url(url).build();
        okHttpClient.newCall(request).enqueue(callback);
    }

    public static void updateDeviceStatus(String clientId, int port, boolean online, Callback callback) {
        String url = AGENT_SERVER + "/device/update";
        OkHttpClient okHttpClient = new OkHttpClient();
        RequestBody body = new FormBody.Builder()
                .add("clientid", clientId)
                .add("version", DeviceHelper.getAndroidVersion())
                .add("apilevel", String.valueOf(DeviceHelper.getAPILevel()))
                .add("agentver", "1.0-aosp")
                .add("port", String.valueOf(port))
                .add("online", (online)?("1"):("0"))
                .build();
        Request request = new Request.Builder().post(body).url(url).build();
        okHttpClient.newCall(request).enqueue(callback);
    }

}
