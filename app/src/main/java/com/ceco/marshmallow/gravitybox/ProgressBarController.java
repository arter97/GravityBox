/*
 * Copyright (C) 2015 Peter Gregus for GravityBox Project (C3C076@xda)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ceco.marshmallow.gravitybox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.service.notification.StatusBarNotification;
import android.widget.RemoteViews;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ProgressBarController implements BroadcastSubReceiver {
    private static final String TAG = "GB:ProgressBarController";
    private static final boolean DEBUG = false;

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static final List<String> SUPPORTED_PACKAGES = new ArrayList<String>(Arrays.asList(
            "com.android.providers.downloads",
            "com.android.bluetooth",
            "com.mediatek.bluetooth"
    ));

    public interface ProgressStateListener {
        void onProgressTrackingStarted(boolean isBluetooth, Mode mode);
        void onProgressUpdated(ProgressInfo pInfo);
        void onProgressTrackingStopped();
        void onModeChanged(Mode mode);
        void onPreferencesChanged(Intent intent);
    }

    public class ProgressInfo {
        boolean hasProgressBar;
        int progress;
        int max;

        public ProgressInfo(boolean hasProgressBar, int progress, int max) {
            this.hasProgressBar = hasProgressBar;
            this.progress = progress;
            this.max = max;
        }

        public float getFraction() {
            return (max > 0 ? ((float)progress/(float)max) : 0f);
        }
    }

    public enum Mode { OFF, TOP, BOTTOM };

    private String mId;
    private List<ProgressStateListener> mListeners;
    private Mode mMode;

    public ProgressBarController(XSharedPreferences prefs) {
        mListeners = new ArrayList<ProgressStateListener>();
        mMode = Mode.valueOf(prefs.getString(GravityBoxSettings.PREF_KEY_STATUSBAR_DOWNLOAD_PROGRESS, "OFF"));
    }

    public void registerListener(ProgressStateListener listener) {
        if (listener == null) return;
        synchronized (mListeners) {
            if (!mListeners.contains(listener)) {
                mListeners.add(listener);
            }
        }
    }

    public void unregisterListener(ProgressStateListener listener) {
        if (listener == null) return;
        synchronized (mListeners) {
            if (mListeners.contains(listener)) {
                mListeners.remove(listener);
            }
        }
    }

    private void notifyProgressStarted(boolean isBluetooth) {
        synchronized (mListeners) {
            for (ProgressStateListener l : mListeners) {
                l.onProgressTrackingStarted(isBluetooth, mMode);
            }
        }
    }

    private void notifyProgressUpdated(ProgressInfo pInfo) {
        synchronized (mListeners) {
            for (ProgressStateListener l : mListeners) {
                l.onProgressUpdated(pInfo);
            }
        }
    }

    private void notifyProgressStopped() {
        synchronized (mListeners) {
            for (ProgressStateListener l : mListeners) {
                l.onProgressTrackingStopped();
            }
        }
    }

    private void notifyModeChanged() {
        synchronized (mListeners) {
            for (ProgressStateListener l : mListeners) {
                l.onModeChanged(mMode);
            }
        }
    }

    private void notifyPreferencesChanged(Intent intent) {
        synchronized (mListeners) {
            for (ProgressStateListener l : mListeners) {
                l.onPreferencesChanged(intent);
            }
        }
    }

    public void onNotificationAdded(StatusBarNotification statusBarNotif) {
        if (mMode == Mode.OFF) return;

        if (!verifyNotification(statusBarNotif)) {
            if (DEBUG) log("onNotificationAdded: ignoring unsupported notification");
            return;
        }

        if (mId != null) {
            if (DEBUG) log("onNotificationAdded: another download already registered");
            return;
        }

        mId = getIdentifier(statusBarNotif);
        if (mId != null) {
            if (DEBUG) log("starting progress for " + mId);
            startTracking();
            updateProgress(statusBarNotif);
        }
    }

    public void onNotificationUpdated(StatusBarNotification statusBarNotif) {
        if (mMode == Mode.OFF) return;

        if (mId == null) {
            // treat it as if it was added, e.g. to show progress in case
            // feature has been enabled during already ongoing download
            onNotificationAdded(statusBarNotif);
            return;
        }

        if (mId.equals(getIdentifier(statusBarNotif))) {
            // if notification became clearable, stop tracking immediately
            if (statusBarNotif.isClearable()) {
                if (DEBUG) log("onNotificationUpdated: notification became clearable - stopping tracking");
                stopTracking();
            } else {
                if (DEBUG) log("updating progress for " + mId);
                updateProgress(statusBarNotif);
            }
        }
    }

    public void onNotificationRemoved(StatusBarNotification statusBarNotif) {
        if (mMode == Mode.OFF) return;

        if (mId == null) {
            if (DEBUG) log("onNotificationRemoved: no download registered");
            return;
        } else if (mId.equals(getIdentifier(statusBarNotif))) {
            if (DEBUG) log("finishing progress for " + mId);
            stopTracking();
        }
    }

    private boolean verifyNotification(StatusBarNotification statusBarNotif) {
        if (statusBarNotif == null || statusBarNotif.isClearable()) {
            return false;
        }

        Notification n = statusBarNotif.getNotification();
        return (n != null && 
               (SUPPORTED_PACKAGES.contains(statusBarNotif.getPackageName()) ||
                n.extras.getBoolean(ModLedControl.NOTIF_EXTRA_PROGRESS_TRACKING)) &&
                getProgressInfo(n).hasProgressBar);
    }

    private String getIdentifier(StatusBarNotification statusBarNotif) {
        if (statusBarNotif == null) return null;
        String pkgName = statusBarNotif.getPackageName();
        if (SUPPORTED_PACKAGES.get(0).equals(pkgName)) {
            String tag = statusBarNotif.getTag();
            if (tag != null && tag.contains(":")) {
                return pkgName + ":" + tag.substring(tag.indexOf(":")+1);
            }
            if (DEBUG) log("getIdentifier: Unexpected notification tag: " + tag);
        } else {
            return (pkgName + ":" + String.valueOf(statusBarNotif.getId()));
        }
        return null;
    }

    private void startTracking() {
        notifyProgressStarted(mId.startsWith(SUPPORTED_PACKAGES.get(1)) ||
                mId.startsWith(SUPPORTED_PACKAGES.get(2)));
    }

    private void updateProgress(StatusBarNotification statusBarNotif) {
        if (statusBarNotif != null) {
            ProgressInfo pInfo = getProgressInfo(statusBarNotif.getNotification());
            if (DEBUG) log("updateProgress: newScaleX=" + pInfo.getFraction());
            notifyProgressUpdated(pInfo);
        }
    }

    private void stopTracking() {
        mId = null;
        notifyProgressStopped();
    }

    private ProgressInfo getProgressInfo(Notification n) {
        ProgressInfo pInfo = new ProgressInfo(false, 0, 0);
        if (n == null) return pInfo;

        // We have to extract the information from the content view
        RemoteViews views = n.bigContentView;
        if (views == null) views = n.contentView;
        if (views == null) return pInfo;

        try {
            @SuppressWarnings("unchecked")
            List<Parcelable> actions = (List<Parcelable>) 
                XposedHelpers.getObjectField(views, "mActions");
            if (actions == null) return pInfo;

            for (Parcelable p : actions) {
                Parcel parcel = Parcel.obtain();
                p.writeToParcel(parcel, 0);
                parcel.setDataPosition(0);

                // The tag tells which type of action it is (2 is ReflectionAction)
                int tag = parcel.readInt();
                if (tag != 2)  {
                    parcel.recycle();
                    continue;
                }

                parcel.readInt(); // skip View ID
                String methodName = parcel.readString();
                if ("setMax".equals(methodName)) {
                    parcel.readInt(); // skip type value
                    pInfo.max = parcel.readInt();
                    if (DEBUG) log("getProgressInfo: total=" + pInfo.max);
                } else if ("setProgress".equals(methodName)) {
                    parcel.readInt(); // skip type value
                    pInfo.progress = parcel.readInt();
                    pInfo.hasProgressBar = true;
                    if (DEBUG) log("getProgressInfo: current=" + pInfo.progress);
                }

                parcel.recycle();
            }
        } catch (Throwable  t) {
            XposedBridge.log(t);
        }

        return pInfo;
    }

    @Override
    public void onBroadcastReceived(Context context, Intent intent) {
        if (intent.getAction().equals(GravityBoxSettings.ACTION_PREF_STATUSBAR_DOWNLOAD_PROGRESS_CHANGED)) {
            if (intent.hasExtra(GravityBoxSettings.EXTRA_STATUSBAR_DOWNLOAD_PROGRESS_ENABLED)) {
                mMode = Mode.valueOf(intent.getStringExtra(
                        GravityBoxSettings.EXTRA_STATUSBAR_DOWNLOAD_PROGRESS_ENABLED));
                notifyModeChanged();
                if (mMode == Mode.OFF) {
                    stopTracking();
                }
            } else {
                notifyPreferencesChanged(intent);
            }
        }
    }
}
