package com.colossaldb.dnd;

import android.app.Application;
import android.content.Context;

import com.colossaldb.dnd.prefs.AppPreferences;

/**
 * Copyright (C) 2013  Jayaprakash Pasala
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
/**
 * Created by Jayaprakash Pasala
 * Date:  12/11/13
 * Time:  4:01 PM
 *
 * Instantiate the single AppPreferences class here and get easy access to application context.
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
