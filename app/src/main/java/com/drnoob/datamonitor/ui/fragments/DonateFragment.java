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

package com.drnoob.datamonitor.ui.fragments;

import static com.drnoob.datamonitor.Common.dismissOnClick;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.core.base.Preference;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;

public class DonateFragment extends Fragment {
    private static final String TAG = DonateFragment.class.getSimpleName();
    private static ActivityResultLauncher<Intent> bhimResultLauncher;

    public DonateFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bhimResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            Log.d(TAG, "onActivityResult: " + data.getStringExtra("response"));
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_donate, container, false);


        return view;
    }

    public static class Donate extends PreferenceFragmentCompat {
        Preference mBitcoin, mEthereum, mLitecoin, mMonero, mBhim;
        Snackbar snackbar;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.donate, rootKey);

            mBitcoin = (Preference) findPreference("bitcoin");
            mEthereum = (Preference) findPreference("ethereum");
            mLitecoin = (Preference) findPreference("litecoin");
            mMonero = (Preference) findPreference("monero");
            mBhim = (Preference) findPreference("bhim");

            mBitcoin.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    copyAddress("btc address", getString(R.string.btc_address));
                    snackbar = Snackbar.make(getActivity().findViewById(R.id.container_root),
                            R.string.btc_address_copied, Snackbar.LENGTH_SHORT);
                    dismissOnClick(snackbar);
                    snackbar.show();
                    return false;
                }
            });

            mEthereum.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    copyAddress("eth address", getString(R.string.eth_address));
                    snackbar = Snackbar.make(getActivity().findViewById(R.id.container_root),
                            R.string.eth_address_copied, Snackbar.LENGTH_SHORT);
                    dismissOnClick(snackbar);
                    snackbar.show();
                    return false;
                }
            });

            mLitecoin.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    copyAddress("ltc address", getString(R.string.ltc_address));
                    snackbar = Snackbar.make(getActivity().findViewById(R.id.container_root),
                            R.string.ltc_address_copied, Snackbar.LENGTH_SHORT);
                    dismissOnClick(snackbar);
                    snackbar.show();
                    return false;
                }
            });

            mMonero.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    copyAddress("xmr address", getString(R.string.xmr_address));
                    snackbar = Snackbar.make(getActivity().findViewById(R.id.container_root),
                            R.string.xmr_address_copied, Snackbar.LENGTH_SHORT);
                    dismissOnClick(snackbar);
                    snackbar.show();
                    return false;
                }
            });

            mBhim.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    Uri uri = Uri.parse(getContext().getString(R.string.upi_address));
                    Log.d(TAG, "onClick: uri: " + uri);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    try {
                        bhimResultLauncher.launch(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (e instanceof ActivityNotFoundException) {
                            Snackbar.make(getActivity().findViewById(R.id.container_host_fragment),
                                    getString(R.string.upi_app_not_found), Snackbar.LENGTH_SHORT).show();
                        }
                        else {
                            Snackbar.make(getActivity().findViewById(R.id.container_host_fragment),
                                    getString(R.string.upi_unknown_error), Snackbar.LENGTH_SHORT).show();
                        }
                    }
                    return false;
                }
            });
        }

        @Override
        public void onPause() {
            super.onPause();
            if (snackbar != null) {
                snackbar.dismiss();
            }
        }

        private void copyAddress(String label, String address) {
            ClipboardManager manager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData data = ClipData.newPlainText(label, address);
            manager.setPrimaryClip(data);
        }
    }
}
