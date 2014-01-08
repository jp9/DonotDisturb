package com.colossaldb.dnd.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Pair;
import com.colossaldb.dnd.prefs.AppPreferences;

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
public class RingerVolumeChangedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        AppPreferences pref = AppPreferences.getInstance();
        if (!pref.isEnabled()) {
            // The application is not in play. Shutdown.
            return;
        }

        Pair<Long, Boolean> result = StartStopReceiver.getDelay(pref);

        if (AudioManager.RINGER_MODE_CHANGED_ACTION.equals(intent.getAction())) {
            // Volume was manually adjusted during quiet time.
            if (result.second)
                AppPreferences.getInstance().markRingerChangedManually();
        } else {
            AppPreferences.getInstance().writeErrorEvent("RingerVolumeChangedReceiver was called with unknown action.",
                    "Action name : " + intent.getAction());
        }
    }
}

