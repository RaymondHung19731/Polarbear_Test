package com.ultraflymodel.polarbear.ultraflymodel;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.Application;

import com.activeandroid.ActiveAndroid;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;
import com.ultraflymodel.polarbear.common.HILog;

import net.ralphpina.permissionsmanager.PermissionsManager;


public class UltraflyModelApplication extends Application {

    private static String TAG = UltraflyModelApplication.class.getSimpleName();
    private static UltraflyModelApplication mInstance;
    public static Bus bus;


    public static synchronized UltraflyModelApplication getInstance() {
        HILog.d(TAG, "getInstance:");
        return mInstance;
    }

    public void onCreate() {
        HILog.d(TAG, "onCreate:");
        super.onCreate();

        mInstance = this;
        bus = new Bus(ThreadEnforcer.ANY);

        ActiveAndroid.initialize(this);
        PermissionsManager.init(this);
    }


    @Override
    public void onLowMemory() {
        HILog.d(TAG, "On Low Memory!!!!");
        MemoryInfo mi = new MemoryInfo();

        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);

        long availableMegs = mi.availMem / 1048576L;
        long thresholdMegs = mi.threshold / 1048576L;

        HILog.d(TAG, "availableMegs:" + availableMegs + " thresholdMegs:" + thresholdMegs);
        super.onLowMemory();
    }

    

}
