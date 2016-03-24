package com.ceco.marshmallow.gravitybox.managers;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

import com.ceco.marshmallow.gravitybox.GravityBoxSettings;
import com.ceco.marshmallow.gravitybox.ledcontrol.QuietHoursActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class SysUiManagers {
    private static final String TAG = "GB:SysUiManagers";

    public static BatteryInfoManager BatteryInfoManager;
    public static StatusBarIconManager IconManager;
    public static StatusbarQuietHoursManager QuietHoursManager;
    public static AppLauncher AppLauncher;
    public static KeyguardStateMonitor KeyguardMonitor;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void init(Context context, XSharedPreferences prefs) {
        if (context == null)
            throw new IllegalArgumentException("Context cannot be null");
        if (prefs == null)
            throw new IllegalArgumentException("Prefs cannot be null");

        createKeyguardMonitor(context, prefs);

        try {
            BatteryInfoManager = new BatteryInfoManager(context, prefs);
        } catch (Throwable t) {
            log("Error creating BatteryInfoManager: ");
            XposedBridge.log(t);
        }

        try {
            IconManager = new StatusBarIconManager(context, prefs);
        } catch (Throwable t) {
            log("Error creating IconManager: ");
            XposedBridge.log(t);
        }

        try {
            QuietHoursManager = StatusbarQuietHoursManager.getInstance(context);
        } catch (Throwable t) {
            log("Error creating QuietHoursManager: ");
            XposedBridge.log(t);
        }

        try {
            AppLauncher = new AppLauncher(context, prefs);
        } catch (Throwable t) {
            log("Error creating AppLauncher: ");
            XposedBridge.log(t);
        }

        IntentFilter intentFilter = new IntentFilter();
        // battery info manager
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        intentFilter.addAction(GravityBoxSettings.ACTION_PREF_BATTERY_SOUND_CHANGED);
        intentFilter.addAction(GravityBoxSettings.ACTION_PREF_LOW_BATTERY_WARNING_POLICY_CHANGED);
        intentFilter.addAction(com.ceco.marshmallow.gravitybox.managers.BatteryInfoManager.ACTION_POWER_SAVE_MODE_CHANGING);

        // icon manager
        intentFilter.addAction(GravityBoxSettings.ACTION_PREF_STATUSBAR_COLOR_CHANGED);

        // quiet hours manager
        intentFilter.addAction(Intent.ACTION_TIME_TICK);
        intentFilter.addAction(Intent.ACTION_TIME_CHANGED);
        intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        intentFilter.addAction(QuietHoursActivity.ACTION_QUIET_HOURS_CHANGED);

        // AppLauncher
        intentFilter.addAction(GravityBoxSettings.ACTION_PREF_APP_LAUNCHER_CHANGED);
        intentFilter.addAction(com.ceco.marshmallow.gravitybox.managers.AppLauncher.ACTION_SHOW_APP_LAUCNHER);

        // KeyguardStateMonitor
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(GravityBoxSettings.ACTION_PREF_POWER_CHANGED);
        intentFilter.addAction(GravityBoxSettings.ACTION_LOCKSCREEN_SETTINGS_CHANGED);

        context.registerReceiver(sBroadcastReceiver, intentFilter);
    }

    public static void createKeyguardMonitor(Context ctx, XSharedPreferences prefs) {
        if (KeyguardMonitor != null) return;
        try {
            KeyguardMonitor = new KeyguardStateMonitor(ctx, prefs);
        } catch (Throwable t) {
            log("Error creating KeyguardMonitor: ");
            XposedBridge.log(t);
        }
    }

    private static BroadcastReceiver sBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BatteryInfoManager != null) {
                BatteryInfoManager.onBroadcastReceived(context, intent);
            }
            if (IconManager != null) {
                IconManager.onBroadcastReceived(context, intent);
            }
            if (QuietHoursManager != null) {
                QuietHoursManager.onBroadcastReceived(context, intent);
            }
            if (AppLauncher != null) {
                AppLauncher.onBroadcastReceived(context, intent);
            }
            if (KeyguardMonitor != null) {
                KeyguardMonitor.onBroadcastReceived(context, intent);
            }
        }
    };
}
