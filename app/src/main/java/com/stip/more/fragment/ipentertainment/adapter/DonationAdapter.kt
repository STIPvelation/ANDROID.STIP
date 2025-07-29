package com.stip.stip.more.fragment.ipentertainment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.stip.stip.R
import com.stip.stip.more.fragment.ipentertainment.model.DonationModel
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DonationAdapter : ListAdapter<DonationModel, DonationAdapter.DonationViewHolder>(DonationDiffCallback()) {

    private var onItemClickListener: ((DonationModel) -> Unit)? = null

    fun setOnItemClickListener(listener: (DonationModel) -> Unit) {
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DonationViewHolder {
        // 추후 item_donation 레이아웃이 없어서 오류 발생, item_filter_period로 대체
        // 이 어댑터는 더 이상 사용되지 않으므로 임시 조치임
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_filter_period, parent, false)
        return DonationViewHolder(view)
    }

    override fun onBindViewHolder(holder: DonationViewHolder, position: Int) {
        val donation = getItem(position)
        holder.bind(donation)
    }

    inner class DonationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // 임시로 TextView 하나만 사용 (item_filter_period.xml의 tv_period)
        private val textView: TextView = itemView.findViewById(R.id.tv_period)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClickListener?.invoke(getItem(position))
                }
            }
        }

        fun bind(donation: DonationModel) {
            // 간단히 제목만 표시 (실제로는 사용되지 않음)
            textView.text = donation.title
        }
        
        private fun getDaysLeft(endDate: Date): Long {
            val now = Date()
            val diffInMillis = endDate.time - now.time
            return if (diffInMillis <= 0) 0 else TimeUnit.MILLISECONDS.toDays(diffInMillis)
        }
    }

    class DonationDiffCallback : DiffUtil.ItemCallback<DonationModel>() {
        override fun areItemsTheSame(oldItem: DonationModel, newItem: DonationModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DonationModel, newItem: DonationModel): Boolean {
            return oldItem == newItem
        }
    }
}
