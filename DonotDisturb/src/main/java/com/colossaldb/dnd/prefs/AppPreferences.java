package com.colossaldb.dnd.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
 */

/**
 * Created by Jayaprakash Pasala
 * Date:  12/10/13
 * Time:  10:28 AM
 * <p/>
 * A singleton class representing the options used by the application (and also has helper methods to write and read debug logs).
 */
public class AppPreferences {
    private static final String PREFERENCE_NAME = "AppPreferences";
    private static final String DEBUG_PREF_NAME = "DebugAppInfo";

    private static final int MAX_LIST_SIZE = 20;

    // Keys
    private static final String START_HOUR = "sh";
    private static final String START_MIN = "sm";
    private static final String END_HOUR = "eh";
    private static final String END_MIN = "em";
    private static final String DND_ENABLED = "dnd_enabled";
    private static final String RING_ON_REPEAT = "ring_on_repeat";
    private static final String RING_FOR_CONTACTS = "ring_for_contacts";
    public static final String NEXT_ALARM_KEY = "NextAlarm";
    public static final String ERROR_EVENTS_KEY = "ErrorEvents";
    public static final String DEBUG_EVENTS_KEY = "DebugEvents";
    public static final String TITLE_KEY = "title";
    public static final String DETAIL_KEY = "detail";
    public static final String TIMESTAMP_KEY = "ts";

    private static volatile AppPreferences INSTANCE = null;

    private SharedPreferences preferences;
    private SharedPreferences debugPreferences;

    /**
     * Expected to be called only once during the full lifecycle.
     *
     * @param appContext - Application context
     */
    public static synchronized void initialize(Context appContext) {
        INSTANCE = new AppPreferences(appContext);
    }

    public static AppPreferences getInstance() {
        return INSTANCE;
    }

    private AppPreferences(Context appContext) {
        preferences = appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        debugPreferences = appContext.getSharedPreferences(DEBUG_PREF_NAME, Context.MODE_PRIVATE);
    }

    public int getStartHour(int defValue) {
        return preferences.getInt(START_HOUR, defValue);
    }

    public int getStartMinute(int defValue) {
        return preferences.getInt(START_MIN, defValue);
    }

    public int getEndHour(int defValue) {
        return preferences.getInt(END_HOUR, defValue);
    }

    public int getEndMinute(int defValue) {
        return preferences.getInt(END_MIN, defValue);
    }

    public boolean isEnabled() {
        return preferences.getBoolean(DND_ENABLED, false);
    }

    public boolean ringOnRepeatCall() {
        return preferences.getBoolean(RING_ON_REPEAT, false);
    }

    public void setStartTime(int startHour, int startMin) {
        setTimes(START_HOUR, startHour, START_MIN, startMin);
    }

    public void setEndTime(int endHour, int endMin) {
        setTimes(END_HOUR, endHour, END_MIN, endMin);
    }

    public boolean ringForContacts() {
        return preferences.getBoolean(RING_FOR_CONTACTS, false);
    }

    private void setTimes(String hourKey, int hour, String minKey, int mins) {
        SharedPreferences.Editor e = preferences.edit();
        e.putInt(hourKey, hour);
        e.putInt(minKey, mins);
        e.apply();
    }

    public void save(boolean dndEnabled, int startHour, int startMin, int endHour, int endMin,
                     boolean ringOnRepeat, boolean ringForContacts) {
        assert startHour >= 0 && startHour < 24;
        assert endHour >= 0 && endHour < 24;
        assert startMin >= 0 && startMin < 60;
        assert endMin >= 0 && endMin < 60;

        SharedPreferences.Editor edit = preferences.edit();
        edit.putBoolean(DND_ENABLED, dndEnabled);
        edit.putInt(START_HOUR, startHour);
        edit.putInt(START_MIN, startMin);
        edit.putInt(END_HOUR, endHour);
        edit.putInt(END_MIN, endMin);
        edit.putBoolean(RING_ON_REPEAT, ringOnRepeat);
        edit.putBoolean(RING_FOR_CONTACTS, ringForContacts);
        edit.apply();
    }

