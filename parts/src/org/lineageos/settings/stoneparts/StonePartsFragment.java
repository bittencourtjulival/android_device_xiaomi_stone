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

package org.lineageos.settings.stoneparts;

import android.content.Intent;
import android.os.Bundle;
import androidx.preference.Preference;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;
import org.lineageos.settings.R;

public class StonePartsFragment extends SettingsBasePreferenceFragment {

    private static final String KEY_CORE_CONTROL = "core_control";
    private static final String KEY_FAST_CHARGE = "fast_charge";
    private static final String KEY_ZRAM = "zram";
    private static final String KEY_KERNEL_MANAGER = "kernel_manager";
    private static final String KEY_GPU_MANAGER = "gpu_manager";
    private static final String KEY_THERMAL = "thermal";
    private static final String KEY_HBM = "hbm";
    private static final String KEY_DC_DIMMING = "dc_dimming";
    private static final String KEY_CLEAR_SPEAKER = "clear_speaker";
    private static final String KEY_BATTERY_STATUS = "battery_status";

    private static final java.util.Map<String, String> TARGETS;
    static {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        map.put(KEY_CORE_CONTROL, "org.lineageos.settings.corecontrol.CoreControlActivity");
        map.put(KEY_FAST_CHARGE, "org.lineageos.settings.fastcharge.FastChargeActivity");
        map.put(KEY_ZRAM, "org.lineageos.settings.zram.ZramActivity");
        map.put(KEY_KERNEL_MANAGER, "org.lineageos.settings.kernelmanager.KernelManagerActivity");
        map.put(KEY_GPU_MANAGER, "org.lineageos.settings.gpumanager.GpuManagerActivity");
        map.put(KEY_THERMAL, "org.lineageos.settings.thermal.ThermalActivity");
        map.put(KEY_HBM, "org.lineageos.settings.hbm.HBMActivity");
        map.put(KEY_DC_DIMMING, "org.lineageos.settings.display.DcDimmingSettingsActivity");
        map.put(KEY_CLEAR_SPEAKER, "org.lineageos.settings.speaker.ClearSpeakerActivity");
        map.put(KEY_BATTERY_STATUS, "org.lineageos.settings.batterystatus.BatteryStatusActivity");
        TARGETS = java.util.Collections.unmodifiableMap(map);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.stone_parts_settings, rootKey);

        for (java.util.Map.Entry<String, String> entry : TARGETS.entrySet()) {
            Preference pref = findPreference(entry.getKey());
            final String targetClass = entry.getValue();
            if (pref != null) {
                pref.setOnPreferenceClickListener(click -> {
                    openActivity(targetClass);
                    return true;
                });
            }
        }
    }

    private void openActivity(String fqcn) {
        if (getActivity() == null || fqcn == null) return;
        try {
            Intent intent = new Intent();
            intent.setClassName(getActivity().getPackageName(), fqcn);

            if (intent.resolveActivity(getActivity().getPackageManager()) == null) {
                intent.setClassName("", fqcn);
            }
            startActivity(intent);
        } catch (Exception e) {
            android.util.Log.w("StonePartsFragment", "Failed to open " + fqcn, e);
        }
    }
}
