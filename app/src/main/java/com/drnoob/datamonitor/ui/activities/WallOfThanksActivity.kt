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

package com.drnoob.datamonitor.ui.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.Interpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.drnoob.datamonitor.Common.dismissOnClick
import com.drnoob.datamonitor.R
import com.drnoob.datamonitor.adapters.DonorAdapter
import com.drnoob.datamonitor.adapters.data.DonorModel
import com.drnoob.datamonitor.core.Values.DONATE_FRAGMENT
import com.drnoob.datamonitor.core.Values.GENERAL_FRAGMENT_ID
import com.drnoob.datamonitor.core.Values.WALL_OF_THANKS_ALL_DONORS
import com.drnoob.datamonitor.core.Values.WALL_OF_THANKS_FEATURED_DONORS
import com.drnoob.datamonitor.core.Values.WALL_OF_THANKS_LAST_UPDATE
import com.drnoob.datamonitor.databinding.ActivityWallOfThanksBinding
import com.drnoob.datamonitor.utils.SharedPreferences
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets


class WallOfThanksActivity : AppCompatActivity() {
    companion object {
        private val TAG = WallOfThanksActivity::class.simpleName
    }
    lateinit var binding: ActivityWallOfThanksBinding

    val duration = 120L
    val pixelsToMove = 45
    private var mHandler = Handler(Looper.getMainLooper())
    private val SCROLLING_RUNNABLE: Runnable = object : Runnable {
        override fun run() {
            isListInMotion = true
            binding.allDonorsList.smoothScrollBy(pixelsToMove, 0, UniformSpeedInterpolator())
            mHandler.postDelayed(this, duration)
        }
    }
    private var isListInMotion = false
    private var isDataRefreshing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityWallOfThanksBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        setContentView(binding.root)

        initialiseLayout()
        refreshData(false)

        /*
        Listen to recyclerView scroll state to see if user is scrolling the recyclerView.
        Remove autoScroll handler during user scroll and attach it back when its idle.
         */
        binding.allDonorsList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val lastItem: Int = (binding.allDonorsList.layoutManager as LinearLayoutManager)
                    .findLastCompletelyVisibleItemPosition()
                if (lastItem == (binding.allDonorsList.layoutManager as LinearLayoutManager)
                        .itemCount - 1) {
                    mHandler.removeCallbacks(SCROLLING_RUNNABLE)
                    isListInMotion = false
                    val postHandler = Handler()
                    postHandler.postDelayed({
                        var adapter = recyclerView.adapter as DonorAdapter
                        recyclerView.adapter = null
                        recyclerView.adapter = adapter
                        mHandler.postDelayed(SCROLLING_RUNNABLE, 0)
                    }, 0)
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        mHandler.postDelayed(SCROLLING_RUNNABLE, 100)
                    }
                    RecyclerView.SCROLL_STATE_DRAGGING, RecyclerView.SCROLL_STATE_SETTLING -> {
                        mHandler.removeCallbacks(SCROLLING_RUNNABLE)
                        isListInMotion = false
                    }
                }
            }
        })

