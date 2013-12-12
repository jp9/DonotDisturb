package com.colossaldb.dnd.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.colossaldb.dnd.MyApp;

import java.util.SortedMap;
import java.util.TreeMap;

import prefs.AppPreferences;

/**
 * Created by Jayaprakash Pasala
 * Date:  12/11/13
 * Time:  10:58 PM
 */
public class PhoneStateBroadcastReceiver extends BroadcastReceiver {
    // Stores the phone number to last received call.
    // This map is pruned every time we are in the service.
    private final SortedMap<String, Long> phoneNumToLastCall = new TreeMap<String, Long>();

    @Override
    public void onReceive(Context context, Intent intent) {
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        Log.i("PhoneStateBroadcastReceiver", " State = " + state);
        if (!AppPreferences.getInstance().isEnabled())
            return;
        if ("RINGING".equals(state)) {
            String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            Log.i("PhoneStateBroadcastReceiver", " Number = " + number);
            boolean unSilence = isContact(number);
            Log.i("PhoneStateBroadcastReceiver", " isContact = " + (unSilence));

            if (!unSilence) {
                Long last = phoneNumToLastCall.get(number);
                long currentTimeMillis = System.currentTimeMillis();
                if (last == null) {
                    phoneNumToLastCall.put(number, currentTimeMillis);
                } else {
                    if (currentTimeMillis - last <= 600000L) {
                        // Second call in 10 mins, hence un silence
                        unSilence = true;
                        phoneNumToLastCall.put(number, currentTimeMillis);
                    }
                }
            }

            if (unSilence) {
                AudioManager audioManager = (AudioManager) MyApp.getAppContext().getSystemService(Context.AUDIO_SERVICE);
                StartStopReceiver.enableNormal(audioManager);
            }
        } else if ("IDLE".equals(state)) {
            StartStopReceiver.execDnd(MyApp.getAppContext(), AppPreferences.getInstance());
        }
    }

    public boolean isContact(String number) {
//        Uri contactUri = ContactsContract.Contacts.CONTENT_URI;
//        String[] PROJECTION = new String[] {
//                ContactsContract.Contacts._ID,
//                ContactsContract.Contacts.DISPLAY_NAME,
//                ContactsContract.Contacts.HAS_PHONE_NUMBER,
//        };
//        String SELECTION = ContactsContract.Contacts.HAS_PHONE_NUMBER + "='1'";
//        Cursor contacts = MyApp.getAppContext().getContentResolver().
//                query(ContactsContract.Contacts.CONTENT_URI, PROJECTION, SELECTION, null, null);


        Cursor c = null;
        try {
            Uri uri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, Uri.encode(number));
            c = MyApp.getAppContext().getContentResolver().query(uri,
                    new String[]{ContactsContract.CommonDataKinds.Phone._ID,
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
                            ContactsContract.CommonDataKinds.Phone.NUMBER},
                    null, null, null);
            Toast.makeText(MyApp.getAppContext(), "The contact count = " + (c == null ? 0 : c.getCount()), Toast.LENGTH_SHORT);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
            }
            return c != null && c.getCount() > 0;
        } finally {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

}
