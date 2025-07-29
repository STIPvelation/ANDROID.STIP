package com.stip.stip.iphome.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.R
import com.stip.stip.databinding.ItemIpFilterDropdownBinding

class IpDropdownFilterAdapter(
    private val filters: List<String>,
    private var selectedIndex: Int = 0,
    private val onFilterClick: (String) -> Unit
) : RecyclerView.Adapter<IpDropdownFilterAdapter.FilterViewHolder>() {

    inner class FilterViewHolder(private val binding: ItemIpFilterDropdownBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(text: String, isSelected: Boolean) {
            binding.textDropdownFilter.text = text

            val styleRes = if (isSelected) {
                R.style.all_ip_fillter_activ_style
            } else {
                R.style.all_ip_fillter_inactiv_style
            }

            binding.textDropdownFilter.setTextAppearance(styleRes)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemIpFilterDropdownBinding.inflate(inflater, parent, false)
        return FilterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        val isSelected = position == selectedIndex
        holder.bind(filters[position], isSelected)

        holder.itemView.setOnClickListener {
            if (position != selectedIndex) {
                val previousIndex = selectedIndex
                selectedIndex = position
                notifyItemChanged(previousIndex)
                notifyItemChanged(position)
                onFilterClick(filters[position])
            }
        }
    }

    override fun getItemCount(): Int = filters.size

    fun setSelectedFilter(filter: String) {
        val index = filters.indexOf(filter)
        if (index != -1 && index != selectedIndex) {
            val previousIndex = selectedIndex
            selectedIndex = index
            notifyItemChanged(previousIndex)
            notifyItemChanged(selectedIndex)
        }
    }

    fun getSelectedFilter(): String {
        return filters.getOrNull(selectedIndex) ?: "ALL IP"
    }
}