package com.ceco.marshmallow.gravitybox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ceco.marshmallow.gravitybox.quicksettings.AospTile;
import com.ceco.marshmallow.gravitybox.quicksettings.QsPanel;
import com.ceco.marshmallow.gravitybox.quicksettings.QsQuickPulldownHandler;
import com.ceco.marshmallow.gravitybox.quicksettings.QsTile;
import com.ceco.marshmallow.gravitybox.quicksettings.QsTileEventDistributor;
import com.ceco.marshmallow.gravitybox.quicksettings.TileOrderActivity;

import android.content.Context;
import android.content.res.XModuleResources;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;

public class ModQsTiles {
    public static final String PACKAGE_NAME = "com.android.systemui";
    public static final String TAG = "GB:ModQsTile";
    public static final boolean DEBUG = false;

    public static final String CLASS_TILE_HOST = "com.android.systemui.statusbar.phone.QSTileHost";
    public static final String TILES_SETTING = "sysui_qs_tiles";

    public static final List<String> GB_TILE_KEYS = new ArrayList<String>(Arrays.asList(
            "gb_tile_battery",
            "gb_tile_nfc",
            "gb_tile_gps_slimkat",
            "gb_tile_gps_alt",
            "gb_tile_ringer_mode",
            "gb_tile_volume",
            "gb_tile_network_mode",
            "gb_tile_smart_radio",
            "gb_tile_sync",
            "gb_tile_torch",
            "gb_tile_sleep",
            "gb_tile_stay_awake",
            "gb_tile_quickrecord",
            "gb_tile_quickapp",
            "gb_tile_quickapp2",
            "gb_tile_expanded_desktop",
            "gb_tile_screenshot",
            "gb_tile_gravitybox",
            "gb_tile_usb_tether",
            "gb_tile_music",
            "gb_tile_lock_screen",
            "gb_tile_quiet_hours",
            "gb_tile_compass"
    ));

    public static class RES_IDS {
        public static int NM_TITLE;
        public static int RM_TITLE;
        public static int SA_TITLE;
    }

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    private static QsTileEventDistributor mEventDistributor;
    private static QsQuickPulldownHandler mQuickPulldownHandler;
    private static QsPanel mQsPanel;

    public static void initResources(final InitPackageResourcesParam resparam) {
        XModuleResources modRes = XModuleResources.createInstance(GravityBox.MODULE_PATH, resparam.res);
        RES_IDS.NM_TITLE = resparam.res.addResource(modRes, R.string.qs_tile_network_mode);
        RES_IDS.RM_TITLE = resparam.res.addResource(modRes, R.string.qs_tile_ringer_mode);
        RES_IDS.SA_TITLE = resparam.res.addResource(modRes, R.string.qs_tile_stay_awake);
    }

    public static void init(final XSharedPreferences prefs, final ClassLoader classLoader) {
        try {
            if (DEBUG) log("init");

            Class<?> classTileHost = XposedHelpers.findClass(CLASS_TILE_HOST, classLoader);
            mQsPanel = new QsPanel(prefs, classLoader);
            
            XposedHelpers.findAndHookMethod(classTileHost, "onTuningChanged",
                    String.class, String.class, new XC_MethodHook() {
                @SuppressWarnings("unchecked")
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!TILES_SETTING.equals(param.args[0])) return;

                    if (mEventDistributor != null) {
                        Map<String, Object> tileMap = (Map<String, Object>)
                                XposedHelpers.getObjectField(param.thisObject, "mTiles");
                        for (Entry<String,Object> entry : tileMap.entrySet()) {
                            XposedHelpers.callMethod(entry.getValue(), "handleDestroy");
                        }
                        tileMap.clear();
                        ((List<?>)XposedHelpers.getObjectField(param.thisObject, "mTileSpecs")).clear();
                    }
                }
                @SuppressWarnings("unchecked")
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!TILES_SETTING.equals(param.args[0])) return;

                    if (mEventDistributor == null) {
                        mEventDistributor = new QsTileEventDistributor(param.thisObject, prefs);
                        mQsPanel.setEventDistributor(mEventDistributor);
                        if (DEBUG) log("Tile event distributor created");
                    }

                    Map<String, Object> tileMap = (Map<String, Object>)
                            XposedHelpers.getObjectField(param.thisObject, "mTiles");
                    Map<String, Object> gbTileMap = new HashMap<String,Object>();
                    Context context = (Context) XposedHelpers.callMethod(param.thisObject, "getContext");

                    // prepare AOSP tile wrappers
                    for (Entry<String,Object> entry : tileMap.entrySet()) {
                        AospTile aospTile = AospTile.create(param.thisObject, entry.getValue(), 
                                entry.getKey(), prefs, mEventDistributor);
                        if (aospTile != null) {
                            gbTileMap.put(aospTile.getKey(), aospTile);
                        }
                    }

                    // prepare GB tiles
                    for (String key : GB_TILE_KEYS) {
                        QsTile tile = QsTile.create(param.thisObject, key, prefs,
                                mEventDistributor);
                        if (tile != null) {
                            gbTileMap.put(key, tile);
                        }
                    }

                    // sort tiles
                    LinkedHashMap<String, Object> orderedMap = new LinkedHashMap<String,Object>();
                    String[] orderedKeys = prefs.getString(
                            TileOrderActivity.PREF_KEY_TILE_ENABLED,
                            TileOrderActivity.getDefaultTileList(
                                    Utils.getGbContext(context))).split(",");
                    for (String key : orderedKeys) {
                         for (Entry<String, Object> entry : gbTileMap.entrySet()) {
                             if (entry.getValue() instanceof AospTile) {
                                 AospTile t = (AospTile) entry.getValue();
                                 if (key.equals(t.getKey())) {
                                     orderedMap.put(t.getAospKey(), tileMap.get(t.getAospKey()));
                                     tileMap.remove(t.getAospKey());
                                     if (DEBUG) log("orderedMap: added " + t.getKey());
                                     break;
                                 }
                             } else {
                                 QsTile t = (QsTile) entry.getValue();
                                 if (key.equals(t.getKey())) {
                                     orderedMap.put(key, t.getTile());
                                     gbTileMap.remove(key);
                                     if (DEBUG) log("orderedMap: added " + key);
                                     break;
                                 }
                             }
                         }
                    }

                    // add left-overs
                    for (Entry<String,Object> entry : tileMap.entrySet()) {
                        orderedMap.put(entry.getKey(), entry.getValue());
                        if (DEBUG) log("orderedMap: added disabled or unknown AOSP tile: " + entry.getKey());
                    }
                    for (Entry<String,Object> entry : gbTileMap.entrySet()) {
                        if (entry.getValue() instanceof QsTile) {
                            orderedMap.put(entry.getKey(), ((QsTile)entry.getValue()).getTile());
                            if (DEBUG) log("orderedMap: added disabled GB tile: " + entry.getKey());
                        }
                    }

                    // put all tiles into host tile map
                    tileMap.clear();
                    tileMap.putAll(orderedMap);

                    Object cb = XposedHelpers.getObjectField(param.thisObject, "mCallback");
                    if (cb != null) {
                        XposedHelpers.callMethod(cb, "onTilesChanged");
                    }

                    if (DEBUG) log("Tiles created");

                    if (mQuickPulldownHandler == null) {
                        mQuickPulldownHandler = new QsQuickPulldownHandler(context, prefs, mEventDistributor);
                    }
                    mQsPanel.updateResources();
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}
