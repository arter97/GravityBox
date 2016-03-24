/*
 * Copyright (C) 2013 The SlimRoms Project
 * Copyright (C) 2016 Peter Gregus for GravityBox Project (C3C076@xda)
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

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.UserManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ceco.marshmallow.gravitybox.GravityBoxSettings;
import com.ceco.marshmallow.gravitybox.ModStatusBar;
import com.ceco.marshmallow.gravitybox.R;
import com.ceco.marshmallow.gravitybox.Utils;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class LocationTileSlimkat extends QsTile {

    private static final Intent LOCATION_SETTINGS_INTENT = 
            new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);

    public static final Integer[] LOCATION_SETTINGS = new Integer[] {
        Settings.Secure.LOCATION_MODE_BATTERY_SAVING,
        Settings.Secure.LOCATION_MODE_SENSORS_ONLY,
        Settings.Secure.LOCATION_MODE_HIGH_ACCURACY
    };

    private boolean mIsReceiving;
    private int mLastActiveMode;
    private Object mDetailAdapter;
    private List<Integer> mLocationList = new ArrayList<Integer>();
    private boolean mQuickMode;

    public LocationTileSlimkat(Object host, String key, XSharedPreferences prefs,
            QsTileEventDistributor eventDistributor) throws Throwable {
        super(host, key, prefs, eventDistributor);

        mLastActiveMode = getLocationMode();
        if(mLastActiveMode == Settings.Secure.LOCATION_MODE_OFF) {
            mLastActiveMode = Settings.Secure.LOCATION_MODE_HIGH_ACCURACY;
        }
    }

    @Override
    public void initPreferences() {
        mQuickMode = mPrefs.getBoolean(GravityBoxSettings.PREF_KEY_LOCATION_TILE_QUICK_MODE, false);

        super.initPreferences();
    }

    @Override
    public void onBroadcastReceived(Context context, Intent intent) {
        if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_QUICKSETTINGS_CHANGED)) {
            if (intent.hasExtra(GravityBoxSettings.EXTRA_LOCATION_TILE_QUICK_MODE)) {
                mQuickMode = intent.getBooleanExtra(GravityBoxSettings.EXTRA_LOCATION_TILE_QUICK_MODE, false);
            }
        }

        super.onBroadcastReceived(context, intent);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(LocationManager.MODE_CHANGED_ACTION) || 
                    action.equals(LocationManager.PROVIDERS_CHANGED_ACTION)) {
               locationSettingsChanged();
            }
        }
    };

    private void registerLocationManagerReceiver() {
        if (mIsReceiving) return;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocationManager.MODE_CHANGED_ACTION);
        intentFilter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
        mContext.registerReceiver(mBroadcastReceiver, intentFilter);
        mIsReceiving = true;
        if (DEBUG) log(getKey() + ": Location manager registered");
    }

    private void unregisterLocationManagerReceiver() {
        if (mIsReceiving) {
            mContext.unregisterReceiver(mBroadcastReceiver);
            mIsReceiving = false;
            if (DEBUG) log(getKey() + ": Location manager unregistered");
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

    private boolean isLocationEnabled() {
        return (getLocationMode() != Settings.Secure.LOCATION_MODE_OFF);
    }

    private int getLocationMode() {
        try {
            int currentUserId = Utils.getCurrentUser();
            if (isUserLocationRestricted(currentUserId)) {
                return Settings.Secure.LOCATION_MODE_OFF;
            }
            final ContentResolver cr = mContext.getContentResolver();
            return (int) XposedHelpers.callStaticMethod(Settings.Secure.class, "getIntForUser",
                    cr, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF, currentUserId);
        } catch (Throwable t) {
            XposedBridge.log(t);
            return Settings.Secure.LOCATION_MODE_OFF;
        }
    }

    private void setLocationMode(int mode) {
        try {
            int currentUserId = Utils.getCurrentUser();
            if (isUserLocationRestricted(currentUserId)) {
                return;
            }
            final ContentResolver cr = mContext.getContentResolver();
            XposedHelpers.callStaticMethod(Settings.Secure.class,
                    "putIntForUser", cr, Settings.Secure.LOCATION_MODE,
                    mode, currentUserId);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private void switchLocationMode() {
        int currentMode = getLocationMode();
        switch (currentMode) {
            case Settings.Secure.LOCATION_MODE_OFF:
                setLocationMode(Settings.Secure.LOCATION_MODE_BATTERY_SAVING);
                break;
            case Settings.Secure.LOCATION_MODE_BATTERY_SAVING:
                setLocationMode(Settings.Secure.LOCATION_MODE_SENSORS_ONLY);
                break;
            case Settings.Secure.LOCATION_MODE_SENSORS_ONLY:
                setLocationMode(Settings.Secure.LOCATION_MODE_HIGH_ACCURACY);
                break;
            case Settings.Secure.LOCATION_MODE_HIGH_ACCURACY:
                setLocationMode(Settings.Secure.LOCATION_MODE_OFF);
                break;
        }
    }

    private boolean setLocationEnabled(boolean enabled) {
        int currentUserId = Utils.getCurrentUser();
        if (isUserLocationRestricted(currentUserId)) {
            return false;
        }
        final ContentResolver cr = mContext.getContentResolver();

        // Store last active mode if we are switching off
        // so we can restore it at the next enable
        if(!enabled) {
            mLastActiveMode = getLocationMode();
        }

        int mode = enabled ? mLastActiveMode : Settings.Secure.LOCATION_MODE_OFF;
        return (boolean) XposedHelpers.callStaticMethod(Settings.Secure.class, "putIntForUser",
                cr, Settings.Secure.LOCATION_MODE, mode, currentUserId);
    }

    private boolean isUserLocationRestricted(int userId) {
        try {
            final UserManager um = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
            return (boolean) XposedHelpers.callMethod(um, "hasUserRestriction",
                    UserManager.DISALLOW_SHARE_LOCATION,
                    Utils.getUserHandle(userId));
        } catch (Throwable t) {
            XposedBridge.log(t);
            return false;
        }
    }

    private void locationSettingsChanged() {
        if (DEBUG) {
            log(getKey() + ": mLocationEnabled = " + isLocationEnabled() + 
                    "; mLocationMode = " + getLocationMode());
        }
        refreshState();
    }

    private String getModeLabel(int currentState) {
        switch (currentState) {
            case Settings.Secure.LOCATION_MODE_OFF:
                return mGbContext.getString(R.string.quick_settings_location_off);
            case Settings.Secure.LOCATION_MODE_BATTERY_SAVING:
                return mGbContext.getString(R.string.location_mode_battery_saving);
            case Settings.Secure.LOCATION_MODE_SENSORS_ONLY:
                return mGbContext.getString(R.string.location_mode_device_only);
            case Settings.Secure.LOCATION_MODE_HIGH_ACCURACY:
                return mGbContext.getString(R.string.location_mode_high_accuracy);
            default:
                return mGbContext.getString(R.string.qs_tile_gps);
         }
    }

    @Override
    public void handleUpdateState(Object state, Object arg) {
        mState.visible = true;
        int locationMode = getLocationMode();
        switch (locationMode) {
            case Settings.Secure.LOCATION_MODE_SENSORS_ONLY:
                mState.icon = mGbContext.getDrawable(R.drawable.ic_qs_location_on);
                break;
            case Settings.Secure.LOCATION_MODE_BATTERY_SAVING:
                mState.icon = mGbContext.getDrawable(R.drawable.ic_qs_location_battery_saving);
                break;
            case Settings.Secure.LOCATION_MODE_HIGH_ACCURACY:
                mState.icon = mGbContext.getDrawable(R.drawable.ic_qs_location_on);
                break;
            case Settings.Secure.LOCATION_MODE_OFF:
                mState.icon = mGbContext.getDrawable(R.drawable.ic_qs_location_off);
                break;
        }
        mState.label = getModeLabel(locationMode);

        super.handleUpdateState(state, arg);
    }

    @Override
    public void handleClick() {
        if (mQuickMode) {
            switchLocationMode();
        } else if (supportsDualTargets()) {
            setLocationEnabled(!isLocationEnabled());
        } else {
            showDetail(true);
        }
        super.handleClick();
    }

    @Override
    public boolean handleLongClick() {
        if (supportsDualTargets()) {
            startSettingsActivity(LOCATION_SETTINGS_INTENT);
        } else if (mQuickMode) {
            showDetail(true);
        } else {
            setLocationEnabled(!isLocationEnabled());
        }
        return true;
    }

    @Override
    public boolean supportsHideOnChange() {
        return supportsDualTargets();
    }

    @Override
    public boolean handleSecondaryClick() {
        showDetail(true);
        return true;
    }

    @Override
    public void handleDestroy() {
        mDetailAdapter = null;
        mLocationList.clear();
        mLocationList = null;
        super.handleDestroy();
    }

    @Override
    public Object getDetailAdapter() {
        if (mDetailAdapter == null) {
            mDetailAdapter = QsDetailAdapterProxy.createProxy(
                    mContext.getClassLoader(), new LocationDetailAdapter());
        }
        return mDetailAdapter;
    }

    private class AdvancedLocationAdapter extends ArrayAdapter<Integer> {
        public AdvancedLocationAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_single_choice, mLocationList);
        }

        @SuppressLint("ViewHolder")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            CheckedTextView label = (CheckedTextView) inflater.inflate(
                    android.R.layout.simple_list_item_single_choice, parent, false);
            label.setText(getModeLabel(getItem(position)));
            return label;
        }
    }

    private class LocationDetailAdapter implements QsDetailAdapterProxy.Callback, AdapterView.OnItemClickListener {

        private AdvancedLocationAdapter mAdapter;
        private QsDetailItemsList mDetails;

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            setLocationMode((Integer) parent.getItemAtPosition(position));
        }

        @Override
        public int getTitle() {
            return mContext.getResources().getIdentifier("quick_settings_location_label",
                    "string", ModStatusBar.PACKAGE_NAME);
        }

        @Override
        public Boolean getToggleState() {
            boolean state = isLocationEnabled();
            rebuildLocationList(state);
            return state;
        }

        @Override
        public View createDetailView(Context context, View convertView, ViewGroup parent) throws Throwable {
            if (mDetails == null) {
                mDetails = QsDetailItemsList.create(context, parent);
                mDetails.setEmptyState(R.drawable.ic_qs_location_off,
                        getModeLabel(Settings.Secure.LOCATION_MODE_OFF));
                mAdapter = new AdvancedLocationAdapter(context);
                mDetails.setAdapter(mAdapter);
    
                final ListView list = mDetails.getListView();
                list.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
                list.setOnItemClickListener(this);
            }

            return mDetails.getView();
        }

        @Override
        public Intent getSettingsIntent() {
            return LOCATION_SETTINGS_INTENT;
        }

        @Override
        public void setToggleState(boolean state) {
            setLocationEnabled(state);
            rebuildLocationList(state);
            fireToggleStateChanged(state);
            if (!state) {
                showDetail(false);
            }
        }

        private void rebuildLocationList(boolean populate) {
            mLocationList.clear();
            if (populate) {
                mLocationList.addAll(Arrays.asList(LOCATION_SETTINGS));
                mDetails.getListView().setItemChecked(mAdapter.getPosition(
                        getLocationMode()), true);
            }
            mAdapter.notifyDataSetChanged();
        }
    }
}
