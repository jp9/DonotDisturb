package com.colossaldb.dnd.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;

import com.colossaldb.dnd.prefs.AppPreferences;

import java.util.Calendar;

/**
 * Created by Jayaprakash Pasala on 12/10/13.
 *
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

            return;
        }

        long delay = execDnd(context, pref);
        reSchedule(context, delay);
    }

    protected static long execDnd(Context context, AppPreferences pref) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        Pair<Long, Boolean> result = getDelay(pref);
        if (result.second)
            setToSilent(audioManager);
        else
            enableNormal(audioManager);

        return result.first;
    }

    /**
     * Get the delay from now and what action needs to taken now.
     *
     * @param pref - Application preference
     * @return - Delay in Milli Seconds and the action to take.
     * true - silence the phone.
     * false - restore the volume on phone.
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
    }

    static void setToSilent(AudioManager am) {
        if (am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
            am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            Log.i("StartStopReceiver", "Ringer is made silent");
        } else {
            Log.i("StartStopReceiver", "Ringer is already Normal");
        }
    }

    static void enableNormal(AudioManager am) {
        if (am.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
            am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            Log.i("StartStopReceiver", "Ringer is made normal");
        } else {
            Log.i("StartStopReceiver", "Ringer is already silent");
        }
    }
}

