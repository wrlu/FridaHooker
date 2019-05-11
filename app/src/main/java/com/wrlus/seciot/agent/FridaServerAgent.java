package com.wrlus.seciot.agent;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class FridaServerAgent {
    private static final String AGENT_SERVER = "http://10.5.26.179:8080/SecIoT";
    private static final String FRIDA_DOWNLOAD_LINK = "https://github.com/frida/frida/releases/download/${version}/";
    private static final String FRIDA_SERVER_NAME = "frida-server-${version}-android-${abi}.xz";

    public static void getFridaVersionOnServer(Callback callback) {
        String url = AGENT_SERVER + "/agent/frida-version";
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().get().url(url).build();
        okHttpClient.newCall(request).enqueue(callback);
    }

    public static void downloadFrida(String version, String abi, Callback callback) {
        String url = FRIDA_DOWNLOAD_LINK.replace("${version}", version) +
                FRIDA_SERVER_NAME.replace("${version}", version).replace("${abi}", abi);
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().get().url(url).build();
        okHttpClient.newCall(request).enqueue(callback);
    }

}
