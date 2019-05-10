package com.wrlus.seciot.hook;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class FridaServerAgent {
    private static OkHttpClient okHttpClient = new OkHttpClient();
    private static final String AGENT_SERVER = "http://10.5.26.179:8080/SecIoT";
    private static final String FRIDA_DOWNLOAD_LINK = "https://github.com/frida/frida/releases/download/${version}/";
    private static final String FRIDA_ARM_FILE_NAME = "frida-server-${version}-android-arm.xz";
    private static final String FRIDA_ARM64_FILE_NAME = "frida-server-${version}-android-arm64.xz";
    private static final String FRIDA_X86_FILE_NAME = "frida-server-${version}-android-x86.xz";
    private static final String FRIDA_X86_64_FILE_NAME = "frida-server-${version}-android-x86_64.xz";

    public static void getFridaVersionOnServer(Callback callback) {
        Request request = new Request.Builder().get().url(AGENT_SERVER + "/agent/frida-version").build();
        okHttpClient.newCall(request).enqueue(callback);
    }

}
