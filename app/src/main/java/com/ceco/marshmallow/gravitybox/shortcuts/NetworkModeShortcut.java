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

import com.ceco.marshmallow.gravitybox.PhoneWrapper;
import com.ceco.marshmallow.gravitybox.R;
import com.ceco.marshmallow.gravitybox.adapters.IIconListAdapterItem;
import com.ceco.marshmallow.gravitybox.adapters.IconListAdapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.graphics.drawable.Drawable;

public class NetworkModeShortcut extends AShortcut {
    protected static final String ACTION =  PhoneWrapper.ACTION_CHANGE_NETWORK_TYPE;

    public NetworkModeShortcut(Context context) {
        super(context);
    }

    @Override
    public String getText() {
        return mContext.getString(R.string.shortcut_network_mode);
    }

    @Override
    public Drawable getIconLeft() {
        return mResources.getDrawable(R.drawable.shortcut_network_mode);
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
        return ShortcutIconResource.fromContext(mContext, R.drawable.shortcut_network_mode);
    }

    @Override
    protected void createShortcut(final CreateShortcutListener listener) {
        final List<IIconListAdapterItem> list = new ArrayList<IIconListAdapterItem>();
        list.add(new NetworkModeItem(R.drawable.shortcut_network_mode_2g, 
                PhoneWrapper.NT_GSM_ONLY));
        list.add(new NetworkModeItem(R.drawable.shortcut_network_mode_2g3g, 
                PhoneWrapper.NT_GSM_WCDMA_AUTO));
        list.add(new NetworkModeItem(R.drawable.shortcut_network_mode_3g2g, 
                PhoneWrapper.NT_WCDMA_PREFERRED));
        list.add(new NetworkModeItem(R.drawable.shortcut_network_mode_3g, 
                PhoneWrapper.NT_WCDMA_ONLY));
        list.add(new NetworkModeItem(R.drawable.shortcut_network_mode_cdma_evdo, 
                PhoneWrapper.NT_CDMA_EVDO));
        list.add(new NetworkModeItem(R.drawable.shortcut_network_mode_cdma, 
                PhoneWrapper.NT_CDMA_ONLY));
        list.add(new NetworkModeItem(R.drawable.shortcut_network_mode_evdo, 
                PhoneWrapper.NT_EVDO_ONLY));
        list.add(new NetworkModeItem(R.drawable.shortcut_network_mode_2g3g,
                PhoneWrapper.NT_GLOBAL));
        list.add(new NetworkModeItem(R.drawable.shortcut_network_mode_lte_cdma, 
                PhoneWrapper.NT_LTE_CDMA_EVDO));
        list.add(new NetworkModeItem(R.drawable.shortcut_network_mode_lte_gsm, 
                PhoneWrapper.NT_LTE_GSM_WCDMA));
        list.add(new NetworkModeItem(R.drawable.shortcut_network_mode_lte_global, 
                PhoneWrapper.NT_LTE_CMDA_EVDO_GSM_WCDMA));

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
            .setTitle(getText())
            .setAdapter(new IconListAdapter(mContext, list), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    NetworkModeItem item = (NetworkModeItem) list.get(which);

                    Intent launchIntent = new Intent(mContext, LaunchActivity.class);
                    launchIntent.setAction(ShortcutActivity.ACTION_LAUNCH_ACTION);
                    launchIntent.putExtra(ShortcutActivity.EXTRA_ACTION, getAction());
                    launchIntent.putExtra(PhoneWrapper.EXTRA_NETWORK_TYPE, item.getNetworkMode());
                    launchIntent.putExtra(ShortcutActivity.EXTRA_ACTION_TYPE, getActionType());
                    launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    Intent intent = new Intent();
                    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent);
                    intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, item.getText());
                    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, item.getIconResource());

                    NetworkModeShortcut.this.onShortcutCreated(intent, listener);
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
        launchIntent.putExtra(PhoneWrapper.EXTRA_NETWORK_TYPE,
                intent.getIntExtra(PhoneWrapper.EXTRA_NETWORK_TYPE, 0));
        context.sendBroadcast(launchIntent);
    }

    class NetworkModeItem implements IIconListAdapterItem {
        private int mIconResId;
        private int mNetworkMode;

        public NetworkModeItem(int iconResId, int networkMode) {
            mIconResId = iconResId;
            mNetworkMode = networkMode;
        }

        @Override
        public String getText() {
            return PhoneWrapper.getNetworkModeNameFromValue(mNetworkMode);
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

        public int getNetworkMode() {
            return mNetworkMode;
        }
    }
}
