package com.colossaldb.dnd.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.provider.CallLog;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.colossaldb.dnd.MyApp;
import com.colossaldb.dnd.prefs.AppPreferences;

/**
 * Created by Jayaprakash Pasala
 * Date:  12/11/13
 * Time:  10:58 PM
 */
public class PhoneStateBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        AppPreferences.initialize(context.getApplicationContext());
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        Log.i("PhoneStateBroadcastReceiver", " State = " + state);
        if (!AppPreferences.getInstance().isEnabled() ||
                !(AppPreferences.getInstance().ringOnRepeatCall() && AppPreferences.getInstance().ringForContacts()))
            return;

        if ("RINGING".equals(state)) {
            String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            Log.i("PhoneStateBroadcastReceiver", " Number = " + number);

            boolean isContact = false;
            boolean isSecondMissedCall = false;
            Uri uri = Uri.withAppendedPath(CallLog.Calls.CONTENT_FILTER_URI, Uri.encode(number));
            if (uri != null) {
                Cursor c = MyApp.getAppContext().getContentResolver().query(uri, null,
                        null, null, CallLog.Calls.DEFAULT_SORT_ORDER);
                long cutOffTime = System.currentTimeMillis() - 600 * 1000L;
                if (c != null) {
                    while (c.moveToNext()) {
                        String num = c.getString(c.getColumnIndex(CallLog.Calls.NUMBER));// for  number
                        String name = c.getString(c.getColumnIndex(CallLog.Calls.CACHED_NAME));// for name
                        if (num != null && name != null) {
                            // This is a contact
                            isContact = true;
                        }

                        long callTime = Long.parseLong(c.getString(c.getColumnIndex(CallLog.Calls.DATE)));
                        if (callTime <= cutOffTime) {
                            break;
                        }

                        int type = Integer.parseInt(c.getString(c.getColumnIndex(CallLog.Calls.TYPE)));// Type - we need only outgoing
                        if (!(CallLog.Calls.MISSED_TYPE == type || CallLog.Calls.INCOMING_TYPE == type))
                            continue;

                        isSecondMissedCall = true;
                    }
                }
            }

            if ((isContact && AppPreferences.getInstance().ringForContacts())
                    || (isSecondMissedCall && AppPreferences.getInstance().ringOnRepeatCall())) {
                AudioManager audioManager = (AudioManager) MyApp.getAppContext().getSystemService(Context.AUDIO_SERVICE);
                StartStopReceiver.enableNormal(audioManager);
            }
        } else if ("IDLE".equals(state)) {
            StartStopReceiver.execDnd(MyApp.getAppContext(), AppPreferences.getInstance());
        }
    }
}
