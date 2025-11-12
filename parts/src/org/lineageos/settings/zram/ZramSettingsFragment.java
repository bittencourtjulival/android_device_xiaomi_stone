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
package org.lineageos.settings.zram;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import org.lineageos.settings.R;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

public class ZramSettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_ZRAM_SIZE = "zram_size";
    private ListPreference mZramSizePreference;
    private ZramUtils mZramUtils;
    private int mActualRamGB;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.zram_settings, rootKey);
        mZramUtils = new ZramUtils(getActivity());

        // Get actual RAM capacity
        mActualRamGB = getActualRamCapacity();

        mZramSizePreference = (ListPreference) findPreference(KEY_ZRAM_SIZE);
        if (mZramSizePreference != null) {
            updateZramEntries();

            int currentSize = mZramUtils.getCurrentZramSize();
            mZramSizePreference.setValue(String.valueOf(currentSize));
            updateSummary(currentSize);
            mZramSizePreference.setOnPreferenceChangeListener(this);
        }
    }

    private int getActualRamCapacity() {
    try {
        BufferedReader reader = new BufferedReader(new FileReader("/proc/meminfo"));
        String line = reader.readLine();
        reader.close();

        if (line != null && line.startsWith("MemTotal:")) {
            String[] parts = line.split("\\s+");
            long memKB = Long.parseLong(parts[1]);
            double memGB = memKB / 1048576.0;

            // Determine actual RAM capacity based on available memory
            // Android reserves memory for kernel, so we round up to nearest standard size
            if (memGB >= 7.0) {
                return 8;
            } else if (memGB >= 5.1) {
                return 6;
            } else {
                return 4;
            }
        }
    } catch (IOException | NumberFormatException e) {
        e.printStackTrace();
    }

    ActivityManager actManager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
    ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
    actManager.getMemoryInfo(memInfo);
    double memGB = memInfo.totalMem / 1073741824.0;

    if (memGB >= 7.0) {
        return 8;
    } else if (memGB >= 5.1) {
        return 6;
    } else {
        return 4;
    }
}

    private void updateZramEntries() {
        String[] entries = new String[5];

        // Calculate sizes based on actual RAM capacity
        double quarterGB = mActualRamGB / 4.0;
        double halfGB = mActualRamGB / 2.0;
        double fullGB = mActualRamGB;

        entries[0] = getString(R.string.zram_disabled);
        entries[1] = String.format(Locale.getDefault(), getString(R.string.zram_size_quarter),
                                   formatGB(quarterGB));
        entries[2] = String.format(Locale.getDefault(), getString(R.string.zram_size_half),
                                   formatGB(halfGB));
        entries[3] = String.format(Locale.getDefault(), getString(R.string.zram_size_full),
                                   formatGB(fullGB));
        entries[4] = getString(R.string.zram_size_dynamic);

        mZramSizePreference.setEntries(entries);
    }

    private String formatGB(double gb) {
        if (gb == Math.floor(gb)) {
            return String.format(Locale.getDefault(), "%.0f", gb);
        } else {
            return String.format(Locale.getDefault(), "%.1f", gb);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (KEY_ZRAM_SIZE.equals(preference.getKey())) {
            try {
                int value = Integer.parseInt((String) newValue);
                mZramUtils.setZramSize(value);
                updateSummary(value);

                new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.zram_reboot_recommended)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    private void updateSummary(int value) {
        String summary;
        switch (value) {
            case 0:
                summary = getString(R.string.zram_disabled);
                break;
            case 25:
                double quarterGB = mActualRamGB / 4.0;
                summary = String.format(Locale.getDefault(),
                                      getString(R.string.zram_size_quarter),
                                      formatGB(quarterGB));
                break;
            case 50:
                double halfGB = mActualRamGB / 2.0;
                summary = String.format(Locale.getDefault(),
                                      getString(R.string.zram_size_half),
                                      formatGB(halfGB));
                break;
            case 100:
                summary = String.format(Locale.getDefault(),
                                      getString(R.string.zram_size_full),
                                      formatGB((double)mActualRamGB));
                break;
            default:
                summary = getString(R.string.zram_size_dynamic);
        }
        mZramSizePreference.setSummary(summary);
    }
}
