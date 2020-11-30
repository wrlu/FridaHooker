package com.wrlus.fridahooker.shell;

public class NativeShell {

    static {
        System.loadLibrary("fridahooker");
    }

    public static native int execute(String cmd);
}
