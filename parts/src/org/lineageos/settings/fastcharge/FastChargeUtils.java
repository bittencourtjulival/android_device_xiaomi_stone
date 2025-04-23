/*
 * Copyright (C) 2025 The LineageOS Project
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

package org.lineageos.settings.fastcharge;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.preference.PreferenceManager;

import org.lineageos.settings.utils.FileUtils;

public class FastChargeUtils {

    private static final String TAG = "FastChargeUtils";
    public static final String NORMAL_CHARGE_NODE = "/sys/kernel/fastchgtoggle/mode";
    public static final String USB_CHARGE_NODE = "/sys/kernel/fast_charge/force_fast_charge";
    public static final String THERMAL_BOOST_NODE = "/sys/kernel/fastchgtoggle/thermals";
    public static final String BYPASS_CHARGE_NODE = "/sys/class/qcom-battery/input_suspend";
    
    private static final String PREF_NORMAL_CHARGE = "fastcharge_normal";
    private static final String PREF_USB_CHARGE = "fastcharge_usb";
    private static final String PREF_THERMAL_BOOST = "thermal_boost";
    private static final String PREF_BYPASS_CHARGE = "bypass_charge";

    // Charging modes
    public static final int MODE_30W = 2;
    public static final int MODE_15W = 1;
    public static final int MODE_8W = 0;

    // Bypass modes
    public static final int BYPASS_DISABLED = 0;
    public static final int BYPASS_ENABLED = 1;

    private SharedPreferences mSharedPrefs;

    public FastChargeUtils(Context context) {
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public int getNormalFastChargeMode() {
        try {
            String value = FileUtils.readOneLine(NORMAL_CHARGE_NODE);
            return value != null ? Integer.parseInt(value) : MODE_30W;
        } catch (Exception e) {
            Log.e(TAG, "Failed to read normal fast charge status", e);
            return MODE_30W;
        }
    }

    public boolean isUsbFastChargeEnabled() {
        try {
            String value = FileUtils.readOneLine(USB_CHARGE_NODE);
            return value != null && value.equals("1");
        } catch (Exception e) {
            Log.e(TAG, "Failed to read USB fast charge status", e);
            return false;
        }
    }

    public boolean isThermalBoostEnabled() {
        try {
            String value = FileUtils.readOneLine(THERMAL_BOOST_NODE);
            return value != null && value.equals("1");
        } catch (Exception e) {
            Log.e(TAG, "Failed to read thermal boost status", e);
            return false;
        }
    }

    public boolean isBypassChargeEnabled() {
        try {
            String value = FileUtils.readOneLine(BYPASS_CHARGE_NODE);
            return value != null && value.equals("1");
        } catch (Exception e) {
            Log.e(TAG, "Failed to read bypass charge status", e);
            return false;
        }
    }

    public void setNormalFastChargeMode(String mode) {
        try {
            FileUtils.writeLine(NORMAL_CHARGE_NODE, mode);
            mSharedPrefs.edit().putString(PREF_NORMAL_CHARGE, mode).apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to write normal fast charge status", e);
        }
    }

    public void enableUsbFastCharge(boolean enable) {
        try {
            FileUtils.writeLine(USB_CHARGE_NODE, enable ? "1" : "0");
            mSharedPrefs.edit().putBoolean(PREF_USB_CHARGE, enable).apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to write USB fast charge status", e);
        }
    }

    public void enableThermalBoost(boolean enable) {
        try {
            FileUtils.writeLine(THERMAL_BOOST_NODE, enable ? "1" : "0");
            mSharedPrefs.edit().putBoolean(PREF_THERMAL_BOOST, enable).apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to write thermal boost status", e);
        }
    }

    public void enableBypassCharge(boolean enable) {
        try {
            FileUtils.writeLine(BYPASS_CHARGE_NODE, enable ? "1" : "0");
            mSharedPrefs.edit().putBoolean(PREF_BYPASS_CHARGE, enable).apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to write bypass charge status", e);
        }
    }
    
    public boolean isNodeAccessible(String node) {
        try {
            String value = FileUtils.readOneLine(node);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Node " + node + " not accessible", e);
            return false;
        }
    }
    
    public boolean isThermalBoostSupported() {
        return isNodeAccessible(THERMAL_BOOST_NODE);
    }

    public boolean isBypassChargeSupported() {
        return isNodeAccessible(BYPASS_CHARGE_NODE);
    }
}
