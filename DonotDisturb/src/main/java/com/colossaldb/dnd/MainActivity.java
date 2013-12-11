package com.colossaldb.dnd;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TimePicker;

import com.colossaldb.dnd.service.StartStopService;

import prefs.AppPreferences;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppPreferences.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // Set the default value for enabled.
        ((Switch) findViewById(R.id.dnd_enabled)).setChecked(AppPreferences.getInstance().isEnabled());
        ((Switch) findViewById(R.id.ring_on_repeat)).setChecked(AppPreferences.getInstance().ringOnRepeatCall());

        // Set the current time.
        setButtonTime((Button) findViewById(R.id.start_time), AppPreferences.getInstance().getStartHour(22),
                AppPreferences.getInstance().getStartMinute(0), getApplicationContext());
        // Set the current time.
        setButtonTime((Button) findViewById(R.id.end_time), AppPreferences.getInstance().getEndHour(6),
                AppPreferences.getInstance().getEndMinute(0), getApplicationContext());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
        boolean dndEnabled = ((Switch) findViewById(R.id.dnd_enabled)).isChecked();
        boolean ringOnRepeat = ((Switch) findViewById(R.id.ring_on_repeat)).isChecked();
        int startHourMin = Integer.valueOf(findViewById(R.id.start_time).getTag().toString());
        int startHour = startHourMin / 60;
        int startMin = startHourMin % 60;
        int endHourMin = Integer.valueOf(findViewById(R.id.end_time).getTag().toString());
        int endHour = endHourMin / 60;
        int endMin = endHourMin % 60;
        AppPreferences.getInstance().save(dndEnabled, startHour, startMin, endHour, endMin, ringOnRepeat);
        if (dndEnabled) {
            // Send Broadcast
            Intent intent = new Intent(getApplicationContext(), StartStopService.class);
            intent.setAction("com.colossaldb.dnd.START_STOP");
            sendBroadcast(intent);
        } else {
            // Figure out if we should re-enable ringer?
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onToggleClicked(View view) {
        boolean dndEnabled = ((Switch) findViewById(R.id.dnd_enabled)).isChecked();
        boolean ringOnRepeat = ((Switch) findViewById(R.id.ring_on_repeat)).isChecked();
        int startHourMin = Integer.valueOf(findViewById(R.id.start_time).getTag().toString());
        int startHour = startHourMin / 60;
        int startMin = startHourMin % 60;
        int endHourMin = Integer.valueOf(findViewById(R.id.end_time).getTag().toString());
        int endHour = endHourMin / 60;
        int endMin = endHourMin % 60;
        AppPreferences.getInstance().save(dndEnabled, startHour, startMin, endHour, endMin, ringOnRepeat);
    }

    public void onSpinnerChanged(View view) {
    }

    public void showTimePickerDialog(View view) {
        int hourMin = Integer.valueOf(view.getTag().toString());
        int hour = hourMin / 60;
        int min = hourMin % 60;
        boolean isStart = (view.getId() == R.id.start_time);
        DialogFragment newFragment = new TimePickerFragment(isStart, hour, min);
        newFragment.show(getFragmentManager(), "timePicker");
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_main, container, false);
        }
    }

    public class TimePickerFragment extends DialogFragment
            implements TimePickerDialog.OnTimeSetListener {

        final boolean isStartTime;
        final int defaultHour;
        final int defaultMin;

        TimePickerFragment(boolean isStartTime, int hour, int min) {
            this.isStartTime = isStartTime;
            this.defaultHour = hour;
            this.defaultMin = min;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(MainActivity.this, this, this.defaultHour, this.defaultMin,
                    DateFormat.is24HourFormat(MainActivity.this));
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            Button b;
            if (isStartTime) {
                AppPreferences.getInstance().setStartTime(hourOfDay, minute);
                b = (Button) MainActivity.this.findViewById(R.id.start_time);
            } else {
                AppPreferences.getInstance().setEndTime(hourOfDay, minute);
                b = (Button) MainActivity.this.findViewById(R.id.end_time);
            }
            setButtonTime(b, hourOfDay, minute, MainActivity.this);
        }

    }

    private static void setButtonTime(Button b, int hourOfDay, int minute, Context context) {
        if (b == null)
            return;

        b.setTag((hourOfDay * 60 + minute) + "");
        boolean isAM = true;
        if (hourOfDay > 11) {
            isAM = false;
            hourOfDay = hourOfDay == 12 ? 12 : hourOfDay - 12;
        }

        b.setText(String.format("%2d:%02d %s", hourOfDay, minute, context.getString(isAM ? R.string.am : R.string.pm)));
    }

}