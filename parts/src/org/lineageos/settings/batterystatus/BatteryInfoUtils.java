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
import android.os.BatteryManager;
import android.os.SystemClock;

import java.util.Locale;

public class BatteryInfoUtils {

    public static float getVoltage() {
        Intent batteryIntent = getBatteryIntent();
        if (batteryIntent != null) {
            int mv = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
            if (mv > 0) return mv;
        }
        return 0f;
    }

    public static float getCurrent() {
        try {
            BatteryManager bm = (BatteryManager)
                    AppContextProvider.getContext().getSystemService(Context.BATTERY_SERVICE);
            if (bm != null) {
                long microAmps = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
                return Math.abs(microAmps) / 1000f; // µA → mA
            }
        } catch (Exception ignored) {}
        return 0f;
    }

    public static float getTemperature() {
        Intent batteryIntent = getBatteryIntent();
        if (batteryIntent != null) {
            int tenths = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
            if (tenths > 0) return tenths / 10f;
        }
        return 0f;
    }

    public static float getPower() {
        float voltage = getVoltage();
        float current = getCurrent();
        return (voltage * current) / 1000f;
    }

    private static Intent getBatteryIntent() {
        try {
            Context ctx = AppContextProvider.getContext();
            return ctx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        } catch (Exception e) {
            return null;
        }
    }

    public static String formatDuration(long millis) {
        if (millis <= 0) return "00h 00m 00s";
        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02dh %02dm %02ds", hours, minutes, seconds);
    }

    public static String getUptime() {
        long uptime = SystemClock.elapsedRealtime();
        return formatDuration(uptime);
    }
}
