package com.dong.plugin;

import android.util.Log;

/**
 * Created by dongjiangpeng on 2019/8/19 0019.
 */
public class MyLog {

    private static final String TAG = "dong";

    public static void log(String message) {
        Log.d(TAG, String.format("MyLog/log:thread(%s) msg(%s)",Thread.currentThread().getName(),message));
    }
}
