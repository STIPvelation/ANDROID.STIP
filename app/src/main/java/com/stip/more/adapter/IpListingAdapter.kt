package com.stip.stip.more.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.R

class IpListingAdapter(
    private val ipItems: List<String>
) : RecyclerView.Adapter<IpListingAdapter.ViewHolder>() {

    // 선택된 IP 항목 추적
    private val selectedItems = HashSet<Int>()
    
    // 선택 변경 리스너
    private var onSelectionChangedListener: ((Int) -> Unit)? = null
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvIpTitle: TextView = itemView.findViewById(R.id.tvIpTitle)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBoxSelected)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ip_exchange_listing, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvIpTitle.text = ipItems[position]
        holder.checkBox.isChecked = selectedItems.contains(position)

        // 전체 아이템에 클릭 리스너 연결
        holder.itemView.setOnClickListener {
            toggleSelection(position)
        }

        // 체크박스에 클릭 리스너 연결
        holder.checkBox.setOnClickListener {
            toggleSelection(position)
        }
    }

    private fun toggleSelection(position: Int) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position)
        } else {
            selectedItems.add(position)
        }
        notifyItemChanged(position)
        onSelectionChangedListener?.invoke(selectedItems.size)
    }

    override fun getItemCount() = ipItems.size
    
    fun getSelectedCount(): Int = selectedItems.size
    
    fun setOnSelectionChangedListener(listener: (Int) -> Unit) {
        onSelectionChangedListener = listener
    }
    
    fun getSelectedItems(): List<String> {
        return selectedItems.map { ipItems[it] }
    }
}
