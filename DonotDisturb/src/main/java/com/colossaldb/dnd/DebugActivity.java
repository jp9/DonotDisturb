package com.colossaldb.dnd;

import android.app.Activity;
import android.os.Bundle;

/**
 * Created by Jayaprakash Pasala
 * Date:  12/12/13
 * Time:  10:59 PM
 */
public class DebugActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new DebugFragment())
                .commit();
    }
}
