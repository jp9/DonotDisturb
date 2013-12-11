package prefs;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Jayaprakash Pasala
 * Date:  12/10/13
 * Time:  10:28 AM
 */
public class AppPreferences {
    private static final String PREFERENCE_NAME = "AppPreferences";

    // Keys
    private static final String START_HOUR = "sh";
    private static final String START_MIN = "sm";
    private static final String END_HOUR = "eh";
    private static final String END_MIN = "em";
    private static final String DND_ENABLED = "dnd_enabled";
    private static final String RING_ON_REPEAT = "ring_on_repeat";

    private static AppPreferences INSTANCE = null;

    private SharedPreferences preferences;

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

    private void setTimes(String hourKey, int hour, String minKey, int mins) {
        SharedPreferences.Editor e = preferences.edit();
        e.putInt(hourKey, hour);
        e.putInt(minKey, mins);
        e.apply();
    }

    public void save(boolean dndEnabled, int startHour, int startMin, int endHour, int endMin,
                     boolean ringOnRepeat) {
        assert startHour >= 0 && startHour < 24;
        assert endHour >= 0 && endHour < 24;
        assert startMin >= 0 && startMin < 60;
        assert endMin >= 0 && endMin < 60;

        SharedPreferences.Editor e = preferences.edit();
        e.putBoolean(DND_ENABLED, dndEnabled);
        e.putInt(START_HOUR, startHour);
        e.putInt(START_MIN, startMin);
        e.putInt(END_HOUR, endHour);
        e.putInt(END_MIN, endMin);
        e.putBoolean(RING_ON_REPEAT, ringOnRepeat);
        e.apply();
    }
}
