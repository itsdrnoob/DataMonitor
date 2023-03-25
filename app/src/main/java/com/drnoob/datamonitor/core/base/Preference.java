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
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;

public class Preference extends androidx.preference.Preference {

    private static final String TAG = Preference.class.getSimpleName();

    public Preference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public Preference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public Preference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Preference(Context context) {
        super(context);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        if (holder != null) {
            TextView title = (TextView) holder.findViewById(android.R.id.title);
            LinearLayout rootLayout = (LinearLayout) title.getParent().getParent();
            TextView summary = (TextView) holder.findViewById(android.R.id.summary);
            summary.setMaxLines(2);
            summary.setEllipsize(TextUtils.TruncateAt.END);
            rootLayout.setPadding(75, 10, 75, 10);

            title.setSingleLine(false);
        }
    }

}
