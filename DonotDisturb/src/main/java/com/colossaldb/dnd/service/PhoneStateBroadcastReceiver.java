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
 *
 * Phone State broad cast receiver.
 *  - Decisions to unmute the phone or not is tackled here.
 *      - First check whether the app is enabled or not.
 *      - Next verify that the options are turned on
 *      - Verify that we are actually in the quiet time period.
 */
/**
 * Created by Jayaprakash Pasala
 * Date:  12/11/13
 * Time:  10:58 PM
 */
public class PhoneStateBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        Log.i("PhoneStateBroadcastReceiver", " State = " + state);
        if (!AppPreferences.getInstance().isEnabled() ||
                !(AppPreferences.getInstance().ringOnRepeatCall() && AppPreferences.getInstance().ringForContacts())
                || !(StartStopReceiver.getDelay(AppPreferences.getInstance()).second))
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
                AppPreferences.getInstance().writeDebugEvent("Enabling Ringer Volume", "isContact: ["
                        + isContact + "] isSecondMissedCall [" + isSecondMissedCall + "]");
                AudioManager audioManager = (AudioManager) MyApp.getAppContext().getSystemService(Context.AUDIO_SERVICE);
                StartStopReceiver.enableNormal(audioManager);
            }
        } else if ("IDLE".equals(state)) {
            StartStopReceiver.execDnd(MyApp.getAppContext(), AppPreferences.getInstance());
        }
    }
}
