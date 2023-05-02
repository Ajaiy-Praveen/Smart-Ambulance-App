package me.ajaybala.ambulanceapp;

import android.app.Application;

import com.fxn.stash.Stash;

public class MapApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Stash.init(this);
    }
}
