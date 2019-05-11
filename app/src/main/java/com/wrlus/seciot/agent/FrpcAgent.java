package com.wrlus.seciot.agent;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class FrpcAgent {
    private static final String AGENT_SERVER = "http://10.5.26.179:8080/SecIoT";
    private static final String FRP_DOWNLOAD_LINK = "https://github.com/fatedier/frp/releases/download/v${version}/";
    private static final String FRP_NAME = "frp_${version}_linux_${abi}.tar.gz";

    public static void getFrpsVersionOnServer(Callback callback) {
        String url = AGENT_SERVER + "/agent/frps-version";
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().get().url(url).build();
        okHttpClient.newCall(request).enqueue(callback);
    }

    public static void downloadFrida(String version, String abi, Callback callback) {
        String url = FRP_DOWNLOAD_LINK.replace("${version}", version) +
                FRP_NAME.replace("${version}", version).replace("${abi}", abi);
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().get().url(url).build();
        okHttpClient.newCall(request).enqueue(callback);
    }
}
