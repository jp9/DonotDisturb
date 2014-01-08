package com.colossaldb.dnd.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Pair;
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
        if (shouldSkip())
            return;

        if ("RINGING".equals(state)) {
            String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

            Pair<Boolean, Boolean> contactOrSecondCall = isContactOrSecondCall(number);
            boolean isContact = contactOrSecondCall.first;
            boolean isSecondMissedCall = contactOrSecondCall.second;

            if ((isContact && AppPreferences.getInstance().ringForContacts())
                    || (isSecondMissedCall && AppPreferences.getInstance().ringOnRepeatCall())) {
                logUnmutingRinger(isContact, isSecondMissedCall);
                // Enable the ringer.
                StartStopReceiver.execDnd(context,
                        (AudioManager) MyApp.getAppContext().getSystemService(Context.AUDIO_SERVICE),
                        false);
            }
        } else if ("IDLE".equals(state)) {
            AppPreferences.getInstance().writeDebugEvent("Ringer", "Resetting ringer.");
            StartStopReceiver.execDnd(MyApp.getAppContext(),
                    (AudioManager) MyApp.getAppContext().getSystemService(Context.AUDIO_SERVICE),
                    StartStopReceiver.getDelay(AppPreferences.getInstance()).second);
        }
    }

    /**
     * Log : The ringer is unmuted.
     */
    private void logUnmutingRinger(boolean isContact, boolean isSecondMissedCall) {
        AppPreferences.getInstance().writeDebugEvent("Ringer", "Unmuting ringer: isContact: ["
                + isContact + "] isSecondMissedCall [" + isSecondMissedCall + "]");
    }

    /**
     * Whether we should skip trying to mute or unmute the ringer.
     */
    private boolean shouldSkip() {
        return !AppPreferences.getInstance().isEnabled() || // App is not enabled.
                !(AppPreferences.getInstance().ringOnRepeatCall() || AppPreferences.getInstance().ringForContacts()) // Options are not enabled
                || !(StartStopReceiver.getDelay(AppPreferences.getInstance()).second) // Not in quiet period
                || (AppPreferences.getInstance().isRingerChangedManually()); // If ringer is changed, then we leave the user's preference in place
    }

    /**
     * Is the phone call from a contact or is it a second call from same number in last 10 mins.
     */
    private Pair<Boolean, Boolean> isContactOrSecondCall(String number) {
        boolean isContact;
        boolean isSecondMissedCall;
        {
            long cutOffTime = System.currentTimeMillis() - 600 * 1000L;
            Cursor c = MyApp.getAppContext().getContentResolver().query(
                    CallLog.Calls.CONTENT_URI, new String[]{CallLog.Calls.NUMBER},
                    CallLog.Calls.DATE + ">= ? AND " + CallLog.Calls.NUMBER + " = ? ", new String[]{cutOffTime + "", number},
                    CallLog.Calls.DEFAULT_SORT_ORDER);
            isSecondMissedCall = (c != null && c.moveToNext());
            closeQuietly(c);

            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
            c = MyApp.getAppContext().getContentResolver().query(uri, new String[]{ContactsContract.PhoneLookup.NUMBER}, null, null, null);
            isContact = (c != null && c.moveToNext());
            closeQuietly(c);
        }
        return new Pair<Boolean, Boolean>(isContact, isSecondMissedCall);
    }

    void closeQuietly(Cursor c) {
        try {
            if (c != null)
                c.close();
        } catch (Exception ignored) {
        }
    }
}
