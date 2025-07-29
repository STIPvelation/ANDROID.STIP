package com.stip.stip.more.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.R

/**
 * 로그인 이력 기간 필터 선택용 어댑터
 */
class FilterPeriodAdapter(
    private val periods: List<String>,
    private var selectedPeriod: String,
    private val onPeriodSelected: (String) -> Unit
) : RecyclerView.Adapter<FilterPeriodAdapter.FilterViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_filter_period,
            parent,
            false
        )
        return FilterViewHolder(view)
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        val period = periods[position]
        holder.bind(period, period == selectedPeriod)
        
        holder.itemView.setOnClickListener {
            val oldSelectedPosition = periods.indexOf(selectedPeriod)
            selectedPeriod = period
            notifyItemChanged(oldSelectedPosition)
            notifyItemChanged(position)
            onPeriodSelected(period)
        }
    }

    override fun getItemCount(): Int = periods.size

    fun updateSelectedPeriod(period: String) {
        if (selectedPeriod == period) return
        val oldSelectedPosition = periods.indexOf(selectedPeriod)
        selectedPeriod = period
        val newSelectedPosition = periods.indexOf(selectedPeriod)
        notifyItemChanged(oldSelectedPosition)
        notifyItemChanged(newSelectedPosition)
    }

    class FilterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPeriod: TextView = itemView.findViewById(R.id.tv_period)
        private val ivSelected: ImageView = itemView.findViewById(R.id.iv_selected)

        fun bind(period: String, isSelected: Boolean) {
            tvPeriod.text = period
            if (isSelected) {
                ivSelected.visibility = View.VISIBLE
                tvPeriod.setTextColor(itemView.context.getColor(R.color.main_point))
            } else {
                ivSelected.visibility = View.GONE
                tvPeriod.setTextColor(itemView.context.getColor(android.R.color.black))
            }
        }
    }
}
