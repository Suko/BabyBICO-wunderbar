package com.babybico.relayr;

import android.app.Application;

public class BabyBicoApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        RelayrSdkInitializer.initSdk(this);
    }
}
