package com.dong.plugin;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity {

    private static final String TAG = "dong";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, String.format("MainActivity/onCreate:thread(%s)",Thread.currentThread().getName()));
        new Test().print();
    }
}
