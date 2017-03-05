package com.colossaldb.dnd.service;

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
        long startTime = System.nanoTime();
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        Log.i("PhoneStateBcastRcvr", " State = " + state);
        AppPreferences.getInstance().writeDebugEvent("BroadcastReceiver", "Received intent");
        if (shouldSkip())
            return;

        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            AppPreferences.getInstance().writeDebugEvent("BroadcastReceiver", "Got ringing event");
            String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

            Pair<Boolean, Boolean> contactOrSecondCall = isContactOrSecondCall(context, number);
            boolean isContact = contactOrSecondCall.first;
            boolean isSecondMissedCall = contactOrSecondCall.second;

            AppPreferences.getInstance().writeDebugEvent("BroadcastReceiver", "Is Contact [" + isContact + "] Is Second Missed call [" + isSecondMissedCall + "]");
            if ((isContact && AppPreferences.getInstance().ringForContacts())
                    || (isSecondMissedCall && AppPreferences.getInstance().ringOnRepeatCall())) {
                logUnmutingRinger(isContact, isSecondMissedCall);
                // Enable the ringer.
                StartStopReceiver.execDnd(context,
                        (AudioManager) context.getSystemService(Context.AUDIO_SERVICE),
                        false);
            } else {
                AppPreferences.getInstance().writeDebugEvent("BroadcastReceiver", "Not unmuting... condition not satisfied");
            }
        } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            AppPreferences.getInstance().writeDebugEvent("BroadcastReceiver", "State Idle. Resetting ringer.");
            StartStopReceiver.execDnd(context,
                    (AudioManager) context.getSystemService(Context.AUDIO_SERVICE),
                    StartStopReceiver.getDelay(AppPreferences.getInstance()).second);
        }
        long endTime = System.nanoTime();
        AppPreferences.getInstance().writeDebugEvent("BroadcastReceiver", "Total time for execution [ms] : " + ((endTime - startTime) / 1000000L));
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
        boolean isAppEnabled = AppPreferences.getInstance().isEnabled();
        boolean areOptionsEnabled = (AppPreferences.getInstance().ringOnRepeatCall() || AppPreferences.getInstance().ringForContacts());
        boolean inQuietPeriod = (StartStopReceiver.getDelay(AppPreferences.getInstance()).second);
        boolean manualRingerChange = (AppPreferences.getInstance().isRingerChangedManually());

        boolean shouldSkip = !isAppEnabled  // App is not enabled.
                || !areOptionsEnabled // Options are not enabled
                || !inQuietPeriod // Not in quiet period
                || manualRingerChange; // If ringer is changed, then we leave the user's preference in place
        AppPreferences.getInstance().writeDebugEvent("Should skip event:[" + shouldSkip + "]", "Is app enabled [" + isAppEnabled + "], are options enabled: ["
                + areOptionsEnabled + "], inQuietPeriod : [" + inQuietPeriod + "], manualRingerChange [" + manualRingerChange + "]");
        return shouldSkip;
    }

    /**
     * Is the phone call from a contact or is it a second call from same number in last 10 mins.
     */
    private Pair<Boolean, Boolean> isContactOrSecondCall(Context context, String number) {
        boolean isContact = false;
        boolean isSecondMissedCall = false;
        {
            long cutOffTime = System.currentTimeMillis() - 600 * 1000L;
            try {
                Cursor c = context.getContentResolver().query(
                        CallLog.Calls.CONTENT_URI, new String[]{CallLog.Calls.NUMBER},
                        CallLog.Calls.DATE + ">= ? AND " + CallLog.Calls.NUMBER + " = ? ", new String[]{cutOffTime + "", number},
                        CallLog.Calls.DEFAULT_SORT_ORDER);
                isSecondMissedCall = (c != null && c.moveToNext());
                closeQuietly(c);

                Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
                c = context.getContentResolver().query(uri, new String[]{ContactsContract.PhoneLookup.NUMBER}, null, null, null);
                isContact = (c != null && c.moveToNext());
                closeQuietly(c);
            } catch(SecurityException e) {
                AppPreferences.getInstance().writeDebugEvent("Ringer", "The user has not given us the permission to check contact or second call. Skipping unmute stuff");
            }
        }
        return new Pair<>(isContact, isSecondMissedCall);
    }

    void closeQuietly(Cursor c) {
        try {
            if (c != null)
                c.close();
        } catch (Exception ignored) {
        }
    }
}
