package com.drnoob.datamonitor.adapters

import android.content.Context
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.drnoob.datamonitor.Common
import com.drnoob.datamonitor.R
import com.drnoob.datamonitor.adapters.data.AppDataUsageModel
import com.drnoob.datamonitor.utils.NetworkStatsHelper
import com.skydoves.progressview.ProgressView

class UsageDataAdapter(private val context: Context) :
    RecyclerView.Adapter<UsageDataAdapter.UsageDataViewHolder>() {

    inner class UsageDataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private val differCallbacks = object : DiffUtil.ItemCallback<AppDataUsageModel>() {
        override fun areItemsTheSame(
            oldItem: AppDataUsageModel,
            newItem: AppDataUsageModel
        ): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(
            oldItem: AppDataUsageModel,
            newItem: AppDataUsageModel
        ): Boolean {
            return oldItem.totalDataUsage == newItem.totalDataUsage
        }
    }

    val differ = AsyncListDiffer(this, differCallbacks)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): UsageDataAdapter.UsageDataViewHolder {
        return UsageDataViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.app_data_usage_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: UsageDataAdapter.UsageDataViewHolder, position: Int) {

        val model = differ.currentList[position]
        val itemView = holder.itemView

        val mAppIcon = itemView.findViewById<ImageView>(R.id.app_icon)
        val mAppName = itemView.findViewById<TextView>(R.id.app_name)
        val mDataUsage = itemView.findViewById<TextView>(R.id.data_usage)
        val mProgress = itemView.findViewById<ProgressView>(R.id.progress)

        try {
            if (model.packageName == "com.android.tethering")
                mAppIcon.setImageResource(R.drawable.hotspot)
            else if (model.packageName == "com.android.deleted")
                mAppIcon.setImageResource(R.drawable.deleted_apps)
            else
                if (Common.isAppInstalled(context, model.packageName))
                    mAppIcon.setImageDrawable(
                        context.packageManager.getApplicationIcon(model.packageName)
                    )
                else mAppIcon.setImageResource(R.drawable.deleted_apps)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        val totalDataUsage =
            NetworkStatsHelper.formatData(model.sentMobile, model.receivedMobile)[2]

        if (model.progress > 0) mProgress.progress = model.progress.toFloat()
        else mProgress.progress = 1F

        mAppName.text = model.appName
        mDataUsage.text = totalDataUsage

        itemView.setOnClickListener {
            onItemClickListener?.let {
                if (model != null) it(model)
            }
        }

    }

    override fun getItemCount(): Int =
        differ.currentList.size

    private var onItemClickListener: ((AppDataUsageModel) -> Unit)? = null

    fun setOnItemClickListener(listener: (AppDataUsageModel) -> Unit) {
        onItemClickListener = listener
    }

}