package com.dormigo;

import android.app.Application;

public class DormigoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ApiClient.init(getApplicationContext());
    }
}
