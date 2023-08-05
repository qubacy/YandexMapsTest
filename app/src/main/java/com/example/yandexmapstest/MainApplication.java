package com.example.yandexmapstest;

import android.app.Application;

import com.yandex.mapkit.MapKitFactory;

public class MainApplication extends Application {
    private static final String C_API_KEY = "9c7983ec-3e4c-4cfe-8d28-516d317d53f0";

    @Override
    public void onCreate() {
        super.onCreate();

        MapKitFactory.setApiKey(C_API_KEY);
    }
}
