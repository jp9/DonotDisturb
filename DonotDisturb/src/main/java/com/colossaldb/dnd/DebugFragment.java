package com.colossaldb.dnd;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.colossaldb.dnd.prefs.AppPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Jayaprakash Pasala
 * Date:  12/12/13
 * Time:  11:01 PM
 */
public class DebugFragment extends PreferenceFragment {
    public DebugFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getPreferenceManager() == null)
            return;

        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(getActivity());
        if (root == null)
            return;

        root.setTitle("Debug Information");

        root.addPreference(getPast(getPreferenceManager()));
        root.addPreference(getPresent(getPreferenceManager()));
        root.addPreference(getFuture(getPreferenceManager()));
        root.addPreference(getErrors(getPreferenceManager()));
        setPreferenceScreen(root);
    }

    Preference getPast(PreferenceManager preferenceManager) {
        PreferenceScreen past = preferenceManager.createPreferenceScreen(getActivity());
        if (past == null)
            return failedPreference("Failed to create past");
        past.setTitle(R.string.past);

        JSONArray events = AppPreferences.getInstance().getDebugEvents();
        addEventsToPreferenceScreen(past, events);
        return past;
    }

    Preference getErrors(PreferenceManager preferenceManager) {
        PreferenceScreen errors = preferenceManager.createPreferenceScreen(getActivity());
        if (errors == null)
            return failedPreference("Failed to create errors");
        errors.setTitle(R.string.errors);

        JSONArray events = AppPreferences.getInstance().getErrorEvents();
        addEventsToPreferenceScreen(errors, events);
        return errors;
    }

    private void addEventsToPreferenceScreen(PreferenceScreen screen, JSONArray events) {
        if (events == null || events.length() == 0) {
            screen.addPreference(failedPreference("No events"));
            return;
        }

        for (int i = events.length() - 1; i >= 0; i--) {
            try {
                JSONObject object = events.getJSONObject(i);
                Preference eventPreference = new Preference(getActivity());
                eventPreference.setTitle(object.getString(AppPreferences.TITLE_KEY));
                eventPreference.setSummary(object.getString(AppPreferences.TIMESTAMP_KEY) + " : " + object.getString(AppPreferences.DETAIL_KEY));
                screen.addPreference(eventPreference);
            } catch (JSONException ignored) {
                Log.e("DebugFragment", "Failed get object", ignored);
            }
        }
    }

    Preference getFuture(PreferenceManager preferenceManager) {
        PreferenceScreen future = preferenceManager.createPreferenceScreen(getActivity());
        if (future == null)
            return failedPreference("Failed to create future");
        future.setTitle(R.string.future);

        Preference nextEvent = new Preference(getActivity());
        nextEvent.setTitle("Next Schedule Run");
        nextEvent.setSummary(AppPreferences.getInstance().getNextScheduleRun());
        future.addPreference(nextEvent);
        return future;
    }

    private Preference failedPreference(String msg) {
        Preference p = new Preference(getActivity());
        p.setTitle(msg);
        return p;
    }

    Preference getPresent(PreferenceManager preferenceManager) {
        PreferenceScreen present = preferenceManager.createPreferenceScreen(getActivity());
        if (present == null)
            return failedPreference("Failed to create present");

        present.setTitle(R.string.present);

        AppPreferences pref = AppPreferences.getInstance();

        Preference enabled = new Preference(getActivity());
        enabled.setTitle(R.string.enabled);
        enabled.setSummary("" + pref.isEnabled());
        present.addPreference(enabled);

        Preference startTime = new Preference(getActivity());
        startTime.setTitle(R.string.start);
        startTime.setSummary(pref.getFormattedStartTime());
        present.addPreference(startTime);

        Preference endTime = new Preference(getActivity());
        endTime.setTitle(R.string.end);
        endTime.setSummary(pref.getFormattedEndTime());
        present.addPreference(endTime);

        Preference ringForContacts = new Preference(getActivity());
        ringForContacts.setTitle(R.string.contacts);
        ringForContacts.setSummary("" + pref.isEnabled());
        present.addPreference(ringForContacts);

        Preference ringOnRepeat = new Preference(getActivity());
        ringOnRepeat.setTitle(R.string.repeat_on_ring);
        ringOnRepeat.setSummary("" + pref.isEnabled());
        present.addPreference(ringOnRepeat);

        // Add the current state
        // Did the user change volume?
        // Did we sent the ringer volume?

        return present;
    }
}
