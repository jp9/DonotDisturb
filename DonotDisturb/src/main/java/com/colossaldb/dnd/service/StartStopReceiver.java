package com.colossaldb.dnd.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;
import com.colossaldb.dnd.prefs.AppPreferences;

import java.util.Calendar;
import java.util.Date;

/**
 * Copyright (C) 2015 Jayaprakash Pasala
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
 *
 */
public class StartStopReceiver extends BroadcastReceiver {

    private static final long TIME_DELAY_TO_SET_RINGER = 200L; // In milli seconds
    private static final long TIME_DELAY_TO_ENABLE_RECEIVER = 3000L; // In milli seconds
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

        // If ringer was changed during the quiet period then note it.
        if (AudioManager.RINGER_MODE_CHANGED_ACTION.equals(intent.getAction())) {
            AppPreferences.getInstance().writeDebugEvent("StartStopReceiver",
                    "Intent info: pkg: " + intent.getPackage() + " datastr: " + intent.getDataString() +
                            "scheme: " + intent.getScheme() + " tostr:" + intent.toString() + " extras: " + intent.getExtras().toString());

            // Volume was manually adjusted during quiet time.
            if (result.second) //  && (System.currentTimeMillis() - LAST_CHANGE_TO_QUIET.get())> HACK_INTERVAL_TO_SKIP
                AppPreferences.getInstance().markRingerChangedManually();

            //Finished handling action - return here.
            return;
        }

        // Code written for readability ..
        if (intent.getBooleanExtra(AppPreferences.SETTINGS_CHANGED_KEY, false)
                && !result.second) {
            // Only the settings changed, and don't change the phone mode, unless we are in quiet period.
            AppPreferences.getInstance().writeDebugEvent("StartStopReceiver",
                    "Not calling dnd");
        } else {
            AppPreferences.getInstance().writeDebugEvent("StartStopReceiver",
                    "Executed  [" + (result.second ? "mute" : "unmute") + "] and set the alarm to run at scheduled time. Next run in: " + (result.first / 1000L) + " seconds");

            execDnd(context, audioManager, result.second);
        }
        reSchedule(context, result.first);
    }

    protected static synchronized void execDnd(final Context context, AudioManager audioManager, boolean silencePhone) {
        // We have to first disable the broadcast receiver for ringer change
        // or we will call this class again (and cannot distinguish between manual and automatic change).
        final ComponentName component = new ComponentName(context, StartStopReceiver.class);
        int status = context.getPackageManager().getComponentEnabledSetting(component);
        boolean disableStartStopReceiver = false;
        AppPreferences.getInstance().writeDebugEvent("StartStopReceiver", "PackageManager status :" + status);
        if (status == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || status == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
            disableStartStopReceiver = true;
            // Disable the broadcast receiver.
            context.getPackageManager().setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            AppPreferences.getInstance().writeDebugEvent("StartStopReceiver", "Disabled the start stop receiver");
        } else {
            AppPreferences.getInstance().writeDebugEvent("StartStopReceiver", "Not disabling the start stop receiver");
        }

        try {
            if (silencePhone)
                setToSilent(audioManager);
            else
                enableNormal(audioManager);
        } finally {
            if (disableStartStopReceiver) {
                // Enable the broadcast receiver
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        context.getPackageManager().setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                        AppPreferences.getInstance().writeDebugEvent("StartStopReceiver", "Enabled the start stop receiver");
                    }
                }, TIME_DELAY_TO_ENABLE_RECEIVER);
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
                ((currTime >= startTime) && (currTime < endTime)) :
                ((currTime >= startTime) || (currTime < endTime))) {
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
        intent.setAction(AppPreferences.BROADCAST_START_STOP_ACTION);

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
        // 10 Seconds of window - Give or take a few seconds.
        alarmManager.setWindow(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delay, 10000, pendingIntent);
        AppPreferences.getInstance().logNextRun("Next alarm: " + new Date(System.currentTimeMillis() + delay));
    }

    private static void setToSilent(final AudioManager am) {
        if (am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
            am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            // hack as hell for lollipop bug.
            if (android.os.Build.VERSION.SDK_INT == 21) {
                /**
                 * This is a serious not recommended hack. Unfortunately, if this removed the value of
                 * the app is lost in Lollipop.
                 * We have to call the silent twice or the volume is not silenced.
                 * Hate the hack but need it till Google fixes setRinger()
                 */
                AppPreferences.getInstance().writeDebugEvent("Ringer Silent", "Scheduled one more for Lollipop");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        AppPreferences.getInstance().writeDebugEvent("Ringer Silent", "Executed one more for Lollipop");
                        am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                    }
                }, TIME_DELAY_TO_SET_RINGER);
            }
            //LAST_CHANGE_TO_QUIET.set(System.currentTimeMillis());
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

