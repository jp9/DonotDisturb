package com.colossaldb.dnd.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.SystemClock;
import android.util.Log;

import java.util.Calendar;

import prefs.AppPreferences;

/**
 * Created by Jayaprakash Pasala on 12/10/13.
 */
public class StartStopService extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        AppPreferences.initialize(context);
        AppPreferences pref = AppPreferences.getInstance();
        if (!pref.isEnabled()) {
            // The application is not in play. Shutdown.
            return;
        }

        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int min = cal.get(Calendar.MINUTE);

        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        long delay;
        int startHour = pref.getStartHour(24);
        int startMin = pref.getStartMinute(60);
        int endHour = pref.getEndHour(-1);
        int endMin = pref.getEndMinute(-1);
        if ((hour >= startHour && min >= startMin) ||
                (hour <= endHour && min < endMin)) {
            // Silence the phone
            setToSilent(audioManager);
            delay = ((24 + endHour) * 60 + endMin - (hour * 60 + min)) * 60 * 1000L;
        } else {
            // Set the phone to normal
            enableNormal(audioManager);
            delay = ((startHour + startMin) * 60 - (hour * 60 + min)) * 60 * 1000L;
        }

        Log.i("StartStopService", "Will run in : " + (delay / 60000) + " minutes ");
        reSchedule(context, delay);

    }

    /**
     * Reschedule to start or stop the DND
     *
     * @param context - Context
     */
    private synchronized void reSchedule(Context context, long delay) {
        Intent intent = new Intent(context.getApplicationContext(), StartStopService.class);
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
    }

    private void setToSilent(AudioManager am) {
        if (am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
            am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        }
    }

    private void enableNormal(AudioManager am) {
        if (am.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
            am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        }
    }
}

