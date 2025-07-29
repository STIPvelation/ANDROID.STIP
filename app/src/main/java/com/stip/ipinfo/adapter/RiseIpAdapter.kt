package com.stip.stip.ipinfo.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.stip.stip.R
import com.stip.stip.ipinfo.model.RiseIpItem

class RiseIpAdapter(
    private val items: List<RiseIpItem>,
    private val onItemClick: (RiseIpItem) -> Unit
) : RecyclerView.Adapter<RiseIpAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.ticker_text)
        val tickerText: TextView = view.findViewById(R.id.name_text)
        val percentText: TextView = view.findViewById(R.id.percent_text)
        fun bind(item: RiseIpItem) {
            // 회사이름 (티커/USD) 형식으로 표시
            nameText.text = "${item.name} (${item.ticker}/USD)"
            percentText.text = item.percent
            // ticker 개별 TextView는 숨기거나 사용하지 않음
            tickerText.visibility = View.GONE
            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_top_rising_ip, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
        // 상승/하락/무변화 색상 적용
        val percentValue = item.percent.replace("%", "").toFloatOrNull() ?: 0f
        val colorRes = when {
            percentValue > 0f -> R.color.color_rise
            percentValue < 0f -> R.color.color_fall
            else ->  R.color.color_text_default // 0.00%일 경우
        }
        holder.percentText.setTextColor(
            ContextCompat.getColor(holder.itemView.context, colorRes)
        )
    }

    override fun getItemCount(): Int = items.size
}