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

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.lineageos.settings.R;

public class BatteryStatusFragment extends Fragment {

    private static final String PREFS = "BatteryStatusPrefs";
    private static final long UPDATE_INTERVAL_MS = 5000L;

    private TextView statusText, levelText, voltageText, powerText, currentText, tempText;
    private TextView screenOnText, screenOffText, awakeText, deepSleepText;
    private Switch notifySwitch;
    private SharedPreferences prefs;
    private PowerManager pm;
    private Context ctx;
    private Handler handler;
    private Runnable updateRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.batterystatus_fragment, container, false);
        ctx = requireContext();
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        handler = new Handler();

        statusText = v.findViewById(R.id.status);
        levelText = v.findViewById(R.id.level);
        voltageText = v.findViewById(R.id.voltage);
        powerText = v.findViewById(R.id.power);
        currentText = v.findViewById(R.id.current);
        tempText = v.findViewById(R.id.temperature);
        screenOnText = v.findViewById(R.id.screen_on);
        screenOffText = v.findViewById(R.id.screen_off);
        awakeText = v.findViewById(R.id.awake_with_screen_off);
        deepSleepText = v.findViewById(R.id.deep_sleep);
        notifySwitch = v.findViewById(R.id.switch_notify);

        notifySwitch.setChecked(prefs.getBoolean("notifyEnabled", false));
        notifySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("notifyEnabled", isChecked).apply();
            Intent svc = new Intent(ctx, BatteryNotificationService.class);
            if (isChecked) ctx.startForegroundService(svc);
            else ctx.stopService(svc);
        });

        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateBatteryInfo();
                handler.postDelayed(this, UPDATE_INTERVAL_MS);
            }
        };

        updateBatteryInfo();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateBatteryInfo();
        handler.postDelayed(updateRunnable, UPDATE_INTERVAL_MS);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(updateRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (handler != null) {
            handler.removeCallbacks(updateRunnable);
        }
    }

    private void updateBatteryInfo() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent b = ctx.registerReceiver(null, ifilter);
        if (b == null) return;

        int status = b.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        String statusStr;
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                statusStr = "Charging";
                break;
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                statusStr = "Discharging";
                break;
            case BatteryManager.BATTERY_STATUS_FULL:
                statusStr = "Charged";
                break;
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                statusStr = "Not charging";
                break;
            default:
                statusStr = "Unknown";
        }
        statusText.setText("⚡ " + statusStr);

        int level = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = b.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int pct = (int) ((level / (float) scale) * 100f);
        levelText.setText("Level: " + pct + "%");

        int voltageMv = b.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
        float voltageV = voltageMv > 0 ? voltageMv / 1000f : 0f;
        voltageText.setText(String.format("Voltage: %.2f V", voltageV));

        BatteryManager bm = (BatteryManager) ctx.getSystemService(Context.BATTERY_SERVICE);
        float currentmA = 0f;
        float tempC = 0f;
        if (bm != null) {
            try {
                long microA = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
                currentmA = Math.abs(microA) / 1000f;
            } catch (Exception ignored) {}

            try {
                Intent batt = ctx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if (batt != null) {
                    int tempTenths = batt.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
                    tempC = tempTenths / 10f;
                }
            } catch (Exception ignored) {}
        }

        float powerW = voltageV * (currentmA / 1000f);

        currentText.setText(String.format("⚡ Current: %.0f mA", currentmA));
        powerText.setText(String.format("🔋 Power: %.2f W", powerW));
        tempText.setText(String.format("🌡️ Temp: %.1f °C", tempC));

        long onDur = prefs.getLong("screenOnDuration", 0L);
        long awakeDur = prefs.getLong("awakeWithScreenOff", 0L);
        long deepDur = prefs.getLong("deepSleepDuration", 0L);
        long offDur = deepDur + awakeDur;

        screenOnText.setText("📱 Screen on: " + formatDuration(onDur));
        screenOffText.setText("🌙 Screen off: " + formatDuration(offDur));
        awakeText.setText("⚡ Awake: " + formatDuration(awakeDur));
        deepSleepText.setText("💤 DeepSleep: " + formatDuration(deepDur));
    }

    private String formatDuration(long ms) {
        if (ms <= 0) return "00h 00m 00s";
        long s = ms / 1000;
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        return String.format("%02dh %02dm %02ds", h, m, sec);
    }
}
