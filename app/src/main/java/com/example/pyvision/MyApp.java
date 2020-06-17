package com.example.pyvision;

import android.app.Application;

/**
 * Created by zale.zhang on 2020/6/9.
 *
 * @author zale.zhang
 */
public class MyApp extends Application {

    private static MyApp app;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
    }

    public static MyApp getInstance() {
        return app;
    }
}
