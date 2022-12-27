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
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import org.jetbrains.annotations.NotNull;

public class PreferenceHeader extends PreferenceCategory {
    public PreferenceHeader(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public PreferenceHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PreferenceHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PreferenceHeader(Context context) {
        super(context);
    }

    @Override
    protected boolean onPrepareAddPreference(@NonNull @NotNull Preference preference) {
        this.setIconSpaceReserved(false);
        return super.onPrepareAddPreference(preference);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        if (holder != null) {
            TextView title = (TextView) holder.findViewById(android.R.id.title);
//            title.setTextColor(getContext().getResources().getColor(R.color.text_primary, null));
//            title.setTextSize(18);
            RelativeLayout rootLayout = (RelativeLayout) title.getParent();
            rootLayout.setPadding(30, 0, 0, 0);
//            this.setIconSpaceReserved(false);
        }
    }
}
