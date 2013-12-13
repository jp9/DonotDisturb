package com.colossaldb.dnd;

import android.app.Application;
import android.content.Context;

import com.colossaldb.dnd.prefs.AppPreferences;

/**
 * Created by Jayaprakash Pasala
 * Date:  12/11/13
 * Time:  4:01 PM
 */
public class MyApp extends Application {
    private static volatile Context context;

    public void onCreate() {
        super.onCreate();
        MyApp.context = getApplicationContext();
        AppPreferences.initialize(MyApp.context);
    }

    public static Context getAppContext() {
        return MyApp.context;
    }
}
