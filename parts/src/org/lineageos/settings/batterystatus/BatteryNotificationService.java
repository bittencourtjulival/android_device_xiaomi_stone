/*
 * Copyright (C) 2025 Julival Bittencourt
 *
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

package org.lineageos.settings.batterystatus;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.Locale;

public class BatteryNotificationService extends Service {
    private static final String CHANNEL_ID = "battery_status_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final long UPDATE_INTERVAL_MS = 5000L;
    private static final String PREFS = "BatteryStatusPrefs";

    private Handler handler;
    private SharedPreferences prefs;
    private PowerManager powerManager;
    private long lastUpdateTime;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        handler = new Handler();

        BatteryManager bm = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
        int initialLevel = bm != null ? bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) : 100;
        prefs.edit().putInt("lastBatteryLevel", initialLevel * 10).apply();
        prefs.edit().putLong("lastLevelUpdate", SystemClock.elapsedRealtime()).apply();
        prefs.edit().putInt("mahDropScreenOn", 0).apply();
        prefs.edit().putInt("mahDropScreenOff", 0).apply();

        registerReceiver(receiver, createIntentFilter());
        createNotificationChannel();

        // start foreground (notification will be updated regularly)
        startForeground(NOTIFICATION_ID, buildNotification());

        // initialize timing
        lastUpdateTime = SystemClock.elapsedRealtime();
        handler.post(updateRunnable);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateRunnable);
        try { unregisterReceiver(receiver); } catch (Exception ignored) {}
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            updateDurations();
            sampleBatteryLevel();
            updateNotification();
            handler.postDelayed(this, UPDATE_INTERVAL_MS);
        }
    };

    private IntentFilter createIntentFilter() {
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_SCREEN_OFF);
        f.addAction(Intent.ACTION_POWER_CONNECTED);
        f.addAction(Intent.ACTION_POWER_DISCONNECTED);
        f.addAction(Intent.ACTION_BATTERY_CHANGED);
        return f;
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            String a = intent.getAction();

            if (Intent.ACTION_SCREEN_ON.equals(a)) {
                prefs.edit().putBoolean("isScreenOn", true).apply();
            } else if (Intent.ACTION_SCREEN_OFF.equals(a)) {
                prefs.edit().putBoolean("isScreenOn", false).apply();
            } else if (Intent.ACTION_POWER_CONNECTED.equals(a) || Intent.ACTION_POWER_DISCONNECTED.equals(a)) {
                resetDurations();
                prefs.edit().putInt("mahDropScreenOn", 0).apply();
                prefs.edit().putInt("mahDropScreenOff", 0).apply();
                BatteryManager bm = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
                int currentLevel = bm != null ? bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) : 100;
                prefs.edit().putInt("lastBatteryLevel", currentLevel * 10).apply();
                lastUpdateTime = SystemClock.elapsedRealtime();
            } else if (Intent.ACTION_BATTERY_CHANGED.equals(a)) {
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                boolean charging = (status == BatteryManager.BATTERY_STATUS_CHARGING
                        || status == BatteryManager.BATTERY_STATUS_FULL);
                prefs.edit().putBoolean("isCharging", charging).apply();
            }
        }
    };

    private void resetDurations() {
        SharedPreferences.Editor e = prefs.edit();
        e.putLong("screenOnDuration", 0L);
        e.putLong("screenOffDuration", 0L);
        e.putLong("awakeWithScreenOff", 0L);
        e.putLong("deepSleepDuration", 0L);
        e.apply();
    }

    private void updateDurations() {
        long now = SystemClock.elapsedRealtime();
        long delta = now - lastUpdateTime;
        if (delta <= 0) {
            lastUpdateTime = now;
            return;
        }
        lastUpdateTime = now;

        boolean isScreenOn = prefs.getBoolean("isScreenOn", true);

        long screenOnDuration = prefs.getLong("screenOnDuration", 0L);
        long awakeWithScreenOff = prefs.getLong("awakeWithScreenOff", 0L);
        long deepSleepDuration = prefs.getLong("deepSleepDuration", 0L);

        boolean isDeviceIdle = false;
        try {
            if (powerManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                isDeviceIdle = powerManager.isDeviceIdleMode();
            }
        } catch (Throwable ignored) {}

        if (isScreenOn) {
            screenOnDuration += delta;
        } else {
            if (isDeviceIdle) {
                deepSleepDuration += delta;
            } else {
                awakeWithScreenOff += delta;
            }
        }

        long screenOffDuration = deepSleepDuration + awakeWithScreenOff;

        SharedPreferences.Editor e = prefs.edit();
        e.putLong("screenOnDuration", screenOnDuration);
        e.putLong("awakeWithScreenOff", awakeWithScreenOff);
        e.putLong("deepSleepDuration", deepSleepDuration);
        e.putLong("screenOffDuration", screenOffDuration);
        e.apply();
    }

    private void sampleBatteryLevel() {
        boolean charging = prefs.getBoolean("isCharging", false);
        
        BatteryManager bm = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
        if (bm == null) return;
        int currentLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        if (currentLevel < 0) return;

        int lastLevel = prefs.getInt("lastBatteryLevel", currentLevel * 10);

        if (charging || currentLevel * 10 >= lastLevel) {
            prefs.edit().putInt("lastBatteryLevel", currentLevel * 10).apply();
            return;
        }

        int levelDropped = lastLevel - (currentLevel * 10);
        
        long onTime = prefs.getLong("screenOnDuration", 0);
        long offTime = prefs.getLong("screenOffDuration", 0);
        long totalTime = onTime + offTime;
        
        if (totalTime > 0 && levelDropped > 0) {
            float onRatio = (float) onTime / totalTime;
            int onDrop = Math.round(levelDropped * onRatio);
            int offDrop = levelDropped - onDrop;
            
            int currentOn = prefs.getInt("mahDropScreenOn", 0);
            int currentOff = prefs.getInt("mahDropScreenOff", 0);
            
            prefs.edit().putInt("mahDropScreenOn", currentOn + onDrop).apply();
            prefs.edit().putInt("mahDropScreenOff", currentOff + offDrop).apply();
        }

        prefs.edit().putInt("lastBatteryLevel", currentLevel * 10).apply();
    }

    private void updateNotification() {
        Notification n = buildNotification();
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(NOTIFICATION_ID, n);
    }

    private Notification buildNotification() {
        long onDur = prefs.getLong("screenOnDuration", 0L);
        long awakeDur = prefs.getLong("awakeWithScreenOff", 0L);
        long deepDur = prefs.getLong("deepSleepDuration", 0L);
        long offDur = prefs.getLong("screenOffDuration", deepDur + awakeDur);

        BatteryManager bm = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
        int level = -1;
        int voltageMv = -1;
        float currentMilliA = 0f;
        float tempC = 0f;
        int status = -1;

        if (bm != null) {
            try {
                level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            } catch (Throwable ignored) {}

            try {
                long microA = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
                currentMilliA = Math.abs(microA) / 1000f;
            } catch (Throwable ignored) {
                currentMilliA = 0f;
            }

            try {
                java.lang.reflect.Field f = BatteryManager.class.getField("BATTERY_PROPERTY_VOLTAGE");
                int constVal = f != null ? f.getInt(null) : -1;
                if (constVal >= 0) {
                    try {
                        voltageMv = bm.getIntProperty(constVal);
                    } catch (Throwable ignored) {
                        voltageMv = -1;
                    }
                }
            } catch (Throwable ignored) {
                voltageMv = -1;
            }
        }

        if (voltageMv <= 0 || currentMilliA == 0f || status < 0) {
            try {
                Intent batt = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if (batt != null) {
                    if (voltageMv <= 0) {
                        voltageMv = batt.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
                    }
                    if (level < 0) {
                        level = batt.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    }
                    if (status < 0) {
                        status = batt.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                    }
                    int tempTenths = batt.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE);
                    if (tempTenths != Integer.MIN_VALUE && tempTenths != 0) {
                        tempC = tempTenths / 10f;
                    }
                }
            } catch (Throwable ignored) {}
        }

        String statusStr;
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                statusStr = "Charging";
                break;
            case BatteryManager.BATTERY_STATUS_FULL:
                statusStr = "Charged";
                break;
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                statusStr = "Discharging";
                break;
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                statusStr = "Not charging";
                break;
            default:
                statusStr = "Unknown";
        }

        float voltageV = (voltageMv > 0) ? (voltageMv / 1000f) : 0f;
        float currentA = currentMilliA / 1000f;
        float powerW = 0f;
        if (voltageV > 0f && currentA > 0f) {
            powerW = voltageV * currentA;
        }

        int dropOnPctTenths = prefs.getInt("mahDropScreenOn", 0);
        int dropOffPctTenths = prefs.getInt("mahDropScreenOff", 0);
        
        float dropOnPct = dropOnPctTenths / 10.0f;
        float dropOffPct = dropOffPctTenths / 10.0f;
        
        double nominalCapacity = BatteryInfoUtils.getBatteryCapacityNominal();
        int dropOnMah = (int) ((dropOnPctTenths * nominalCapacity) / 1000.0);
        int dropOffMah = (int) ((dropOffPctTenths * nominalCapacity) / 1000.0);
        
        float activeRate = 0f;
        float idleRate = 0f;
        if (onDur >= 60000L && dropOnPctTenths > 0) {
            activeRate = (float) ((dropOnPctTenths / 10.0) / (onDur / 3600000.0));
        }
        if (offDur >= 60000L && dropOffPctTenths > 0) {
            idleRate = (float) ((dropOffPctTenths / 10.0) / (offDur / 3600000.0));
        }

        float awakePct = 0f;
        float deepPct = 0f;
        if (offDur > 0) {
            awakePct = (awakeDur * 100.0f) / offDur;
            deepPct = (deepDur * 100.0f) / offDur;
        }

        String line1 = String.format("🔋 Power: %.2fW | %.0fmA   🌡️ %.1f°C", powerW, currentMilliA, tempC);
        String line2 = String.format("📉 Active: %.1f%%/h   Idle: %.1f%%/h", activeRate, idleRate);
        String line3 = String.format("📱 ScreenOn: %s • %.1f%%(%dmAh)", formatDurationCompact(onDur), dropOnPct, dropOnMah);
        String line4 = String.format("🌙 ScreenOff: %s • %.1f%%(%dmAh)", formatDurationCompact(offDur), dropOffPct, dropOffMah);
        String line5 = String.format("⚡ Awake: %s • (%.1f%%)", formatDurationCompact(awakeDur), awakePct);
        String line6 = String.format("💤 DeepSleep: %s • (%.1f%%)", formatDurationCompact(deepDur), deepPct);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(level >= 0 ? ("Battery: " + level + "% (" + statusStr + ")") : "Battery Status")
                .setContentText(line1)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                        line1 + "\n" + line2 + "\n" + line3 + "\n" + line4 + "\n" + line5 + "\n" + line6))
                .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
                .setOngoing(true);

        return nb.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Battery Status", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Battery usage and screen activity monitor");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private String formatDuration(long ms) {
        if (ms <= 0) return "00h 00m 00s";
        long s = ms / 1000;
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        return String.format("%02dh %02dm %02ds", h, m, sec);
    }

    private String formatDurationCompact(long ms) {
        if (ms <= 0) return "00:00:00";
        long s = ms / 1000;
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        return String.format("%02d:%02d:%02d", h, m, sec);
    }
}
