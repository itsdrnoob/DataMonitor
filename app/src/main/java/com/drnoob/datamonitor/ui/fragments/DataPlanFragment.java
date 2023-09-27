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

import static com.drnoob.datamonitor.Common.UTCToLocal;
import static com.drnoob.datamonitor.Common.cancelDataPlanNotification;
import static com.drnoob.datamonitor.Common.dismissOnClick;
import static com.drnoob.datamonitor.Common.localToUTC;
import static com.drnoob.datamonitor.Common.setBoldSpan;
import static com.drnoob.datamonitor.core.Values.DATA_LIMIT;
import static com.drnoob.datamonitor.core.Values.DATA_RESET;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_END;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_END_HOUR;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_END_MIN;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_RESTART;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_START;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_START_HOUR;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_START_MIN;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_DAILY;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_MONTHLY;
import static com.drnoob.datamonitor.core.Values.DATA_TYPE;
import static com.drnoob.datamonitor.core.Values.LIMIT;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.Widget.DataUsageWidget;
import com.drnoob.datamonitor.databinding.FragmentDataPlanBinding;
import com.drnoob.datamonitor.utils.DataUsageMonitor;
import com.drnoob.datamonitor.utils.NotificationService;
import com.drnoob.datamonitor.utils.VibrationUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.elevation.SurfaceColors;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public class DataPlanFragment extends Fragment {
    public static final String TAG = DataPlanFragment.class.getSimpleName();

    private static final int TYPE_PLAN_START = 0;
    private static final int TYPE_PLAN_END = 1;

    FragmentDataPlanBinding binding;

    private Long planStartDateMillis, planEndDateMillis;
    private int startHour, startMinute, endHour, endMinute;
    private long startMillis, endMillis; // Absolute start and end time in millis
    private boolean is12HourView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDataPlanBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @SuppressLint("SimpleDateFormat")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(binding.containerToolbar);
        Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar())
                .setTitle(requireContext().getString(R.string.title_add_data_plan));
        Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar())
                .setDisplayHomeAsUpEnabled(true);
        Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar())
                .setDisplayShowHomeEnabled(true);
        binding.containerToolbar.setBackgroundColor(SurfaceColors.SURFACE_2.getColor(getContext()));
        binding.toolbarSave.setVisibility(View.VISIBLE);

        Calendar calendar = Calendar.getInstance();
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        Date date = new Date();
        String time = DateFormat.getTimeInstance(DateFormat.SHORT).format(date.getTime()).toLowerCase(Locale.ROOT);
        is12HourView = time.contains("am") || time.contains("pm") ||
                time.contains("a.m") || time.contains("p.m");

        try {
            planStartDateMillis = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getLong(DATA_RESET_CUSTOM_DATE_START, UTCToLocal(MaterialDatePicker.todayInUtcMilliseconds()));
            planEndDateMillis = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getLong(DATA_RESET_CUSTOM_DATE_END, UTCToLocal(MaterialDatePicker.todayInUtcMilliseconds()));
        }
        catch (ClassCastException e) {
            int planStartIntValue = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getInt(DATA_RESET_CUSTOM_DATE_START, -1);
            int planEndIntValue = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getInt(DATA_RESET_CUSTOM_DATE_END, -1);
            planStartDateMillis = ((Number) planStartIntValue).longValue();
            planEndDateMillis = ((Number) planEndIntValue).longValue();
        }

        startHour = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getInt(DATA_RESET_CUSTOM_DATE_START_HOUR, -1);
        startMinute = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getInt(DATA_RESET_CUSTOM_DATE_START_MIN, -1);
        endHour = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getInt(DATA_RESET_CUSTOM_DATE_END_HOUR, -1);
        endMinute = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getInt(DATA_RESET_CUSTOM_DATE_END_MIN, -1);

        if (startHour < 0 || startMinute < 0 || endHour < 0 || endMinute < 0) {
            startHour = 0;
            startMinute = 0;
            endHour = 23;
            endMinute = 59;
        }

        String planStart = new SimpleDateFormat("dd/MM/yyyy").format(planStartDateMillis);
        String planEnd = new SimpleDateFormat("dd/MM/yyyy").format(planEndDateMillis);
        String startTime, endTime;
        startTime = getContext().getString(R.string.label_custom_start_time, getTime(startHour, startMinute, is12HourView));
        endTime = getContext().getString(R.string.label_custom_end_time, getTime(endHour, endMinute, is12HourView));
        String startDateToday = getContext().getString(R.string.label_custom_start_date, planStart);
        String endDateToday = getContext().getString(R.string.label_custom_end_date, planEnd);


        binding.customStartDate.setText(setBoldSpan(startDateToday, planStart));
        binding.customEndDate.setText(setBoldSpan(endDateToday, planEnd));
        binding.customStartTime.setText(setBoldSpan(startTime, getTime(startHour, startMinute, is12HourView)));
        binding.customEndTime.setText(setBoldSpan(endTime, getTime(endHour, endMinute, is12HourView)));

        binding.customStartDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calendar.setTimeInMillis(new Date().getTime());
                calendar.add(Calendar.YEAR, -2);
                long startYear = calendar.getTimeInMillis();
                calendar.add(Calendar.YEAR, 2);
                long endYear = calendar.getTimeInMillis();

                CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder()
                        .setStart(startYear)
                        .setEnd(endYear)
                        .setValidator(DateValidatorPointBackward.now());

                MaterialDatePicker<Long> startDatePicker =
                        MaterialDatePicker.Builder.datePicker()
                                .setSelection(localToUTC(planStartDateMillis))
                                .setTitleText(getContext().getString(R.string.label_select_start_date))
                                .setCalendarConstraints(constraintsBuilder.build())
                                .build();


                startDatePicker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener() {
                    @Override
                    public void onPositiveButtonClick(Object selection) {
                        planStartDateMillis = Long.parseLong(selection.toString());
                        Log.d(TAG, "onPositiveButtonClick: UTC: " + planStartDateMillis );
                        Log.d(TAG, "onPositiveButtonClick: Local: " + UTCToLocal(planStartDateMillis));
                        planStartDateMillis = UTCToLocal(planStartDateMillis);
                        Log.d(TAG, "onPositiveButtonClick: UTC: " + localToUTC(planStartDateMillis));
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                        dateFormat.setTimeZone(TimeZone.getDefault());
                        String date = dateFormat.format(new Date(planStartDateMillis));

                        String startDateString = getContext().getString(R.string.label_custom_start_date, date);
                        binding.customStartDate.setText(setBoldSpan(startDateString, date));
                    }
                });

                startDatePicker.show(getChildFragmentManager(), startDatePicker.toString());
            }
        });

        binding.customEndDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calendar.setTimeInMillis(new Date().getTime());