//        mHandler.postDelayed(SCROLLING_RUNNABLE, 500)


        binding.donate.setOnClickListener {
            startActivity(Intent(this, ContainerActivity::class.java)
                .putExtra(GENERAL_FRAGMENT_ID, DONATE_FRAGMENT))
        }

        binding.donate.setOnLongClickListener {
            refreshData(true)
            true
        }

        binding.learnMore.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.wall_of_thanks_about))))
        }
    }

    private fun refreshData(shouldForceRefresh: Boolean) {
        /*
        For convenience, data is refreshed only once in 30 minutes.
        The last updated time is saved and checked before refreshing.
         */
        isDataRefreshing = true
        val lastUpdate = SharedPreferences.getUserPrefs(this).getLong(WALL_OF_THANKS_LAST_UPDATE, 0L)
        val now = System.currentTimeMillis()
        val gson = Gson()
        val type = object : TypeToken<List<DonorModel>>() {}.type

        if (shouldForceRefresh ||
            lastUpdate == 0L ||
            now - lastUpdate > (30 * 60 * 1000).toLong()) {
            val queue = Volley.newRequestQueue(this)
            val request = StringRequest(
                Request.Method.GET,
                getString(R.string.wall_of_thanks_api),
                { response ->
                    try {
                        val jsonObject = JSONObject(response)

                        val donorsJson = jsonObject.get("donors")
                        val featuredDonorsJson = jsonObject.get("featuredDonors")

                        val donorsArray = JSONArray(donorsJson.toString())
                        val featuredDonorsArray = JSONArray(featuredDonorsJson.toString())

                        val allDonorsList = mutableListOf<DonorModel>()
                        val featuredDonorsList = mutableListOf<DonorModel>()

                        for (i in 0 until donorsArray.length()) {
                            val donor = donorsArray.getJSONObject(i)
                            allDonorsList.add(DonorModel(donor.getString("name"),
                                donor.getString("photoURL")))
                        }

                        for (i in 0 until featuredDonorsArray.length()) {
                            val donor = featuredDonorsArray.getJSONObject(i)
                            featuredDonorsList.add(DonorModel(donor.getString("name"),
                                donor.getString("photoURL")))
                        }

                        refreshList(featuredDonorsList, allDonorsList, true)

                        val featuredJsonData = gson.toJson(featuredDonorsList, type)
                        val allJsonData = gson.toJson(allDonorsList, type)
                        SharedPreferences.getAppPrefs(this).edit()
                            .putString(WALL_OF_THANKS_ALL_DONORS, allJsonData)
                            .putString(WALL_OF_THANKS_FEATURED_DONORS, featuredJsonData)
                            .apply()
                        SharedPreferences.getUserPrefs(this).edit()
                            .putLong(WALL_OF_THANKS_LAST_UPDATE, System.currentTimeMillis())
                            .apply()

                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                        val snackbar = Snackbar.make(binding.root, getString(R.string.error_data_fetch_failed), Snackbar.LENGTH_LONG)
                            .setAnchorView(binding.donate)
                        dismissOnClick(snackbar)
                        snackbar.show()
                    }
                },
                { error ->
                    Log.e(TAG, "refreshData error: ${error.networkResponse}")
                    if (!SharedPreferences.getAppPrefs(this)
                            .getString(WALL_OF_THANKS_ALL_DONORS, null).isNullOrBlank() &&
                        !SharedPreferences.getAppPrefs(this)
                            .getString(WALL_OF_THANKS_FEATURED_DONORS, null).isNullOrBlank()) {
                        val allDonors = SharedPreferences.getAppPrefs(this)
                            .getString(WALL_OF_THANKS_ALL_DONORS, null)
                        val featuredDonors = SharedPreferences.getAppPrefs(this)
                            .getString(WALL_OF_THANKS_FEATURED_DONORS, null)

                        val allDonorsList = mutableListOf<DonorModel>()
                        val featuredDonorsList = mutableListOf<DonorModel>()

                        if (!allDonors.isNullOrEmpty()) {
                            allDonorsList.addAll(gson.fromJson(allDonors, type))
                        }

                        if (!featuredDonors.isNullOrEmpty()) {
                            featuredDonorsList.addAll(gson.fromJson(featuredDonors, type))
                        }

                        refreshList(featuredDonorsList, allDonorsList, false)
                    }
                    var errorCode = 0
                    try {
                        errorCode = error.networkResponse.statusCode
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                    }
                    val errorMessage: String = if (errorCode == 429) {
                        String(error.networkResponse.data, StandardCharsets.UTF_8)
                    }
                    else {
                        getString(R.string.error_data_fetch_failed)
                    }
                    val snackbar = Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG)
                        .setAnchorView(binding.donate)
                    dismissOnClick(snackbar)
                    snackbar.show()
                }
            )
            queue.add(request).retryPolicy = DefaultRetryPolicy(0, 0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)

        }
        else {
            val allDonors = SharedPreferences.getAppPrefs(this)
                .getString(WALL_OF_THANKS_ALL_DONORS, null)
            val featuredDonors = SharedPreferences.getAppPrefs(this)
                .getString(WALL_OF_THANKS_FEATURED_DONORS, null)

            val allDonorsList = mutableListOf<DonorModel>()
            val featuredDonorsList = mutableListOf<DonorModel>()

            if (!allDonors.isNullOrEmpty()) {
                allDonorsList.addAll(gson.fromJson(allDonors, type))
            }

            if (!featuredDonors.isNullOrEmpty()) {
                featuredDonorsList.addAll(gson.fromJson(featuredDonors, type))
            }

            refreshList(featuredDonorsList, allDonorsList, false)
        }
    }

    private fun refreshList(featuredDonorsList: MutableList<DonorModel>,
                            allDonorsList: MutableList<DonorModel>, isUpdatedData: Boolean) {
        if (isUpdatedData) {
            binding.topDonorsList.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    binding.topDonorsList.adapter = DonorAdapter(featuredDonorsList, this, DonorAdapter.VIEW_TYPE_FEATURED)

                    binding.topDonorsList.animate()
                        .alpha(1f)
                        .setDuration(200)
                        .start()
                }
                .start()

            binding.allDonorsList.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    binding.allDonorsList.adapter = DonorAdapter(allDonorsList, this, DonorAdapter.VIEW_TYPE_NORMAL)

                    binding.allDonorsList.animate()
                        .alpha(1f)
                        .setDuration(200)
                        .withEndAction {
                            // Start the scroll after 500 millis
                            mHandler.postDelayed(SCROLLING_RUNNABLE, 500)
                            isDataRefreshing = false
                        }
                        .start()
                }
                .start()
        }
        else {
            binding.topDonorsList.adapter = DonorAdapter(featuredDonorsList, this, DonorAdapter.VIEW_TYPE_FEATURED)
            binding.allDonorsList.adapter = DonorAdapter(allDonorsList, this, DonorAdapter.VIEW_TYPE_NORMAL)

            mHandler.postDelayed(SCROLLING_RUNNABLE, 500)
        }

    }

    private fun initialiseLayout() {
        val lp: ConstraintLayout.LayoutParams = binding.donate.layoutParams as ConstraintLayout.LayoutParams
        lp.bottomMargin = getNavBarHeight() + 20
        binding.donate.layoutParams = lp

//        fadeInView(binding.scrollView)

        binding.topDonorsList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.allDonorsList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        binding.topDonorsList.adapter = DonorAdapter(ArrayList(), this, DonorAdapter.VIEW_TYPE_FEATURED_LOADING)
        binding.allDonorsList.adapter = DonorAdapter(ArrayList(), this, DonorAdapter.VIEW_TYPE_NORMAL_LOADING)
        binding.allDonorsList.suppressLayout(true)

    }

    private fun fadeInView(view: View) {
        view.animate()
            .translationYBy(200f)
            .setDuration(0)
            .alpha(0f)
            .setListener(object: AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    view.animate()
                        .translationY(0f)
                        .setDuration(500)
                        .alpha(1f)
                        .start()
                }
            })
            .start()
    }

    @SuppressLint("InternalInsetResource", "DiscouragedApi")
    private fun getNavBarHeight(): Int {
        var navigationBarHeight = 0
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            navigationBarHeight = resources.getDimensionPixelSize(resourceId)
        }
        return navigationBarHeight
    }

    private class UniformSpeedInterpolator : Interpolator {
        override fun getInterpolation(input: Float): Float {
            return input
        }
    }
}