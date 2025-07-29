package com.stip.stip.iphome.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.R

data class LicenseScopeItem(
    val title: String,
    val entries: List<Pair<String, String>> // 예: ("게임", "포함")
)

class LicenseScopeAdapter(
    private val items: List<LicenseScopeItem>
) : RecyclerView.Adapter<LicenseScopeAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHeader: TextView = view.findViewById(R.id.tv_license_scope_header)
        val tvArea10: TextView = view.findViewById(R.id.tv_area_10)
        val tvRatio10: TextView = view.findViewById(R.id.tv_ratio_10)
        val tvArea7: TextView = view.findViewById(R.id.tv_area_7)
        val tvRatio7: TextView = view.findViewById(R.id.tv_ratio_7)
        val tvArea5: TextView = view.findViewById(R.id.tv_area_5)
        val tvRatio5: TextView = view.findViewById(R.id.tv_ratio_5)
        val tvArea1: TextView = view.findViewById(R.id.tv_area_1)
        val tvRatio1: TextView = view.findViewById(R.id.tv_ratio_1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_license_scope, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvHeader.text = item.title

        item.entries.getOrNull(0)?.let {
            holder.tvArea10.text = it.first
            holder.tvRatio10.text = it.second
        }
        item.entries.getOrNull(1)?.let {
            holder.tvArea7.text = it.first
            holder.tvRatio7.text = it.second
        }
        item.entries.getOrNull(2)?.let {
            holder.tvArea5.text = it.first
            holder.tvRatio5.text = it.second
        }
        item.entries.getOrNull(3)?.let {
            holder.tvArea1.text = it.first
            holder.tvRatio1.text = it.second
        }
    }

    override fun getItemCount(): Int = items.size
}
