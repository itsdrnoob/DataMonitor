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

package com.drnoob.datamonitor.ui.fragments

import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.drnoob.datamonitor.R
import com.drnoob.datamonitor.core.Values.APP_CONTRIBUTORS_FRAGMENT
import com.drnoob.datamonitor.core.Values.GENERAL_FRAGMENT_ID
import com.drnoob.datamonitor.core.base.Preference
import com.drnoob.datamonitor.ui.activities.ContainerActivity
import com.drnoob.datamonitor.ui.activities.WallOfThanksActivity

class ContributorsFragment: PreferenceFragmentCompat() {
    private lateinit var appContributors: Preference
    private lateinit var wallOfThanks: Preference
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.contributors_preference, rootKey)

        appContributors = findPreference("app_contributors")!!
        wallOfThanks = findPreference("wall_of_thanks")!!

        appContributors.setOnPreferenceClickListener {
            context?.startActivity(Intent(context, ContainerActivity().javaClass)
                .putExtra(GENERAL_FRAGMENT_ID, APP_CONTRIBUTORS_FRAGMENT))
            false
        }

        wallOfThanks.setOnPreferenceClickListener {
            context?.startActivity(Intent(context, WallOfThanksActivity().javaClass))
            false
        }

    }
}