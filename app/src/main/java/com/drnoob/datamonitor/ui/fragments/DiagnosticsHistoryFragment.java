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

import static com.drnoob.datamonitor.core.Values.DIAGNOSTICS_HISTORY_LIST;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.adapters.DiagnosticsHistoryAdapter;
import com.drnoob.datamonitor.adapters.data.DiagnosticsHistoryModel;
import com.drnoob.datamonitor.adapters.data.LiveData;
import com.drnoob.datamonitor.databinding.FragmentDiagnosticsHistoryBinding;
import com.drnoob.datamonitor.utils.SharedPreferences;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DiagnosticsHistoryFragment extends Fragment {
    private static final String TAG = DiagnosticsHistoryFragment.class.getSimpleName();

    FragmentDiagnosticsHistoryBinding binding;

    private DiagnosticsHistoryAdapter mAdapter;
    private List<DiagnosticsHistoryModel> mList;
    private LiveData mLiveData;
    private Gson gson = new Gson();
    private Type type = new TypeToken<List<DiagnosticsHistoryModel>>() {}.getType();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mList = new ArrayList<>();
        mAdapter = new DiagnosticsHistoryAdapter(mList, requireActivity());
        mLiveData = new ViewModelProvider(requireActivity()).get(LiveData.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDiagnosticsHistoryBinding.inflate(inflater, container, false);
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
                            .setTitle(R.string.title_delete_results)
                            .setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    mList.removeAll(mLiveData.getSelectedResults().getValue());
                                    mAdapter.notifyDataSetChanged();
                                    String jsonData = gson.toJson(mList, type);
                                    SharedPreferences.getDiagnosticsHistoryPrefs(requireContext()).edit()
                                                    .putString(DIAGNOSTICS_HISTORY_LIST, jsonData)
                                                    .apply();

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
                if (menuItem.getItemId() == R.id.menu_select_all) {
                    if (mLiveData.getSelectedResults().getValue() != null) {
                        if (mLiveData.getSelectedResults().getValue().size() < mList.size()) {
                            // Select all
                            mAdapter.selectAll();
                        }
                        else {
                            // Deselect all
                            mAdapter.deselectAll();
                        }
                    }

                }
                return false;
            }
        }, requireActivity(), Lifecycle.State.RESUMED);

        binding.historyView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.historyView.setAdapter(mAdapter);

        refreshResults();

    }

    private void refreshResults() {
        binding.historyView.setVisibility(View.GONE);
        String jsonData = SharedPreferences.getDiagnosticsHistoryPrefs(requireContext())
                .getString(DIAGNOSTICS_HISTORY_LIST, null);
        if (jsonData != null) {
            mList.addAll(gson.fromJson(jsonData, type));
        }
        Collections.sort(mList, new Comparator<DiagnosticsHistoryModel>() {
            @Override
            public int compare(DiagnosticsHistoryModel diagnosticsHistoryModel, DiagnosticsHistoryModel t1) {
                return t1.getId().compareTo(diagnosticsHistoryModel.getId());
            }
        });
        mAdapter.notifyDataSetChanged();
        binding.historyView.setVisibility(View.VISIBLE);
    }
}
