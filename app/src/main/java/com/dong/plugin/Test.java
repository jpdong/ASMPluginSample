package com.dong.plugin;


import android.util.Log;

/**
 * Created by dongjiangpeng on 2019/8/16 0016.
 */
public class Test {

    private static final String TAG = "dong";

    public void print() {
        printOne();
        printOne();
    }

    private void printOne() {
        //System.out.println("printOne");
        Log.d(TAG,"Test/printOne:");
    }
}
