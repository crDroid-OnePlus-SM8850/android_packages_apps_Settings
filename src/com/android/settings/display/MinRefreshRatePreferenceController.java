/*
 * Copyright (C) 2020 The LineageOS Project
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

package com.android.settings.display;

import static android.provider.Settings.System.MIN_REFRESH_RATE;

import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;
import android.view.Display;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MinRefreshRatePreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_MIN_REFRESH_RATE = "min_refresh_rate";

    private static final int LTPO_IDLE_REFRESH_RATE = 1;
    private static final int LTPO_LOW_REFRESH_RATE = 30;
    private static final int NO_MIN_REFRESH_RATE = 0;
    private static final String OPLUS_LTPO_MIN_FPS_PROPERTY = "persist.sys.oplus_ltpo_min_fps";

    private ListPreference mListPreference;

    private List<String> mEntries = new ArrayList<>();
    private List<String> mValues = new ArrayList<>();

    public MinRefreshRatePreferenceController(Context context) {
        super(context, KEY_MIN_REFRESH_RATE);

        if (mContext.getResources().getBoolean(R.bool.config_show_min_refresh_rate_switch)) {
            Display.Mode mode = mContext.getDisplay().getMode();
            Display.Mode[] modes = mContext.getDisplay().getSupportedModes();
            Arrays.sort(modes, (mode1, mode2) ->
                Float.compare(mode2.getRefreshRate(), mode1.getRefreshRate()));
            for (Display.Mode m : modes) {
                if (m.getPhysicalWidth() == mode.getPhysicalWidth() &&
                        m.getPhysicalHeight() == mode.getPhysicalHeight()) {
                    mEntries.add(String.format("%.02fHz", m.getRefreshRate())
                            .replaceAll("[\\.,]00", ""));
                    mValues.add(String.format(Locale.US, "%.02f", m.getRefreshRate()));
                }
            }
            mEntries.add(LTPO_LOW_REFRESH_RATE + "Hz");
            mValues.add(String.format(Locale.US, "%.02f", (float) LTPO_LOW_REFRESH_RATE));
            mEntries.add(LTPO_IDLE_REFRESH_RATE + "Hz");
            mValues.add(String.format(Locale.US, "%.02f", (float) NO_MIN_REFRESH_RATE));
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return mEntries.size() > 1 ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_MIN_REFRESH_RATE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mListPreference = screen.findPreference(getPreferenceKey());
        mListPreference.setEntries(mEntries.toArray(new String[mEntries.size()]));
        mListPreference.setEntryValues(mValues.toArray(new String[mValues.size()]));

        super.displayPreference(screen);
    }

    @Override
    public void updateState(Preference preference) {
        final float currentValue = Settings.System.getFloat(mContext.getContentResolver(),
                MIN_REFRESH_RATE, 60.00f);
        int index = mListPreference.findIndexOfValue(
                String.format(Locale.US, "%.02f", currentValue));
        if (index < 0) index = 0;
        mListPreference.setValueIndex(index);
        mListPreference.setSummary(mListPreference.getEntries()[index]);

        setOplusLtpoMinFps(currentValue);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final float minRefreshRate = Float.valueOf((String) newValue);
        Settings.System.putFloat(mContext.getContentResolver(), MIN_REFRESH_RATE, minRefreshRate);
        setOplusLtpoMinFps(minRefreshRate);
        updateState(preference);
        return true;
    }

    private void setOplusLtpoMinFps(float minRefreshRate) {
        SystemProperties.set(OPLUS_LTPO_MIN_FPS_PROPERTY,
                String.valueOf(minRefreshRate >= LTPO_LOW_REFRESH_RATE
                        ? (int) minRefreshRate : NO_MIN_REFRESH_RATE));
    }

}
