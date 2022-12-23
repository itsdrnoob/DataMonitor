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
import static com.drnoob.datamonitor.core.Values.EXCLUDE_APPS_LIST;
import static com.drnoob.datamonitor.core.Values.EXTRA_APP_NAME;
import static com.drnoob.datamonitor.core.Values.EXTRA_APP_PACKAGE;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.adapters.AppsListAdapter;
import com.drnoob.datamonitor.adapters.data.AppModel;
import com.drnoob.datamonitor.adapters.data.LiveData;
import com.drnoob.datamonitor.databinding.FragmentExcludeAppsBinding;
import com.drnoob.datamonitor.ui.activities.AppPickerActivity;
import com.drnoob.datamonitor.utils.SharedPreferences;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ExcludeAppsFragment extends Fragment {
    private static final String TAG = ExcludeAppsFragment.class.getSimpleName();

    FragmentExcludeAppsBinding binding;
    ActivityResultLauncher<Intent> appSelecterLauncher;
    List<AppModel> excludedAppsList;
    List<AppModel> toRemoveAppsList;

    private AppsListAdapter mAdapter;
    private LiveData mLiveData;
    private Type type = new TypeToken<List<AppModel>>() {}.getType();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        excludedAppsList = new ArrayList<>();
        toRemoveAppsList = new ArrayList<>();
        mLiveData = new ViewModelProvider(requireActivity()).get(LiveData.class);

        appSelecterLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getData() != null) {
                            try {
                                String appName = result.getData().getStringExtra(EXTRA_APP_NAME);
                                String appPackage = result.getData().getStringExtra(EXTRA_APP_PACKAGE);

                                addApp(appName, appPackage);

                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });

        if (mLiveData.getSelectedAppsList().getValue() != null &&
                mLiveData.getSelectedAppsList().getValue().size() > 0) {
            mLiveData.setSelectedAppsList(new ArrayList<>());
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentExcludeAppsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {

            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.menu_delete) {
                    requireContext();
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.title_remove_apps)
                            .setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    if (mLiveData != null && mLiveData.getSelectedAppsList().getValue() != null) {
                                        excludedAppsList.removeAll(mLiveData.getSelectedAppsList().getValue());
                                        saveAppsList();
                                        mAdapter.notifyDataSetChanged();

                                        if (excludedAppsList.isEmpty()) {
                                            binding.emptyListBanner.setVisibility(View.VISIBLE);
                                        }

                                        for (AppModel app : mLiveData.getSelectedAppsList().getValue()) {
                                            requireContext();
                                            SharedPreferences.getExcludeAppsPrefs(requireContext())
                                                    .edit()
                                                    .remove(app.getPackageName())
                                                    .apply();
                                        }
                                    }
                                    dialogInterface.dismiss();
                                    getActivity().onBackPressed();
                                }
                            })
                            .setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.cancel();
                                }
                            })
                            .show();
                }
                return false;
            }
        }, requireActivity(), Lifecycle.State.RESUMED);

        binding.addAppFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                appSelecterLauncher.launch(new Intent(getActivity(), AppPickerActivity.class));
            }
        });

        binding.appsList.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.appsList.setHasFixedSize(false);

        refreshAppsList();

        mLiveData.getIsAppSelectionView().observe(requireActivity(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean) {
                    binding.addAppFab.animate()
                            .translationYBy(500)
                            .start();
                }
                else {
                    binding.addAppFab.animate()
                            .translationY(0)
                            .start();
                }
            }
        });

        /*
        Shrink or expand the FAB according to user scroll
         */
        binding.appsList.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (oldScrollY < -15 && binding.addAppFab.isExtended()) {
                    binding.addAppFab.shrink();
                }
                else if (oldScrollY > 15 && !binding.addAppFab.isExtended()) {
                    binding.addAppFab.extend();
                }
                else if (binding.appsList.computeVerticalScrollOffset() == 0 && !binding.addAppFab.isExtended()) {
                    binding.addAppFab.extend();
                }
            }
        });

    }

    private void addApp(String appName, String appPackage) {
        if (excludedAppsList.isEmpty()) {
            binding.emptyListBanner.setVisibility(View.GONE);
        }

        AppModel app = new AppModel(appName, appPackage);

        if (SharedPreferences.getExcludeAppsPrefs(getContext()).getString(appPackage, null) == null) {
            excludedAppsList.add(app);
            SharedPreferences.getExcludeAppsPrefs(getContext()).edit()
                    .putString(appPackage, appName)
                    .apply();
        }
        else {
            Snackbar snackbar;
            snackbar = Snackbar.make(binding.getRoot(), requireContext().getString(R.string.app_already_present),
                            Snackbar.LENGTH_SHORT).setAnchorView(binding.addAppFab);
            dismissOnClick(snackbar);
            snackbar.show();
        }

        Collections.sort(excludedAppsList, new Comparator<AppModel>() {
            @Override
            public int compare(AppModel appModel, AppModel t1) {
                return appModel.getAppName().compareTo(t1.getAppName());
            }
        });

        if (mAdapter == null) {
            mAdapter = new AppsListAdapter(requireActivity(), excludedAppsList);
            binding.appsList.setAdapter(mAdapter);
        }
        else {
            mAdapter.notifyDataSetChanged();
        }
        binding.appsList.setVisibility(View.VISIBLE);
        saveAppsList();
    }

    private void saveAppsList() {
        if (excludedAppsList == null) {
            excludedAppsList = new ArrayList<>();
        }
        Gson gson = new Gson();
        String jsonData = gson.toJson(excludedAppsList, type);
        SharedPreferences.getExcludeAppsPrefs(getContext()).edit()
                .putString(EXCLUDE_APPS_LIST, jsonData)
                .apply();
    }

    private void refreshAppsList() {
        binding.loadAppsProgress.setVisibility(View.VISIBLE);
        binding.appsList.setVisibility(View.GONE);

        Gson gson = new Gson();
        String jsonData = SharedPreferences.getExcludeAppsPrefs(getContext())
                .getString(EXCLUDE_APPS_LIST, null);
        if (jsonData != null) {
            if (excludedAppsList == null) {
                excludedAppsList = new ArrayList<>();
            }
            excludedAppsList.clear();
            excludedAppsList.addAll(gson.fromJson(jsonData, type));

            mAdapter = new AppsListAdapter(getActivity(), excludedAppsList);

            binding.appsList.setAdapter(mAdapter);
            binding.loadAppsProgress.setVisibility(View.GONE);
            binding.appsList.setVisibility(View.VISIBLE);
            if (excludedAppsList.isEmpty()) {
                binding.emptyListBanner.setVisibility(View.VISIBLE);
            }
        }
        else {
            binding.loadAppsProgress.setVisibility(View.GONE);
            binding.emptyListBanner.setVisibility(View.VISIBLE);
        }
    }
}
