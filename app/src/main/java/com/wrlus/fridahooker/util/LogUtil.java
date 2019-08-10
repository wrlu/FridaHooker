package com.wrlus.fridahooker.util;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class LogUtil {
    private static final String TAG = "LogUtil";
    private static LogUtil instance = null;

    private boolean isDebuggable = true;
    private LogUtil() {
        Log.d(TAG, "Create LogUtil singleton");
    }
    public static LogUtil getInstance() {
        if (instance == null) {
            synchronized (LogUtil.class) {
                if (instance == null) {
                    instance = new LogUtil();
                }
            }
        }
        return instance;
    }

    public void setDebuggable(boolean debuggable) {
        isDebuggable = debuggable;
    }

    public void debug(String tag, String msg) {
        if (tag == null || msg == null) {
            error(TAG, "debug(): parameter is null");
            return;
        }
        if (isDebuggable) {
            Log.d(tag, msg);
        }
    }

    public void info(String tag, String msg) {
        if (tag == null || msg == null) {
            error(TAG, "info(): parameter is null");
            return;
        }
        Log.i(tag, msg);
    }

    public void warn(String tag, String msg) {
        if (tag == null || msg == null) {
            error(TAG, "warn(): parameter is null");
            return;
        }
        Log.w(tag, msg);
    }

    public void error(String tag, String msg) {
        if (tag == null || msg == null) {
            error(TAG, "error(): parameter is null");
            return;
        }
        Log.e(tag, msg);
    }

    public void error(String tag, Throwable e) {
        if (tag == null || e == null) {
            error(TAG, "error(): parameter is null");
            return;
        }
        Log.e(tag, e.getClass().getName());
        if (isDebuggable) {
            e.printStackTrace();
        }
    }

    public void toast(Context context, String msg, Object details) {
        if (msg == null) {
            error(TAG, "toast(): parameter is null");
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
