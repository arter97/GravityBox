package com.ceco.marshmallow.gravitybox.quicksettings;

import android.content.ComponentName;
import android.content.Intent;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XC_MethodHook.Unhook;

public class HotspotTile extends AospTile {
    public static final String AOSP_KEY = "hotspot";

    private static final Intent TETHER_SETTINGS = new Intent().setComponent(new ComponentName(
            "com.android.settings", "com.android.settings.TetherSettings"));

    private Unhook mLongClickHook;

    protected HotspotTile(Object host, Object tile, XSharedPreferences prefs,
            QsTileEventDistributor eventDistributor) throws Throwable {
        super(host, "aosp_tile_hotspot", tile, prefs, eventDistributor);

        createHooks();
    }

    @Override
    public String getAospKey() {
        return AOSP_KEY;
    }

    @Override
    public boolean handleLongClick() {
        startSettingsActivity(TETHER_SETTINGS);
        return true;
    }

    @Override
    public void handleDestroy() {
        super.handleDestroy();
        destroyHooks();
    }

    private void createHooks() {
        try {
            mLongClickHook = XposedHelpers.findAndHookMethod(mTile.getClass().getName(), 
                    mContext.getClassLoader(), "handleLongClick", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (handleLongClick()) {
                        param.setResult(null);
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private void destroyHooks() {
        if (mLongClickHook != null) {
            mLongClickHook.unhook();
            mLongClickHook = null;
        }
    }
}