    public CharSequence getFormattedStartTime() {
        return String.format("%2d:%02d ", INSTANCE.getStartHour(-1), INSTANCE.getStartMinute(-1));
    }

    public CharSequence getFormattedEndTime() {
        return String.format("%2d:%02d ", INSTANCE.getEndHour(-1), INSTANCE.getEndMinute(-1));
    }

    /**
     * Write the debug events that have already occurred.
     *
     * @param title  - title of the event
     * @param detail - event details
     */
    public void writeDebugEvent(String title, String detail) {
        try {
            writeToPrefList(debugPreferences, DEBUG_EVENTS_KEY, title, detail);
        } catch (JSONException ignored) {
            Log.e("AppPreferences", "Title [" + title + "] and detail [" + detail + "]", ignored);
        }
    }

    public JSONArray getDebugEvents() {
        return getJSONArray(debugPreferences, DEBUG_EVENTS_KEY);
    }

    /**
     * Write the error event.
     *
     * @param title  - title
     * @param detail - detail
     */
    public void writeErrorEvent(String title, String detail) {
        try {
            writeToPrefList(debugPreferences, ERROR_EVENTS_KEY, title, detail);
        } catch (JSONException ignored) {
            Log.e("AppPreferences", "Title [" + title + "] and detail [" + detail + "]", ignored);
        }
    }

    public JSONArray getErrorEvents() {
        return getJSONArray(debugPreferences, ERROR_EVENTS_KEY);
    }

    /**
     * Store the details when the next scheduled event is going to run.
     *
     * @param details - details
     */
    public void logNextRun(String details) {
        debugPreferences.edit().putString(NEXT_ALARM_KEY, details).apply();
    }

    /**
     * @return The next scheduled run details.
     */
    public String getNextScheduleRun() {
        return debugPreferences.getString(NEXT_ALARM_KEY, "");
    }

    private static void writeToPrefList(SharedPreferences preferences, String listKey, String title, String detail) throws JSONException {
        JSONArray events = getJSONArray(preferences, listKey);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(TIMESTAMP_KEY, new Date(System.currentTimeMillis()).toString());
        jsonObject.put(TITLE_KEY, title);
        jsonObject.put(DETAIL_KEY, detail);
        events.put(jsonObject);

        if (events.length() > MAX_LIST_SIZE) {
            JSONArray newArray = new JSONArray();
            for (int i = events.length() - MAX_LIST_SIZE; i < MAX_LIST_SIZE; i++) {
                newArray.put(events.get(i));
            }
            events = newArray;
        }

        preferences.edit().putString(listKey, events.toString()).apply();
    }

    private static JSONArray getJSONArray(SharedPreferences preferences, String listKey) {
        JSONArray events;
        String eventStr = preferences.getString(listKey, null);
        if (eventStr == null) {
            events = new JSONArray();
        } else {
            try {
                events = new JSONArray(eventStr);
            } catch (JSONException ignored) {
                // Cannot do much...
                Log.e("AppPreferences", "Error: " + ignored.getMessage(), ignored);
                events = new JSONArray();
            }
        }
        return events;
    }

    public void markRingerChangedManually() {
        SharedPreferences.Editor ed = preferences.edit();
        ed.putBoolean("VOLUME_MANUALLY_CHANGED", true);
        ed.putLong("VOLUME_MANUALLY_CHANGED_TIME", System.currentTimeMillis());
        ed.apply();
        writeDebugEvent("Manual volume change", "Manual volume change during quiet period. The app will not change volume when the incoming call comes.");
    }

    public void clearRingerChangedManually() {
        if (preferences.getBoolean("VOLUME_MANUALLY_CHANGED", false)) {
            writeDebugEvent("Reset Manual volume change", "Resetting manual volume change flag");
        }
        SharedPreferences.Editor ed = preferences.edit();
        ed.remove("VOLUME_MANUALLY_CHANGED");
        ed.remove("VOLUME_MANUALLY_CHANGED_TIME");
        ed.apply();
    }

    public boolean isRingerChangedManually() {
        return preferences.getBoolean("VOLUME_MANUALLY_CHANGED", false);
    }
}
