package com.wrlus.fridahooker.agent.shell;

public class NativeRootShell {

    static {
        System.loadLibrary("fridahooker");
    }

    public static native int execute(String cmd);
}
