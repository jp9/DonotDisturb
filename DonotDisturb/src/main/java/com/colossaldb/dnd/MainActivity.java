package com.colossaldb.dnd;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TimePicker;

import com.colossaldb.dnd.prefs.AppPreferences;
import com.colossaldb.dnd.service.StartStopReceiver;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        CompoundButton.OnCheckedChangeListener dndEnabledListener = new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    AudioManager audioManager = (AudioManager) MyApp.getAppContext().getSystemService(Context.AUDIO_SERVICE);
                    if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
                        // Use the Builder class for convenient dialog construction
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setMessage(R.string.enable_ringer)
                                .setPositiveButton(R.string.unmute, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        AudioManager am = (AudioManager) MyApp.getAppContext().getSystemService(Context.AUDIO_SERVICE);
                                        am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        // User cancelled the dialog
                                    }
                                });
                        builder.create().show();
                    }
                }
                saveAll();
            }
        };

        CompoundButton.OnCheckedChangeListener saveListener = new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                saveAll();
            }
        };

        // Set the default value for enabled.
        ((Switch) findViewById(R.id.dnd_enabled)).setOnCheckedChangeListener(dndEnabledListener);
        ((Switch) findViewById(R.id.dnd_enabled)).setChecked(AppPreferences.getInstance().isEnabled());
        ((Switch) findViewById(R.id.ring_on_repeat)).setOnCheckedChangeListener(saveListener);
        ((Switch) findViewById(R.id.ring_on_repeat)).setChecked(AppPreferences.getInstance().ringOnRepeatCall());
        ((Switch) findViewById(R.id.ring_for_contacts)).setOnCheckedChangeListener(saveListener);
        ((Switch) findViewById(R.id.ring_for_contacts)).setChecked(AppPreferences.getInstance().ringForContacts());
        // Set the current time.
        setButtonTime((Button) findViewById(R.id.start_time), AppPreferences.getInstance().getStartHour(22),
                AppPreferences.getInstance().getStartMinute(0));
        // Set the current time.
        setButtonTime((Button) findViewById(R.id.end_time), AppPreferences.getInstance().getEndHour(6),
                AppPreferences.getInstance().getEndMinute(0));
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
    }

    private void saveAll() {
        boolean dndEnabled = ((Switch) findViewById(R.id.dnd_enabled)).isChecked();
        boolean ringOnRepeat = ((Switch) findViewById(R.id.ring_on_repeat)).isChecked();
        boolean ringForContacts = ((Switch) findViewById(R.id.ring_for_contacts)).isChecked();
        int startHourMin = Integer.valueOf(findViewById(R.id.start_time).getTag().toString());
        int startHour = startHourMin / 60;
        int startMin = startHourMin % 60;
        int endHourMin = Integer.valueOf(findViewById(R.id.end_time).getTag().toString());
        int endHour = endHourMin / 60;
        int endMin = endHourMin % 60;
        AppPreferences.getInstance().save(dndEnabled, startHour, startMin, endHour, endMin, ringOnRepeat, ringForContacts);

        // Fire the broadcast to StartStopReceiver.
        Intent intent = new Intent(getApplicationContext(), StartStopReceiver.class);
        intent.setAction("com.colossaldb.dnd.START_STOP");
        sendBroadcast(intent);
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
            case R.id.action_debug:
                Intent intent = new Intent(getApplicationContext(), DebugActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_about:
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage(R.string.about_desc)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                builder.create().show();
                return true;
        }
        return super.onOptionsItemSelected(item);
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

    public static class TimePickerFragment extends DialogFragment
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
            return new TimePickerDialog(getActivity(), this, this.defaultHour, this.defaultMin,
                    DateFormat.is24HourFormat(MyApp.getAppContext()));
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            Button b;
            if (getActivity() == null)
                return;

            if (isStartTime) {
                AppPreferences.getInstance().setStartTime(hourOfDay, minute);
                b = (Button) getActivity().findViewById(R.id.start_time);
            } else {
                AppPreferences.getInstance().setEndTime(hourOfDay, minute);
                b = (Button) getActivity().findViewById(R.id.end_time);
            }
            setButtonTime(b, hourOfDay, minute);
        }
    }

    private static void setButtonTime(Button b, int hourOfDay, int minute) {
        if (b == null)
            return;

        b.setTag((hourOfDay * 60 + minute) + "");
        boolean isAM = true;
        if (hourOfDay > 11) {
            isAM = false;
            hourOfDay = hourOfDay == 12 ? 12 : hourOfDay - 12;
        }

        b.setText(String.format("%2d:%02d %s", hourOfDay, minute, MyApp.getAppContext().getString(isAM ? R.string.am : R.string.pm)));
    }

}
