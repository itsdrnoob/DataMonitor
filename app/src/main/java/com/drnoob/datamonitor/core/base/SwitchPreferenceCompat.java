/*
 * Copyright (C) 2021 Dr.NooB
 *
 * This file is a part of Data Monitor <https://github.com/itsdrnoob/DataMonitor>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.drnoob.datamonitor.core.base;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceViewHolder;

import com.drnoob.datamonitor.R;

public class SwitchPreferenceCompat extends androidx.preference.SwitchPreferenceCompat {
    private static final String TAG = SwitchPreferenceCompat.class.getSimpleName();

    public SwitchPreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public SwitchPreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SwitchPreferenceCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SwitchPreferenceCompat(Context context) {
        super(context);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        if (holder != null) {
            TextView title = (TextView) holder.findViewById(android.R.id.title);
            LinearLayout rootLayout = (LinearLayout) title.getParent().getParent();
            rootLayout.setBackgroundResource(R.drawable.selectable_item_plain_background);
            TextView summary = (TextView) holder.findViewById(android.R.id.summary);
            summary.setTextColor(getContext().getResources().getColor(R.color.text_secondary, null));

            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) rootLayout.getLayoutParams();
            layoutParams.leftMargin = 25;
            layoutParams.rightMargin = 25;
            layoutParams.bottomMargin = 25;

            rootLayout.setLayoutParams(layoutParams);

            rootLayout.setPadding(75, 10, 75, 10);

            SwitchCompat darkModeToggle = (SwitchCompat) holder.findViewById(R.id.switchWidget);

            Boolean isDarkModeEnabled = PreferenceManager.getDefaultSharedPreferences(getContext())
                    .getBoolean("dark_mode_toggle", false);


        }
    }
}
