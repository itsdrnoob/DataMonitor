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
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.RemoteException
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.drnoob.datamonitor.Common
import com.drnoob.datamonitor.R
import com.drnoob.datamonitor.adapters.UsageDataAdapter
import com.drnoob.datamonitor.adapters.data.AppDataUsageModel
import com.drnoob.datamonitor.adapters.data.DataUsageViewModel
import com.drnoob.datamonitor.adapters.data.DataUsageViewModelFactory
import com.drnoob.datamonitor.core.Values
import com.drnoob.datamonitor.databinding.FragmentAppDataUsageBinding
import com.drnoob.datamonitor.ui.activities.ContainerActivity
import com.drnoob.datamonitor.utils.NetworkStatsHelper
import com.drnoob.datamonitor.utils.VibrationUtils
import com.drnoob.datamonitor.utils.helpers.UsageDataHelperImpl
import com.drnoob.datamonitor.utils.loadScreenTime
import com.drnoob.datamonitor.utils.setBoldSpan
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import java.text.ParseException

class AppDataUsageFragment : Fragment() {

    private var _binding: FragmentAppDataUsageBinding? = null
    private val binding get() = _binding!!

    private var session: Int = Values.SESSION_TODAY
    private var type: Int = Values.TYPE_MOBILE_DATA
    private var totalDataUsage: String? = null
    private var fromHome: Boolean = false

    private val dataUsageViewModel: DataUsageViewModel by activityViewModels {
        DataUsageViewModelFactory(UsageDataHelperImpl(requireActivity()))
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UsageDataAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppDataUsageBinding
            .inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("InflateParams")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        session =
            requireActivity().intent.getIntExtra(Values.DATA_USAGE_SESSION, Values.SESSION_TODAY)
        type = requireActivity().intent.getIntExtra(Values.DATA_USAGE_TYPE, Values.TYPE_MOBILE_DATA)
        fromHome = requireActivity().intent.getBooleanExtra(Values.DAILY_DATA_HOME_ACTION, false)
        if (fromHome) binding.filterAppUsage.visibility = View.GONE

        binding.currentSessionTotal.text = "..."

        refreshData()
        recyclerView = binding.appDataUsageRecycler
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = UsageDataAdapter(requireContext())
        recyclerView.adapter = adapter

        binding.filterAppUsage.setOnClickListener {
            val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheet)
            val dialogView =
                LayoutInflater.from(context).inflate(R.layout.layout_app_usage_filter, null)
            val sessionGroup = dialogView.findViewById<ChipGroup>(R.id.session_group)
            val typeGroup = dialogView.findViewById<ChipGroup>(R.id.type_group)
            val footer = dialogView.findViewById<ConstraintLayout>(R.id.footer)
            val cancel = footer.findViewById<TextView>(R.id.cancel)
            val ok = footer.findViewById<TextView>(R.id.ok)
            val sessionCurrentPlan = sessionGroup.findViewById<Chip>(R.id.session_current_plan)
            if (PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getString(Values.DATA_RESET, "null")
                == Values.DATA_RESET_CUSTOM
            ) {
                sessionCurrentPlan.visibility = View.VISIBLE
            } else {
                sessionCurrentPlan.visibility = View.GONE
            }
            sessionGroup.setOnCheckedStateChangeListener { _, _ ->
                if (!PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .getBoolean("disable_haptics", false)
                ) {
                    VibrationUtils.hapticMinor(context)
                }
            }
            typeGroup.setOnCheckedStateChangeListener { _, _ ->
                if (!PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .getBoolean("disable_haptics", false)
                ) {
                    VibrationUtils.hapticMinor(context)
                }
            }
            when (session) {
                Values.SESSION_TODAY -> sessionGroup.check(R.id.session_today)
                Values.SESSION_YESTERDAY -> sessionGroup.check(R.id.session_yesterday)
                Values.SESSION_THIS_MONTH -> sessionGroup.check(R.id.session_this_month)
                Values.SESSION_LAST_MONTH -> sessionGroup.check(R.id.session_last_month)
                Values.SESSION_THIS_YEAR -> sessionGroup.check(R.id.session_this_year)
                Values.SESSION_ALL_TIME -> sessionGroup.check(R.id.session_all_time)
                Values.SESSION_CUSTOM -> sessionGroup.check(R.id.session_current_plan)
            }
            when (type) {
                Values.TYPE_MOBILE_DATA -> typeGroup.check(R.id.type_mobile)
                Values.TYPE_WIFI -> typeGroup.check(R.id.type_wifi)
            }
            cancel.setOnClickListener { dialog.dismiss() }
            ok.setOnClickListener {
                session = when (sessionGroup.checkedChipId) {
                    R.id.session_yesterday -> Values.SESSION_YESTERDAY
                    R.id.session_this_month -> Values.SESSION_THIS_MONTH
                    R.id.session_last_month -> Values.SESSION_LAST_MONTH
                    R.id.session_this_year -> Values.SESSION_THIS_YEAR
                    R.id.session_all_time -> Values.SESSION_ALL_TIME
                    R.id.session_current_plan -> Values.SESSION_CUSTOM
                    R.id.session_today -> Values.SESSION_TODAY
                    else -> Values.SESSION_TODAY
                }
                type = when (typeGroup.checkedChipId) {
                    R.id.type_wifi -> Values.TYPE_WIFI
                    R.id.type_mobile -> Values.TYPE_MOBILE_DATA
                    else -> Values.TYPE_MOBILE_DATA
                }
                refreshData()
                dialog.dismiss()
            }
            dialog.setContentView(dialogView)
            dialog.setOnShowListener { dialogInterface ->
                val bottomSheetDialog = dialogInterface as BottomSheetDialog
                val bottomSheet =
                    bottomSheetDialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                bottomSheet?.let {
                    BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
            dialog.show()
        }
        binding.refreshDataUsage.setOnRefreshListener { refreshData() }

        // Shrink or expand the FAB according to user scroll
        binding.appDataUsageRecycler.setOnScrollChangeListener { _, _, _, _, oldScrollY ->
            if (oldScrollY < -15 && binding.filterAppUsage.isExtended) {
                binding.filterAppUsage.shrink()
            } else if (oldScrollY > 15 && !binding.filterAppUsage.isExtended) {
                binding.filterAppUsage.extend()
            } else if (binding.appDataUsageRecycler.computeVerticalScrollOffset() == 0 && !binding.filterAppUsage.isExtended) {
                binding.filterAppUsage.extend()
            }
        }

        dataUsageViewModel.userAppsList.observe(viewLifecycleOwner) { userAppsList ->
            onDataLoaded(userAppsList)
            adapter.differ.submitList(userAppsList)
        }

        adapter.setOnItemClickListener { model ->
            if (model.packageName == requireContext().getString(R.string.package_system)) {
                val intent = Intent(context, ContainerActivity::class.java)
                //                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(
                    Values.GENERAL_FRAGMENT_ID,
                    Values.DATA_USAGE_SYSTEM
                )
                intent.putExtra(
                    Values.DATA_USAGE_SESSION,
                    model.session
                )
                intent.putExtra(Values.DATA_USAGE_TYPE, model.type)
                requireContext().startActivity(intent)
            } else setDataUsageSheetDialog(model)
        }
    }

