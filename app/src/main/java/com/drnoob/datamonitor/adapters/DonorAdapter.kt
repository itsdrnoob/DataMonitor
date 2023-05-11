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

package com.drnoob.datamonitor.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.drnoob.datamonitor.R
import com.drnoob.datamonitor.adapters.data.DonorModel
import com.google.android.material.imageview.ShapeableImageView
import kotlin.properties.Delegates

class DonorAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder> {
    companion object {
        private val TAG: String? = DonorAdapter::class.java.simpleName
        const val VIEW_TYPE_NORMAL: Int = 0
        const val VIEW_TYPE_FEATURED: Int = 1
        const val VIEW_TYPE_NORMAL_LOADING: Int = 2
        const val VIEW_TYPE_FEATURED_LOADING: Int = 3
    }

    private lateinit var donors: List<DonorModel>
    private lateinit var context: Context
    private var viewType by Delegates.notNull<Int>()

    constructor(donors: List<DonorModel>, context: Context) {
        this.donors = donors
        this.context = context
    }

    constructor(donors: List<DonorModel>, context: Context, viewType: Int) {
        this.donors = donors
        this.context = context
        this.viewType = viewType
    }

    constructor()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.wall_of_thanks_item, parent, false)
        val itemViewLoading = LayoutInflater.from(parent.context)
            .inflate(R.layout.wall_of_thanks_item_loading, parent, false)
        val featuredItemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.wall_of_thanks_featured_item, parent, false)
        val featuredItemViewLoading = LayoutInflater.from(parent.context)
            .inflate(R.layout.wall_of_thanks_featured_item_loading, parent, false)

        return when (this.viewType) {
            VIEW_TYPE_NORMAL -> DonorViewHolder(itemView)
            VIEW_TYPE_NORMAL_LOADING -> DonorViewHolder(itemViewLoading)
            VIEW_TYPE_FEATURED -> FeaturedDonorViewHolder(featuredItemView)
            VIEW_TYPE_FEATURED_LOADING -> FeaturedDonorViewHolder(featuredItemViewLoading)
            else -> DonorViewHolder(itemView)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (viewType == VIEW_TYPE_NORMAL) {
            val realPosition = position % donors.size
            holder as DonorViewHolder
            Glide.with(context).load(donors[realPosition].photoURL)
                .placeholder(R.drawable.ic_donor_placeholder)
                .centerCrop()
                .into(holder.icon)
            holder.title.text = donors[realPosition].name
        }
        else if (viewType == VIEW_TYPE_FEATURED) {
            val realPosition = position % donors.size
            holder as FeaturedDonorViewHolder
            Glide.with(context).load(donors[realPosition].photoURL)
                .placeholder(R.drawable.ic_donor_placeholder)
                .centerCrop()
                .into(holder.icon)
            holder.title.text = donors[realPosition].name
            holder.title.isSelected = true
        }
    }

    override fun getItemCount(): Int {
        return if (viewType == VIEW_TYPE_NORMAL) {
            Int.MAX_VALUE
        }
        else if (viewType == VIEW_TYPE_NORMAL_LOADING) {
            5
        }
        else {
            3
        }
    }

    class DonorViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val icon: ShapeableImageView = itemView.findViewById(R.id.donor_image)
        val title: TextView = itemView.findViewById(R.id.donor_title)
    }

    class FeaturedDonorViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val icon: ShapeableImageView = itemView.findViewById(R.id.donor_image)
        val title: TextView = itemView.findViewById(R.id.donor_title)
    }
}