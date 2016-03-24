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

package com.ceco.marshmallow.gravitybox.shortcuts;

import java.util.ArrayList;
import java.util.List;

import com.ceco.marshmallow.gravitybox.ConnectivityServiceWrapper;
import com.ceco.marshmallow.gravitybox.R;
import com.ceco.marshmallow.gravitybox.adapters.IIconListAdapterItem;
import com.ceco.marshmallow.gravitybox.adapters.IconListAdapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.graphics.drawable.Drawable;
import android.provider.Settings;

public class LocationModeShortcut extends AShortcut {
    protected static final String ACTION =  ConnectivityServiceWrapper.ACTION_SET_LOCATION_MODE;

    public LocationModeShortcut(Context context) {
        super(context);
    }

    @Override
    public String getText() {
        return mContext.getString(R.string.shortcut_location_mode);
    }

    @Override
    public Drawable getIconLeft() {
        return mResources.getDrawable(R.drawable.shortcut_gps_high);
    }

    @Override
    protected String getAction() {
        return ACTION;
    }

    @Override
    protected String getShortcutName() {
        return getText();
    }

    @Override
    protected ShortcutIconResource getIconResource() {
        return null;
    }

    @Override
    protected void createShortcut(final CreateShortcutListener listener) {
        final List<IIconListAdapterItem> list = new ArrayList<IIconListAdapterItem>();
        list.add(new LocationModeItem(mContext.getString(R.string.location_mode_high_accuracy), 
                R.drawable.shortcut_gps_high, Settings.Secure.LOCATION_MODE_HIGH_ACCURACY));
        list.add(new LocationModeItem(mContext.getString(R.string.location_mode_battery_saving),
                R.drawable.shortcut_gps_saving, Settings.Secure.LOCATION_MODE_BATTERY_SAVING));
        list.add(new LocationModeItem(mContext.getString(R.string.location_mode_device_only),
                R.drawable.shortcut_gps_sensors, Settings.Secure.LOCATION_MODE_SENSORS_ONLY));
        list.add(new LocationModeItem(mContext.getString(R.string.location_mode_off),
                R.drawable.shortcut_gps_off, Settings.Secure.LOCATION_MODE_OFF));

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
            .setTitle(getText())
            .setAdapter(new IconListAdapter(mContext, list), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    LocationModeItem item = (LocationModeItem) list.get(which);

                    Intent launchIntent = new Intent(mContext, LaunchActivity.class);
                    launchIntent.setAction(ShortcutActivity.ACTION_LAUNCH_ACTION);
                    launchIntent.putExtra(ShortcutActivity.EXTRA_ACTION, getAction());
                    launchIntent.putExtra(ShortcutActivity.EXTRA_ACTION_TYPE, getActionType());
                    launchIntent.putExtra(ConnectivityServiceWrapper.EXTRA_LOCATION_MODE, item.getLocationMode());
                    launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    Intent intent = new Intent();
                    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent);
                    intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, item.getText());
                    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, item.getIconResource());

                    LocationModeShortcut.this.onShortcutCreated(intent, listener);
                    dialog.dismiss();
                }
            })
            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static void launchAction(final Context context, Intent intent) {
        Intent launchIntent = new Intent(ACTION);
        launchIntent.putExtra(ConnectivityServiceWrapper.EXTRA_LOCATION_MODE,
                intent.getIntExtra(ConnectivityServiceWrapper.EXTRA_LOCATION_MODE,
                        Settings.Secure.LOCATION_MODE_BATTERY_SAVING));
        context.sendBroadcast(launchIntent);
    }

    class LocationModeItem implements IIconListAdapterItem {
        private String mLabel;
        private int mIconResId;
        private int mLocationMode;

        public LocationModeItem(String text, int iconResId, int locationMode) {
            mLabel = text;
            mIconResId = iconResId;
            mLocationMode = locationMode;
        }

        @Override
        public String getText() {
            return mLabel;
        }

        @Override
        public String getSubText() {
            return null;
        }

        @Override
        public Drawable getIconLeft() {
            return mResources.getDrawable(mIconResId);
        }

        @Override
        public Drawable getIconRight() {
            return null;
        }

        public ShortcutIconResource getIconResource() {
            return ShortcutIconResource.fromContext(mContext, mIconResId);
        }

        public int getLocationMode() {
            return mLocationMode;
        }
    }
}
