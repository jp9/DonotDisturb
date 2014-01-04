package com.colossaldb.dnd.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;
import com.colossaldb.dnd.prefs.AppPreferences;

import java.util.Calendar;
import java.util.Date;

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
 * This broadcast receiver is called
 *  - When the phone boots
 *  - When the user enables the app.
 *  - The scheduled call from this receiver itself.
 *
 *  The decision to start/stop the muting of the phone is handled here.
 */

/**
 * Created by Jayaprakash Pasala on 12/10/13.
 */
public class StartStopReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        AppPreferences pref = AppPreferences.getInstance();
        if (!pref.isEnabled()) {
            // The application is not in play. Shutdown.
            // Cancel the Intents if any
            Intent futureIntent = new Intent(context.getApplicationContext(), StartStopReceiver.class);
            futureIntent.setAction("com.colossaldb.dnd.START_STOP");
            PendingIntent.getBroadcast(
                    context,
                    0,
                    futureIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT);

            AppPreferences.getInstance().writeDebugEvent("App Disabled", "Canceled intents.");
            AppPreferences.getInstance().clearRingerChangedManually();
            return;
        }

        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        Pair<Long, Boolean> result = getDelay(pref);

        if (AudioManager.RINGER_MODE_CHANGED_ACTION.equals(intent.getAction())) {
            // Volume was manually adjusted during quiet time.
            if (result.second)
                AppPreferences.getInstance().markRingerChangedManually();
            return;
        }

        // Code written for readability ..
        if (intent.getBooleanExtra("SettingsSaved", false)
                && !result.second) {
            // Only the settings changed, and don't change the phone mode, unless we are in quiet period.
        } else {
            execDnd(context, audioManager, result.second);
        }
        AppPreferences.getInstance().writeDebugEvent("StartStopReceiver", "Executed mute/unmute and set the alarm to run at scheduled time");
        reSchedule(context, result.first);
    }

    protected static synchronized void execDnd(Context context, AudioManager audioManager, boolean silencePhone) {
        // We have to first disable the broadcast receiver for ringer change
        // or we will call this class again (and cannot distinguish between manual and automatic change).
        ComponentName component = new ComponentName(context, StartStopReceiver.class);
        int status = context.getPackageManager().getComponentEnabledSetting(component);
        boolean disableStartStopReceiver = false;
        if (status == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || status == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
            disableStartStopReceiver = true;
            // Disable the broadcast receiver.
            context.getPackageManager().setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }

        try {
            if (silencePhone)
                setToSilent(audioManager);
            else
                enableNormal(audioManager);
        } finally {
            if (disableStartStopReceiver) {
                // Enable the broadcast receiver
                context.getPackageManager().setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            }
        }
    }

    /**
     * Get the delay from now and what action needs to taken now.
     *
     * @param pref - Application preference
     * @return - Delay in Milli Seconds and the action to take.
     *         true - silence the phone.
     *         false - restore the volume on phone.
     */
    protected static Pair<Long, Boolean> getDelay(AppPreferences pref) {
        long delay;
        boolean shouldSilence;

        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int min = cal.get(Calendar.MINUTE);
        int currTime = hour * 60 + min;

        int startHour = pref.getStartHour(24);
        int startMin = pref.getStartMinute(60);
        int endHour = pref.getEndHour(-1);
        int endMin = pref.getEndMinute(-1);

        int startTime = startHour * 60 + startMin;
        int endTime = endHour * 60 + endMin;

        /**
         * Two cases to consider:
         *     case a: 10 pm to 6 am.
         *     case b: 10 am to 5 pm.
         */
        if (endTime > startTime ?
                ((currTime > startTime) && (currTime < endTime)) :
                ((currTime > startTime) || (currTime < endTime))) {
            shouldSilence = true;
            delay = (endTime - (hour * 60 + min)) * 60 * 1000L;
        } else {
            // Set the phone to normal
            shouldSilence = false;
            delay = (startTime - (hour * 60 + min)) * 60 * 1000L;
        }

        if (delay < 0) {
            delay = delay + 1440 * 60 * 1000L;
        }

        Log.i("StartStopReceiver", "Will run in : " + (delay / 60000) + " minutes ");
        return new Pair<Long, Boolean>(delay, shouldSilence);
    }


    /**
     * Reschedule to start or stop the DND
     *
     * @param context - Context
     */
    private synchronized void reSchedule(Context context, long delay) {
        Intent intent = new Intent(context.getApplicationContext(), StartStopReceiver.class);
        intent.setAction("com.colossaldb.dnd.START_STOP");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_NO_CREATE);
        if (pendingIntent == null) {
            pendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }

        AlarmManager alarmManager = (AlarmManager) (context.getSystemService(Context.ALARM_SERVICE));
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delay, pendingIntent);
        AppPreferences.getInstance().logNextRun("Next alarm: " + new Date(System.currentTimeMillis() + delay));
    }

    private static void setToSilent(AudioManager am) {
        if (am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
            am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            AppPreferences.getInstance().writeDebugEvent("Ringer Silent", "Ringer set to silent");
            Log.i("StartStopReceiver", "Ringer is made silent");
        } else {
            Log.i("StartStopReceiver", "Ringer is already silent");
        }
    }

    private static void enableNormal(AudioManager am) {
        // Clear these flags..
        AppPreferences.getInstance().clearRingerChangedManually();
        if (am.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
            am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            Log.i("StartStopReceiver", "Ringer is made normal");
            AppPreferences.getInstance().writeDebugEvent("Ringer normal", "Ringer set to normal");
        } else {
            Log.i("StartStopReceiver", "Ringer is already set to normal");
        }
    }
}

