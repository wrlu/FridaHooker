package com.wrlus.fridahooker.util;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class LogUtil {
    private static final String TAG = "LogUtil";
    private static boolean isDebuggable = true;

    public static void setDebuggable(boolean debuggable) {
        isDebuggable = debuggable;
    }

    public static void d(String tag, String msg) {
        if (tag == null || msg == null) {
            LogUtil.e(TAG, "debug(): parameter is null");
            return;
        }
        if (isDebuggable) {
            Log.d(tag, msg);
        }
    }

    public static void i(String tag, String msg) {
        if (tag == null || msg == null) {
            LogUtil.e(TAG, "info(): parameter is null");
            return;
        }
        Log.i(tag, msg);
    }

    public static void w(String tag, String msg) {
        if (tag == null || msg == null) {
            LogUtil.e(TAG, "warn(): parameter is null");
            return;
        }
        Log.w(tag, msg);
    }

    public static void e(String tag, String msg) {
        if (tag == null || msg == null) {
            LogUtil.e(TAG, "error(): parameter is null");
            return;
        }
        Log.e(tag, msg);
    }

    public static void e(String tag, Throwable e) {
        if (tag == null || e == null) {
            LogUtil.e(TAG, "error(): parameter is null");
            return;
        }
        Log.e(tag, e.getClass().getName());
        if (isDebuggable) {
            e.printStackTrace();
        }
    }

    public static void t(Context context, String msg, Object details) {
        if (msg == null) {
            LogUtil.e(TAG, "toast(): parameter is null");
            return;
        }
        String text = msg;
        if (isDebuggable && details != null) {
            if (details instanceof String) {
                text = msg + "\nToast Details：\n" + details;
            } else if (details instanceof Throwable) {
                text = msg + "\nToast Details：\n" + ((Throwable) details).getLocalizedMessage();
            } else {
                text = msg + "\nToast Details：\n" + details.toString();
            }
        }
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }
}
