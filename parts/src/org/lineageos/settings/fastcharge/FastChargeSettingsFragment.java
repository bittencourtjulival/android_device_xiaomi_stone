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
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.ListPreference;
import androidx.preference.SwitchPreference;

import org.lineageos.settings.R;

public class FastChargeSettingsFragment extends PreferenceFragment
    implements Preference.OnPreferenceChangeListener {

    private static final String KEY_NORMAL_CHARGER = "fastcharge_normal";
    private static final String KEY_USB_CHARGER = "fastcharge_usb";

    private ListPreference mNormalChargerPreference;
    private SwitchPreference mUsbChargerPreference;
    private FastChargeUtils mFastChargeUtils;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.fastcharge_settings, rootKey);
        
        mFastChargeUtils = new FastChargeUtils(getActivity());
        
        mNormalChargerPreference = (ListPreference) findPreference(KEY_NORMAL_CHARGER);
        mUsbChargerPreference = (SwitchPreference) findPreference(KEY_USB_CHARGER);

        boolean normalChargeSupported = mFastChargeUtils.isNodeAccessible(FastChargeUtils.NORMAL_CHARGE_NODE);
        boolean usbChargeSupported = mFastChargeUtils.isNodeAccessible(FastChargeUtils.USB_CHARGE_NODE);

        if (mNormalChargerPreference != null) {
            mNormalChargerPreference.setEnabled(normalChargeSupported);
            if (normalChargeSupported) {
                int mode = mFastChargeUtils.getNormalFastChargeMode();
                mNormalChargerPreference.setValue(String.valueOf(mode));
                updateNormalChargeSummary(mode);
                mNormalChargerPreference.setOnPreferenceChangeListener(this);
            } else {
                mNormalChargerPreference.setSummary(R.string.fastcharge_normal_unavailable);
            }
        }

        if (mUsbChargerPreference != null) {
            mUsbChargerPreference.setEnabled(usbChargeSupported);
            if (usbChargeSupported) {
                mUsbChargerPreference.setChecked(mFastChargeUtils.isUsbFastChargeEnabled());
                mUsbChargerPreference.setOnPreferenceChangeListener(this);
            } else {
                mUsbChargerPreference.setSummary(R.string.fastcharge_usb_unavailable);
            }
        }
    }

    private void updateNormalChargeSummary(int mode) {
        String[] entries = getResources().getStringArray(R.array.fastcharge_modes_entries);
        if (mode >= 0 && mode < entries.length) {
            mNormalChargerPreference.setSummary(entries[mode]);
        } else {
            mNormalChargerPreference.setSummary(R.string.fastcharge_normal_summary);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String key = preference.getKey();
        
        switch (key) {
            case KEY_NORMAL_CHARGER:
                String mode = (String) newValue;
                mFastChargeUtils.setNormalFastChargeMode(mode);
                updateNormalChargeSummary(Integer.parseInt(mode));
                return true;
            case KEY_USB_CHARGER:
                boolean value = (Boolean) newValue;
                mFastChargeUtils.enableUsbFastCharge(value);
                return true;
        }
        return false;
    }
}    
