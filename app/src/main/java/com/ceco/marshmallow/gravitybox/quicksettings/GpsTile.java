/*
 * Copyright (C) 2015 Peter Gregus for GravityBox Project (C3C076@xda)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ceco.marshmallow.gravitybox.quicksettings;

import com.ceco.marshmallow.gravitybox.R;

import de.robv.android.xposed.XSharedPreferences;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.provider.Settings;

public class GpsTile extends QsTile {
    public static final String GPS_ENABLED_CHANGE_ACTION = "android.location.GPS_ENABLED_CHANGE";
    public static final String GPS_FIX_CHANGE_ACTION = "android.location.GPS_FIX_CHANGE";
    public static final String EXTRA_GPS_ENABLED = "enabled";

    private boolean mGpsEnabled;
    private boolean mGpsFixed;
    private boolean mIsReceiving;

    private BroadcastReceiver mLocationManagerReceiver = new BroadcastReceiver() {
        @SuppressWarnings("deprecation")
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DEBUG) log(getKey() + ": Broadcast received: " + intent.toString());
            final String action = intent.getAction();

            if (action.equals(LocationManager.PROVIDERS_CHANGED_ACTION)) {
                mGpsEnabled = Settings.Secure.isLocationProviderEnabled(
                        mContext.getContentResolver(), LocationManager.GPS_PROVIDER);
                mGpsFixed = false;
            } else if (action.equals(GPS_FIX_CHANGE_ACTION)) {
                mGpsFixed = intent.getBooleanExtra(EXTRA_GPS_ENABLED, false);
            } else if (action.equals(GPS_ENABLED_CHANGE_ACTION)) {
                mGpsFixed = false;
            }

            if (DEBUG) log(getKey() + ": mGpsEnabled = " + mGpsEnabled + "; mGpsFixed = " + mGpsFixed);
            refreshState();
        }
    };

    public GpsTile(Object host, String key, XSharedPreferences prefs,
            QsTileEventDistributor eventDistributor) throws Throwable {
        super(host, key, prefs, eventDistributor);
    }

    @SuppressWarnings("deprecation")
    private void registerLocationManagerReceiver() {
        if (mIsReceiving) return;
        mGpsEnabled = Settings.Secure.isLocationProviderEnabled(
                mContext.getContentResolver(), LocationManager.GPS_PROVIDER);
        mGpsFixed = false;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
        intentFilter.addAction(GPS_ENABLED_CHANGE_ACTION);
        intentFilter.addAction(GPS_FIX_CHANGE_ACTION);
        mContext.registerReceiver(mLocationManagerReceiver, intentFilter);
        mIsReceiving = true;
        if (DEBUG) log(getKey() + ": Location manager receiver registered");
    }

    private void unregisterLocationManagerReceiver() {
        if (mIsReceiving) {
            mContext.unregisterReceiver(mLocationManagerReceiver);
            mIsReceiving = false;
            if (DEBUG) log(getKey() + ": Location manager receiver unregistered");
        }
    }

    @Override
    public void setListening(boolean listening) {
        if (listening && mEnabled) {
            registerLocationManagerReceiver();
        } else {
            unregisterLocationManagerReceiver();
        }
    }

    @Override
    public void handleUpdateState(Object state, Object arg) {
        mState.visible = true;
        if (mGpsEnabled) {
            mState.label = mGpsFixed ? mGbContext.getString(R.string.qs_tile_gps_locked) :
                    mGbContext.getString(R.string.qs_tile_gps_enabled);
            mState.icon = mGpsFixed ? mGbContext.getDrawable(R.drawable.ic_qs_gps_locked) :
                    mGbContext.getDrawable(R.drawable.ic_qs_gps_enable);
        } else {
            mState.label = mGbContext.getString(R.string.qs_tile_gps_disabled);
            mState.icon = mGbContext.getDrawable(R.drawable.ic_qs_gps_disable);
        }

        super.handleUpdateState(state, arg);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleClick() {
        Settings.Secure.setLocationProviderEnabled(
                mContext.getContentResolver(), LocationManager.GPS_PROVIDER, !mGpsEnabled);
        super.handleClick();
    }

    @Override
    public boolean handleLongClick() {
        startSettingsActivity(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        return true;
    }

    @Override
    public void handleDestroy() {
        super.handleDestroy();
        unregisterLocationManagerReceiver();
    }
}