//                            calendar.add(Calendar.YEAR, -2);
                long startYear = calendar.getTimeInMillis();
                calendar.add(Calendar.YEAR, 2);
                long endYear = calendar.getTimeInMillis();

                CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder()
                        .setStart(startYear)
                        .setEnd(endYear)
                        .setValidator(DateValidatorPointForward.now());

                MaterialDatePicker<Long> endDatePicker =
                        MaterialDatePicker.Builder.datePicker()
                                .setSelection(localToUTC(planEndDateMillis))
                                .setTitleText(getContext().getString(R.string.label_plan_end_date))
                                .setCalendarConstraints(constraintsBuilder.build())
                                .build();

                endDatePicker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener() {
                    @Override
                    public void onPositiveButtonClick(Object selection) {
                        planEndDateMillis = Long.parseLong(selection.toString());
                        planEndDateMillis = UTCToLocal(planEndDateMillis);
                        String date = new SimpleDateFormat("dd/MM/yyyy").format(planEndDateMillis);
                        String endDateString = getContext().getString(R.string.label_custom_end_date, date);
                        binding.customEndDate.setText(setBoldSpan(endDateString, date));
                    }
                });

                endDatePicker.show(getChildFragmentManager(), endDatePicker.toString());
            }
        });

        binding.customStartTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker(TYPE_PLAN_START);
            }
        });

        binding.customEndTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker(TYPE_PLAN_END);
            }
        });

        binding.dataTypeSwitcher.selectTab(binding.dataTypeSwitcher.getTabAt(PreferenceManager
                .getDefaultSharedPreferences(getContext())
                .getInt(DATA_TYPE, 0)));

        binding.dataTypeSwitcher.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (!PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .getBoolean("disable_haptics", false)) {
                    VibrationUtils.hapticMinor(getContext());
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        Float dataLimit = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getFloat(DATA_LIMIT, -1);
        if (dataLimit > 0) {
            if (dataLimit >= 1024) {
                String data = String.format("%.2f", dataLimit / 1024) + "";
                binding.dataLimit.setText(data);
            } else {
                binding.dataLimit.setText(PreferenceManager.getDefaultSharedPreferences(getContext())
                        .getString(LIMIT, null));
            }
        }

        switch (PreferenceManager.getDefaultSharedPreferences(getContext())
                .getString(DATA_RESET, "")) {
            case DATA_RESET_MONTHLY:
                binding.dataReset.check(R.id.monthly);
                binding.customDateView.setVisibility(View.GONE);
                break;
            case DATA_RESET_DAILY:
                binding.dataReset.check(R.id.daily);
                binding.customDateView.setVisibility(View.GONE);
                break;
            case DATA_RESET_CUSTOM:
                binding.dataReset.check(R.id.custom_reset);
                binding.customDateView.setAlpha(1f);
                binding.customDateView.setVisibility(View.VISIBLE);
                break;
        }

        binding.dataReset.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if (i == R.id.custom_reset) {
                    binding.customDateView.setAlpha(0f);
                    binding.customDateView.setVisibility(View.VISIBLE);
                    binding.customDateView.animate()
                            .alpha(1f)
                            .setDuration(350)
                            .start();
                }
                else {
                    binding.customDateView.animate()
                            .alpha(0f)
                            .setDuration(350)
                            .start();
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            binding.customDateView.setVisibility(View.GONE);
                        }
                    }, 150);

                }
            }
        });

        binding.toolbarSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String previousPlanType = PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .getString(DATA_RESET, "null");
                if (binding.dataLimit.getText().toString().length() <= 0) {
                    binding.dataLimitView.setError(getString(R.string.error_invalid_plan));
                }
                else {
                    String startDate = new SimpleDateFormat("yyyy/MM/dd").format(planStartDateMillis);
                    String endDate = new SimpleDateFormat("yyyy/MM/dd").format(planEndDateMillis);
                    String start = startDate + " " + startHour + ":" + startMinute + ":00";
                    String end = endDate + " " + endHour + ":" + endMinute + ":00";

                    Date date;
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    try {
                        date = dateFormat.parse(start);
                        startMillis = date.getTime();
                        date = dateFormat.parse(end);
                        endMillis = date.getTime();
                    }
                    catch (ParseException e) {
                        e.printStackTrace();
                    }

                    if ((binding.dataReset.getCheckedRadioButtonId() == R.id.custom_reset &&
                            startMillis > System.currentTimeMillis()) ||
                            (binding.dataReset.getCheckedRadioButtonId() == R.id.custom_reset &&
                            endMillis < System.currentTimeMillis())) {
                        Snackbar snackbar = Snackbar.make(binding.getRoot(),
                                requireContext().getString(R.string.error_invalid_plan_duration),
                                Snackbar.LENGTH_SHORT);
                        dismissOnClick(snackbar);
                        snackbar.show();
                    }
                    else {
                        String dataLimitText = binding.dataLimit.getText().toString();
                        if (dataLimitText.contains(",")) {
                            dataLimitText = dataLimitText.replace(",", ".");
                        }
                        if (dataLimitText.contains("٫")) {
                            dataLimitText = dataLimitText.replace("٫", ".");
                        }
                        Float dataLimit = Float.parseFloat(dataLimitText);
                        int dataType;
                        if (binding.dataTypeSwitcher.getTabAt(0).isSelected()) {
                            if (dataLimit >= 1024) {
                                dataType = 1;
                            } else {
                                dataLimit = dataLimit;
                                dataType = binding.dataTypeSwitcher.getSelectedTabPosition();
                            }
                        }
                        else {
                            dataLimit = dataLimit * 1024f;
                            dataType = binding.dataTypeSwitcher.getSelectedTabPosition();
                        }
                        if (binding.dataReset.getCheckedRadioButtonId() == R.id.daily) {
                            PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                                    .putString(DATA_RESET, DATA_RESET_DAILY).apply();
                        }
                        else if (binding.dataReset.getCheckedRadioButtonId() == R.id.monthly) {
                            PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                                    .putString(DATA_RESET, DATA_RESET_MONTHLY).apply();
                        }
                        else if (binding.dataReset.getCheckedRadioButtonId() == R.id.custom_reset) {
                            calendar.setTimeInMillis(planEndDateMillis);
                            calendar.add(Calendar.DATE, 1);

                            PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                                    .putString(DATA_RESET, DATA_RESET_CUSTOM)
                                    .putLong(DATA_RESET_CUSTOM_DATE_START, planStartDateMillis)
                                    .putLong(DATA_RESET_CUSTOM_DATE_END, planEndDateMillis)
                                    .putLong(DATA_RESET_CUSTOM_DATE_RESTART, calendar.getTimeInMillis())
                                    .apply();
                        }

                        PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                                .putFloat(DATA_LIMIT, dataLimit)
                                .putString(LIMIT, binding.dataLimit.getText().toString())
                                .putInt(DATA_TYPE, dataType)
                                .putInt(DATA_RESET_CUSTOM_DATE_START_HOUR, startHour)
                                .putInt(DATA_RESET_CUSTOM_DATE_START_MIN, startMinute)
                                .putInt(DATA_RESET_CUSTOM_DATE_END_HOUR, endHour)
                                .putInt(DATA_RESET_CUSTOM_DATE_END_MIN, endMinute)
                                .apply();

                        if (previousPlanType.equals(DATA_RESET_CUSTOM)) {
                            Log.d(TAG, "onClick: Previously set custom plan found, cancelling refresh alarm" );
                            cancelDataPlanNotification(requireContext());
                        }
                        if (PreferenceManager.getDefaultSharedPreferences(requireContext())
                                .getBoolean("data_usage_alert", false)) {
                            DataUsageMonitor.updateServiceRestart(requireContext());
                        }

                        Intent data = new Intent();
                        requireActivity().setResult(Activity.RESULT_OK, data);
                        requireActivity().finish();
                    }
                }
            }
        });

        binding.dataLimit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (binding.dataLimit.getText().toString().length() <= 0) {

                }
                else {
                    binding.dataLimitView.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void showTimePicker(int type) {
        BottomSheetDialog dialog = new BottomSheetDialog(getContext(), R.style.BottomSheet);
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.layout_time_picker, null);

        TimePicker timePicker = (TimePicker) dialogView.findViewById(R.id.reset_time_picker);
        ConstraintLayout footer = dialogView.findViewById(R.id.footer);
        TextView cancel = footer.findViewById(R.id.cancel);
        TextView ok = footer.findViewById(R.id.ok);

        (((LinearLayout) ((LinearLayout) timePicker.getChildAt(0)).getChildAt(0)).getChildAt(0)).setVerticalScrollBarEnabled(false);
        (((LinearLayout) ((LinearLayout) timePicker.getChildAt(0)).getChildAt(0)).getChildAt(2)).setVerticalScrollBarEnabled(false);

        timePicker.setIs24HourView(!is12HourView);

        if (type == TYPE_PLAN_START) {
            timePicker.setHour(startHour);
            timePicker.setMinute(startMinute);
        }
        else {
            timePicker.setHour(endHour);
            timePicker.setMinute(endMinute);
        }

        timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker timePicker, int i, int i1) {
                if (!PreferenceManager.getDefaultSharedPreferences(getContext())
                        .getBoolean("disable_haptics", false)) {
                    VibrationUtils.hapticMinor(getContext());
                }
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getContext(), NotificationService.NotificationUpdater.class);
                getContext().sendBroadcast(i);
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getContext());
                int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(getContext(), DataUsageWidget.class));
                Intent intent = new Intent(getContext(), DataUsageWidget.class);
                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                getContext().sendBroadcast(intent);

                String time, timeText;

                time = getTime(timePicker.getHour(), timePicker.getMinute(), is12HourView);

                if (type == TYPE_PLAN_START) {
                    timeText = getContext().getString(R.string.label_custom_start_time, time);
                    binding.customStartTime.setText(setBoldSpan(timeText, time));

                    startHour = timePicker.getHour();
                    startMinute = timePicker.getMinute();
                }
                else {
                    timeText = getContext().getString(R.string.label_custom_end_time, time);
                    binding.customEndTime.setText(setBoldSpan(timeText, time));

                    endHour = timePicker.getHour();
                    endMinute = timePicker.getMinute();
                }

                dialog.dismiss();
            }
        });

        dialog.setContentView(dialogView);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
                FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        dialog.show();
    }

    public static String getTime(int hour, int minute, boolean is12HourView) {
        String time;
        int hourOfDay;
        if (!is12HourView) {
            String formattedHour, formattedMinute;
            if (hour < 10) {
                formattedHour = "0" + hour;
            }
            else {
                formattedHour = "" + hour;
            }
            if (minute < 10) {
                formattedMinute = "0" + minute;
            }
            else {
                formattedMinute = "" + minute;
            }
            time = formattedHour + ":" + formattedMinute;
        }
        else {
            if (hour >= 12) {
                if (hour == 12) {
                    hourOfDay = 12;
                }
                else {
                    hourOfDay = (hour - 12);
                }
                if (minute < 10) {
                    time = hourOfDay + ":0" + minute + " pm";
                }
                else {
                    time = hourOfDay + ":" + minute + " pm";
                }
            }
            else {
                if (hour == 0) {
                    hourOfDay = 12;
                }
                else if (hour < 10) {
                    hourOfDay = hour;
                    if (minute < 10) {
                        time = "0" + hourOfDay + ":0" + minute + " pm";
                    }
                    else {
                        time = "0" + hourOfDay + ":" + minute + " pm";
                    }
                }
                else {
                    hourOfDay = hour;
                }
                if (hourOfDay < 10) {
                    time = "0" + hourOfDay + ":" + minute + " am";
                }
                else {
                    time = hourOfDay + ":" + minute + " am";
                }
                if (minute < 10) {
                    time = hourOfDay + ":0" + minute + " am";
                }
                else {
                    time = hourOfDay + ":" + minute + " am";
                }
            }
        }
        return time;
    }
}
