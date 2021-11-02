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

package com.drnoob.datamonitor.ui.activities;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.databinding.ActivityContainerBinding;
import com.drnoob.datamonitor.ui.fragments.AboutFragment;
import com.drnoob.datamonitor.ui.fragments.ContributorsFragment;
import com.drnoob.datamonitor.ui.fragments.DonateFragment;
import com.drnoob.datamonitor.ui.fragments.LicenseFragment;

import org.jetbrains.annotations.NotNull;

import static com.drnoob.datamonitor.core.Values.ABOUT_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.APP_LICENSE;
import static com.drnoob.datamonitor.core.Values.CONTRIBUTORS_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.DONATE_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.GENERAL_FRAGMENT_ID;
import static com.drnoob.datamonitor.core.Values.LICENSE_FRAGMENT;

public class ContainerActivity extends AppCompatActivity {

    ActivityContainerBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MainActivity.setTheme(ContainerActivity.this);
        super.onCreate(savedInstanceState);
        binding = ActivityContainerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.containerToolbar);
        getSupportActionBar().setTitle(null);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(getDrawable(R.drawable.ic_arrow));

        int fragmentId = getIntent().getIntExtra(GENERAL_FRAGMENT_ID, 0);
        Fragment fragment = null;
        String title = null;
        switch (fragmentId) {
            case ABOUT_FRAGMENT:
                fragment = new AboutFragment();
                title = getString(R.string.about);
                break;

            case LICENSE_FRAGMENT:
                fragment = new LicenseFragment();
                title = getString(R.string.license);
                break;

            case CONTRIBUTORS_FRAGMENT:
                fragment = new ContributorsFragment();
                title = getString(R.string.contributors);
                break;

            case DONATE_FRAGMENT:
                fragment = new DonateFragment();
                title = getString(R.string.donate);
                break;


            case APP_LICENSE:
                fragment = new LicenseFragment();
                title = getString(R.string.app_license_header);
                break;
        }

        getSupportFragmentManager().beginTransaction().replace(R.id.container_host_fragment, fragment).commit();
        getSupportActionBar().setTitle(title);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull @NotNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}