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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.TimePicker
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.drnoob.datamonitor.Common
import com.drnoob.datamonitor.R
import com.drnoob.datamonitor.databinding.FragmentCustomSessionBinding
import com.drnoob.datamonitor.utils.VibrationUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.snackbar.Snackbar
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class CustomSessionFragment: Fragment() {
    companion object {
        private val TAG = CustomSessionFragment::class.simpleName

        private const val TYPE_START = 1
        private const val TYPE_END = 2
    }

    private lateinit var binding: FragmentCustomSessionBinding

    private var filterDate: String? = AppDataUsageFragment.customFilterDate.value ?: null

    private var startDateMillis = AppDataUsageFragment.customFilterDateMillis.value?.first ?: 0L
    private var endDateMillis = AppDataUsageFragment.customFilterDateMillis.value?.second ?: 0L

    private var startHour = AppDataUsageFragment.customFilterTime.value?.get("startHour") ?: 0
    private var startMinute = AppDataUsageFragment.customFilterTime.value?.get("startMinute") ?: 0
    private var endHour = AppDataUsageFragment.customFilterTime.value?.get("endHour") ?: 23
    private var endMinute = AppDataUsageFragment.customFilterTime.value?.get("endMinute") ?: 59

    private var is12HourView = false

    private var isTimeSet = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCustomSessionBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SimpleDateFormat")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val baseActivity = (activity as AppCompatActivity)
        baseActivity.setSupportActionBar(binding.containerToolbar)
        baseActivity.supportActionBar?.title = context?.getString(R.string.add_custom_session)
        baseActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.containerToolbar.setBackgroundColor(SurfaceColors.SURFACE_2.getColor(requireContext()))

        val date = Date()
        val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(date.time).lowercase()
        is12HourView = time.contains("am") || time.contains("pm") ||
                time.contains("a.m") || time.contains("p.m")

        updateTime()

        if (AppDataUsageFragment.shouldShowTime) {
            binding.addTime.visibility = View.GONE
            binding.timeSelectionView.visibility = View.VISIBLE
        }

        if (!filterDate.isNullOrEmpty()) {
            binding.selectedDate.text = filterDate
            binding.selectedDate.visibility = View.VISIBLE
        }


        binding.toolbarReset.setOnClickListener {
            isTimeSet = false
            AppDataUsageFragment.shouldShowTime = false
            filterDate = null

            startHour = 0
            startMinute = 0
            endHour = 23
            endMinute = 59

            binding.selectedDate.visibility = View.GONE
            binding.timeSelectionView.visibility = View.GONE
            binding.addTime.visibility = View.VISIBLE

            startDateMillis = 0
            endDateMillis = 0

            AppDataUsageFragment.customFilter.postValue(null)
            AppDataUsageFragment.customFilterDateMillis.postValue(null)
            AppDataUsageFragment.customFilterTime.postValue(null)
            AppDataUsageFragment.customFilterDate.postValue(null)

            updateTime()
        }

        binding.dateSelectionView.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.dateRangePicker().build()

            datePicker.addOnPositiveButtonClickListener { pair ->
                startDateMillis = Common.UTCToLocal(pair.first)
                endDateMillis = Common.UTCToLocal(pair.second)
                val dateFormat = SimpleDateFormat("dd/MM/yyyy")
                dateFormat.timeZone = TimeZone.getDefault()
                val startDate = dateFormat.format(Date(startDateMillis))
                val endDate = dateFormat.format(Date(endDateMillis))

                filterDate = if (pair.first == pair.second) {
                    startDate
                }
                else {
                    "$startDate - $endDate"
                }
                binding.selectedDate.text = filterDate
                binding.selectedDate.visibility = View.VISIBLE
            }

            datePicker.show(childFragmentManager, datePicker.tag)
        }

        binding.addTime.setOnClickListener {
            binding.addTime.visibility = View.GONE
            binding.timeSelectionView.visibility = View.VISIBLE
        }

        binding.startTimeSelectionView.setOnClickListener {
            showTimePicker(TYPE_START)
        }

        binding.endTimeSelectionView.setOnClickListener {
            showTimePicker(TYPE_END)
        }

        binding.applyFilter.setOnClickListener {
            val data = Intent()

            val calendar = Calendar.getInstance()

            calendar.timeInMillis = startDateMillis
            calendar.set(Calendar.HOUR_OF_DAY, startHour)
            calendar.set(Calendar.MINUTE, startMinute)
            val startTimeMillis = calendar.timeInMillis

            calendar.timeInMillis = endDateMillis
            calendar.set(Calendar.HOUR_OF_DAY, endHour)
            calendar.set(Calendar.MINUTE, endMinute)
            val endTimeMillis = calendar.timeInMillis

            if (startDateMillis == 0L ||
                startTimeMillis < 0 ||
                endTimeMillis < 0 ||
                startTimeMillis > endTimeMillis) {
                Snackbar.make(binding.root, getString(R.string.error_invalid_session), Snackbar.LENGTH_SHORT)
                    .setAnchorView(R.id.apply_filter).show()
                return@setOnClickListener
            }

            data.putExtra("start", startTimeMillis)
            data.putExtra("end", endTimeMillis)
            data.putExtra("date", filterDate)

            AppDataUsageFragment.customFilterTime.postValue(mapOf(
                Pair("startHour", startHour),
                Pair("startMinute", startMinute),
                Pair("endHour", endHour),
                Pair("endMinute", endMinute)
            ))

            AppDataUsageFragment.customFilterDateMillis.postValue(androidx.core.util.Pair(startDateMillis, endDateMillis))

            AppDataUsageFragment.shouldShowTime = isTimeSet

            activity?.setResult(Activity.RESULT_OK, data)
            activity?.finish()
        }

    }

    private fun updateTime() {
        val startTime: String = requireContext().getString(
            R.string.label_custom_start_time,
            DataPlanFragment.getTime(startHour, startMinute, is12HourView)
        )
        val endTime: String = requireContext().getString(
            R.string.label_custom_end_time,
            DataPlanFragment.getTime(endHour, endMinute, is12HourView)
        )

        binding.selectStartTime.text = Common.setBoldSpan(
            startTime,
            DataPlanFragment.getTime(startHour, startMinute, is12HourView)
        )
        binding.selectEndTime.text = Common.setBoldSpan(
            endTime,
            DataPlanFragment.getTime(endHour, endMinute, is12HourView)
        )
    }

    private fun showTimePicker(type: Int) {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheet)
        val dialogView = LayoutInflater.from(context).inflate(R.layout.layout_time_picker, null)
        val title = dialogView.findViewById<TextView>(R.id.title)
        val timePicker = dialogView.findViewById<TimePicker>(R.id.reset_time_picker)
        val footer = dialogView.findViewById<ConstraintLayout>(R.id.footer)
        val cancel = footer.findViewById<TextView>(R.id.cancel)
        val ok = footer.findViewById<TextView>(R.id.ok)

        ((timePicker.getChildAt(0) as LinearLayout).getChildAt(0) as LinearLayout)
            .getChildAt(0).isVerticalScrollBarEnabled = false
        ((timePicker.getChildAt(0) as LinearLayout).getChildAt(0) as LinearLayout)
            .getChildAt(2).isVerticalScrollBarEnabled = false

        timePicker.setIs24HourView(!is12HourView)

        if (type == TYPE_START) {
            title.text = getString(R.string.label_select_start_time)
            timePicker.hour = startHour
            timePicker.minute = startMinute
        }
        else {
            title.text = getString(R.string.label_select_end_time)
            timePicker.hour = endHour
            timePicker.minute = endMinute
        }

        timePicker.setOnTimeChangedListener { _, _, _ ->
            if (!PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getBoolean("disable_haptics", false)) {
                VibrationUtils.hapticMinor(context)
            }
        }

        cancel.setOnClickListener { dialog.dismiss() }

        ok.setOnClickListener {
            val timeText: String
            val time: String = DataPlanFragment.getTime(timePicker.hour, timePicker.minute, is12HourView)

            if (type == TYPE_START) {
                timeText = requireContext().getString(R.string.label_custom_start_time, time)
                binding.selectStartTime.text = Common.setBoldSpan(timeText, time)
                startHour = timePicker.hour
                startMinute = timePicker.minute
            }
            else {
                timeText = requireContext().getString(R.string.label_custom_end_time, time)
                binding.selectEndTime.text = Common.setBoldSpan(timeText, time)
                endHour = timePicker.hour
                endMinute = timePicker.minute
            }

            isTimeSet = true

            dialog.dismiss()
        }
        dialog.setContentView(dialogView)

        dialog.setOnShowListener {
            dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        dialog.show()
    }
}