    @Throws(ParseException::class, RemoteException::class)
    private fun getTotalDataUsage(context: Context?): String {
        val date = PreferenceManager.getDefaultSharedPreferences(
            requireContext()
        ).getInt(Values.DATA_RESET_DATE, -1)
        return when (type) {
            Values.TYPE_MOBILE_DATA -> NetworkStatsHelper.formatData(
                NetworkStatsHelper.getTotalAppMobileDataUsage(context, session, date)[0],
                NetworkStatsHelper.getTotalAppMobileDataUsage(context, session, date)[1]
            )[2]

            Values.TYPE_WIFI -> NetworkStatsHelper.formatData(
                NetworkStatsHelper.getTotalAppWifiDataUsage(context, session)[0],
                NetworkStatsHelper.getTotalAppWifiDataUsage(context, session)[1]
            )[2]

            else -> requireContext().getString(R.string.label_unknown)
        }
    }

    private fun onDataLoaded(userAppsList: List<AppDataUsageModel?>) {
        try {
            if (totalDataUsage?.isEmpty() == true)
                totalDataUsage = getTotalDataUsage(context)

            binding.currentSessionTotal.text =
                requireContext().getString(R.string.total_usage, totalDataUsage)
        } catch (e: ParseException) {
            e.printStackTrace()
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
        binding.refreshDataUsage.isRefreshing = false
        binding.layoutListLoading.root.animate().alpha(0.0f)
        binding.appDataUsageRecycler.animate().alpha(1.0f)
        if (userAppsList.isEmpty()) {
            binding.layoutListLoading.root.animate().alpha(0.0F)
            binding.emptyList.animate().alpha(1.0F)
        } else {
            session = userAppsList[0]?.session ?: 0
            type = userAppsList[0]?.type ?: 0
        }
    }

    private fun refreshData() {
        binding.layoutListLoading.root.animate().alpha(1.0f)
        binding.appDataUsageRecycler.animate().alpha(0.0f)
        binding.emptyList.animate().alpha(0.0f)
        binding.refreshDataUsage.isRefreshing = true
        binding.appDataUsageRecycler.removeAllViews()
        totalDataUsage = ""
        binding.currentSessionTotal.text = "..."
        dataUsageViewModel.loadUserAppsData(session, type)
    }

    @SuppressLint("InflateParams")
    fun setDataUsageSheetDialog(model: AppDataUsageModel) {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheet)
        val dialogView =
            LayoutInflater.from(context).inflate(R.layout.app_detail_view, null)
        dialog.setContentView(dialogView)
        val appIcon = dialogView.findViewById<ImageView>(R.id.icon)
        val appName = dialogView.findViewById<TextView>(R.id.name)
        val dataSent = dialogView.findViewById<TextView>(R.id.data_sent)
        val dataReceived = dialogView.findViewById<TextView>(R.id.data_received)
        val appPackage = dialogView.findViewById<TextView>(R.id.app_package)
        val appUid = dialogView.findViewById<TextView>(R.id.app_uid)
        val appScreenTime = dialogView.findViewById<TextView>(R.id.app_screen_time)
        val appBackgroundTime = dialogView.findViewById<TextView>(R.id.app_background_time)
        val appCombinedTotal = dialogView.findViewById<TextView>(R.id.app_combined_total)
        val appSettings = dialogView.findViewById<MaterialButton>(R.id.app_open_settings)
        appName.text = model.appName
        val packageName: String = requireContext().resources.getString(
            R.string.app_label_package_name,
            model.packageName
        )
        val uid: String = requireContext().resources.getString(
            R.string.app_label_uid,
            model.uid
        )
        appPackage.text = setBoldSpan(packageName, model.packageName)
        appUid.text = setBoldSpan(uid, model.uid.toString())
        if (model.packageName !== requireContext().getString(R.string.package_tethering)) {
            val screenTime: String = requireContext().getString(
                R.string.app_label_screen_time,
                requireContext().getString(R.string.label_loading)
            )
            val backgroundTime: String = requireContext().getString(
                R.string.app_label_background_time,
                requireContext().getString(R.string.label_loading)
            )
            appScreenTime.text = setBoldSpan(
                screenTime,
                requireContext().getString(R.string.label_loading)
            )
            appBackgroundTime.text = setBoldSpan(
                backgroundTime,
                requireContext().getString(R.string.label_loading)
            )
            lifecycleScope.launch {
                loadScreenTime(requireContext(), model, appScreenTime, appBackgroundTime)
            }
        } else {
            appScreenTime.visibility = View.GONE
            appBackgroundTime.visibility = View.GONE
        }
        val total =
            model.sentMobile + model.receivedMobile + model.sentWifi + model.receivedWifi
        val combinedTotal = NetworkStatsHelper.formatData(0L, total)[2]
        dataSent.text =
            NetworkStatsHelper.formatData(model.sentMobile, model.receivedMobile)[0]
        dataReceived.text =
            NetworkStatsHelper.formatData(model.sentMobile, model.receivedMobile)[1]
        appCombinedTotal.text = setBoldSpan(
            requireContext().getString(R.string.app_label_combined_total, combinedTotal),
            combinedTotal
        )
        appSettings.setOnClickListener {
            val intent =
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", model.packageName, null)
            intent.data = uri
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            requireContext().startActivity(intent)
        }
        try {
            if (model.packageName == requireContext().getString(R.string.package_tethering)) {
                appIcon.setImageResource(R.drawable.hotspot)
                appPackage.visibility = View.GONE
                appUid.visibility = View.GONE
                appSettings.visibility = View.GONE
                appCombinedTotal.visibility = View.GONE
            } else if (model.packageName == requireContext().getString(R.string.package_removed)) {
                appIcon.setImageResource(R.drawable.deleted_apps)
                appPackage.visibility = View.GONE
                appUid.visibility = View.GONE
                appSettings.visibility = View.GONE
            } else {
                if (Common.isAppInstalled(context, model.packageName)) {
                    appIcon.setImageDrawable(
                        requireContext().packageManager.getApplicationIcon(model.packageName)
                    )
                } else {
                    appIcon.setImageResource(R.drawable.deleted_apps)
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet =
                bottomSheetDialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior: BottomSheetBehavior<*> =
                    BottomSheetBehavior.from(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        private val TAG = AppDataUsageFragment::class.java.simpleName
    }

}